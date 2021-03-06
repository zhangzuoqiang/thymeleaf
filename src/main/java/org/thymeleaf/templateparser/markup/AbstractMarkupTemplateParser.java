/*
 * =============================================================================
 * 
 *   Copyright (c) 2011-2014, The THYMELEAF team (http://www.thymeleaf.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package org.thymeleaf.templateparser.markup;

import java.io.CharArrayReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.attoparser.AbstractChainedMarkupHandler;
import org.attoparser.IMarkupHandler;
import org.attoparser.IMarkupParser;
import org.attoparser.MarkupParser;
import org.attoparser.ParseException;
import org.attoparser.ParseStatus;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.select.BlockSelectorMarkupHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.engine.ITemplateHandler;
import org.thymeleaf.engine.TemplateHandlerAdapterMarkupHandler;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.resource.CharArrayResource;
import org.thymeleaf.resource.IResource;
import org.thymeleaf.resource.ReaderResource;
import org.thymeleaf.resource.StringResource;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateparser.ITemplateParser;
import org.thymeleaf.util.Validate;

/**
 *
 * @author Daniel Fern&aacute;ndez
 * @since 3.0.0
 * 
 */
public abstract class AbstractMarkupTemplateParser implements ITemplateParser {


    private final IMarkupParser parser;
    private final boolean html;



    protected AbstractMarkupTemplateParser(final ParseConfiguration parseConfiguration, final int bufferPoolSize, final int bufferSize) {
        super();
        Validate.notNull(parseConfiguration, "Parse configuration cannot be null");
        this.parser = new MarkupParser(parseConfiguration, bufferPoolSize, bufferSize);
        this.html = parseConfiguration.getMode().equals(ParseConfiguration.ParsingMode.HTML);
    }




    /*
     * -------------------
     * PARSE METHODS
     * -------------------
     */



    public final void parseTemplate(
            final IEngineConfiguration configuration,
            final TemplateMode templateMode,
            final IResource templateResource,
            final String[] selectors,
            final ITemplateHandler templateHandler) {
        parse(configuration, templateMode, templateResource, true, selectors, templateHandler);
    }


    public final void parseFragment(
            final IEngineConfiguration configuration,
            final TemplateMode templateMode,
            final IResource templateResource,
            final String[] selectors,
            final ITemplateHandler templateHandler) {
        parse(configuration, templateMode, templateResource, false, selectors, templateHandler);
    }



    private void parse(
            final IEngineConfiguration configuration,
            final TemplateMode templateMode,
            final IResource templateResource,
            final boolean topLevel,
            final String[] selectors,
            final ITemplateHandler templateHandler) {

        Validate.notNull(configuration, "Engine Configuration cannot be null");
        Validate.notNull(templateMode, "Template Mode cannot be null");
        Validate.notNull(templateResource, "Template Resource cannot be null");
        // Selectors CAN be null
        Validate.notNull(templateHandler, "Template Handler cannot be null");

        if (templateMode.isHTML()) {
            Validate.isTrue(this.html, "Parser is configured as XML, but HTML-mode template parsing is being requested");
        } else if (templateMode.isXML()) {
            Validate.isTrue(!this.html, "Parser is configured as HTML, but XML-mode template parsing is being requested");
        } else {
            throw new IllegalArgumentException(
                    "Parser is configured as " + (this.html? "HTML" : "XML") + " but an unsupported template mode " +
                    "has been specified: " + templateMode);
        }

        final String templateResourceName = templateResource.getName();

        try {

            // The final step of the handler chain will be the adapter that will convert attoparser's handler chain to thymeleaf's.
            IMarkupHandler handler =
                        new TemplateHandlerAdapterMarkupHandler(
                                templateResourceName,
                                topLevel,
                                templateHandler,
                                configuration.getTextRepository(),
                                configuration.getElementDefinitions(),
                                configuration.getAttributeDefinitions(),
                                templateMode);

            // If we need to select blocks, we will need a block selector here. Note this will get executed in the
            // handler chain AFTER thymeleaf's own TemplateHandlerAdapterMarkupHandler, so that we will be able to
            // include in selectors code inside prototype-only comments.
            if (selectors != null) {

                final String standardDialectPrefix = configuration.getStandardDialectPrefix();

                final TemplateFragmentMarkupReferenceResolver referenceResolver =
                        (standardDialectPrefix != null ?
                            TemplateFragmentMarkupReferenceResolver.forPrefix(this.html, standardDialectPrefix) : null);
                handler = new BlockSelectorMarkupHandler(handler, selectors, referenceResolver);
            }

            // This is the point at which we insert thymeleaf's own handler, which will take care of parser-level
            // and prototype-only comments.
            handler = new ThymeleafMarkupHandler(handler, templateResourceName);

            // Each type of resource will require a different parser method to be called.
            final Reader templateReader;
            if (templateResource instanceof ReaderResource) {

                templateReader = new ThymeleafMarkupTemplateReader(((ReaderResource)templateResource).getContent());

            } else if (templateResource instanceof StringResource) {

                templateReader = new ThymeleafMarkupTemplateReader(new StringReader(((StringResource)templateResource).getContent()));

            } else if (templateResource instanceof CharArrayResource) {

                final CharArrayResource charArrayResource = (CharArrayResource) templateResource;
                final CharArrayReader charArrayReader =
                        new CharArrayReader(charArrayResource.getContent(), charArrayResource.getOffset(), charArrayResource.getLen());
                templateReader = new ThymeleafMarkupTemplateReader(charArrayReader);

            } else {

                throw new IllegalArgumentException(
                        "Cannot parse: unrecognized " + IResource.class.getSimpleName() + " implementation: " + templateResource.getClass().getName());

            }

            this.parser.parse(templateReader, handler);


        } catch (final ParseException e) {
            final String message = "An error happened during template parsing";
            if (e.getLine() != null && e.getCol() != null) {
                throw new TemplateInputException(message, templateResource.getName(), e.getLine().intValue(), e.getCol().intValue(), e);
            }
            throw new TemplateInputException(message, templateResource.getName(), e);
        }

    }






    /*
     * ---------------------------
     * HANDLER IMPLEMENTATION
     * ---------------------------
     */



    protected static final class ThymeleafMarkupHandler extends AbstractChainedMarkupHandler {

        private static final Logger logger = LoggerFactory.getLogger(AbstractMarkupTemplateParser.class);

        private final String templateName;

        private ParseStatus parseStatus;

        /*
         * These structures help processing (or more specifically, not-processing) parser-level comment blocks,
         * which contents (reported as Text because the parser will be disabled inside them) should be completely
         * ignored.
         */
        private static final char[] PARSER_LEVEL_COMMENT_CLOSE = "*/-->".toCharArray();
        private boolean inParserLevelCommentBlock = false;





        protected ThymeleafMarkupHandler(
                final IMarkupHandler next, final String templateName) {

            // We need to adapt the AttoParser adapter to Thymeleaf's own, in a way that causes the less
            // disturbance to the parser, so we just chain a specific-purpose adapter handler.
            super(next);

            this.templateName = templateName;

        }




        /*
         * -----------------
         * Handler maintenance methods
         * -----------------
         */

        @Override
        public void setParseStatus(final ParseStatus status) {
            this.parseStatus = status;
            super.setParseStatus(status);
        }




        /*
         * -----------------
         * Document handling
         * -----------------
         */

        @Override
        public void handleDocumentStart(
                final long startTimeNanos, final int line, final int col) throws ParseException {
            
            super.handleDocumentStart(startTimeNanos, line, col);

        }



        @Override
        public void handleDocumentEnd(
                final long endTimeNanos, final long totalTimeNanos,
                final int line, final int col)
                throws ParseException {

            super.handleDocumentEnd(endTimeNanos, totalTimeNanos, line, col);

            if (logger.isTraceEnabled()) {
                final BigDecimal elapsed = BigDecimal.valueOf(totalTimeNanos);
                final BigDecimal elapsedMs = elapsed.divide(BigDecimal.valueOf(1000000), RoundingMode.HALF_UP);
                if (this.templateName == null) {
                    logger.trace("[THYMELEAF][{}][{}][{}] Processed unnamed template or fragment in {} nanoseconds (approx. {}ms)",
                            new Object[] {TemplateEngine.threadIndex(),
                                            elapsed, elapsedMs,
                                            elapsed, elapsedMs});
                } else {
                    logger.trace("[THYMELEAF][{}][{}][{}][{}] Processed template \"{}\" in {} nanoseconds (approx. {}ms)",
                            new Object[] {TemplateEngine.threadIndex(),
                                            this.templateName, elapsed, elapsedMs,
                                            this.templateName, elapsed, elapsedMs});
                }
            }

        }




        /*
         * -------------
         * Text handling
         * -------------
         */


        @Override
        public void handleText(
                final char[] buffer,
                final int offset, final int len,
                final int line, final int col)
                throws ParseException {

            if (this.inParserLevelCommentBlock) {
                // We are inside a parser-level comment block, which contents are being reported as text
                // because parsing has been disabled. Simply ignore unless the node starts with the closing sequence
                // of the parser-level comment block, in which case we just remove this sequence, put the flag
                // to false and handle the rest of the Text.

                for (int i = 0; i < PARSER_LEVEL_COMMENT_CLOSE.length; i++) {
                    if (buffer[offset + i] != PARSER_LEVEL_COMMENT_CLOSE[i]) {
                        // Ignore the Text event
                        return;
                    }
                }

                // We actually found the end of the parser-level comment block, so we should just process the rest of the Text node
                this.inParserLevelCommentBlock = false;
                if (len - PARSER_LEVEL_COMMENT_CLOSE.length > 0) {

                    super.handleText(
                            buffer,
                            offset + PARSER_LEVEL_COMMENT_CLOSE.length, len - PARSER_LEVEL_COMMENT_CLOSE.length,
                            line, col + PARSER_LEVEL_COMMENT_CLOSE.length);

                }

                return; // No text left to handle

            }

            super.handleText(
                    buffer, offset, len, line, col);

        }




        /*
         * ----------------
         * Comment handling
         * ----------------
         */


        @Override
        public void handleComment(
                final char[] buffer,
                final int contentOffset, final int contentLen,
                final int outerOffset, final int outerLen,
                final int line, final int col)
                throws ParseException {

            if (isParserLevelCommentStartBlock(buffer, contentOffset, contentLen)) {
                handleParserLevelComment(buffer, contentOffset, contentLen, outerOffset, outerLen, line, col);
                return;
            }

            handleNormalComment(buffer, contentOffset, contentLen, outerOffset, outerLen, line, col);

        }


        private static boolean isParserLevelCommentStartBlock(
                final char[] buffer, final int contentOffset, final int contentLen) {

            // This check must always be executed AFTER checking for prototype-only comment blocks
            // Note we only look for the starting sequence of the block, as we will disable the parser
            // until we find the closing sequence ('*/-->') [note the inner content will be reported
            // as text, and we should ignore it]
            return (buffer[contentOffset] == '/' &&
                    buffer[contentOffset + 1] == '*');

        }


        private static boolean isParserLevelCommentEndBlock(
                final char[] buffer, final int contentOffset, final int contentLen) {

            // This check must always be executed AFTER checking for prototype-only comment blocks
            // This is used in order to determine whether the same comment block starts AND ends the parser-level
            // comment, because in this case we should not involve Text handling in this operation
            return (buffer[contentOffset + contentLen - 2] == '*' &&
                    buffer[contentOffset + contentLen - 1] == '/');

        }


        private void handleNormalComment(
                final char[] buffer,
                final int contentOffset, final int contentLen,
                final int outerOffset, final int outerLen,
                final int line, final int col)
                throws ParseException {

            super.handleComment(
                    buffer, contentOffset, contentLen, outerOffset, outerLen, line, col);

        }


        private void handleParserLevelComment(
                final char[] buffer,
                final int contentOffset, final int contentLen,
                final int outerOffset, final int outerLen,
                final int line, final int col)
                throws ParseException {

            if (isParserLevelCommentEndBlock(buffer, contentOffset, contentLen)) {
                // This block both starts AND ends the parser-level comment, so ignoring it
                // should be enough, without involving any text handling events
                return;
            }

            // Comment blocks of this type provoke the disabling of the parser until we find the
            // closing sequence ('*/-->'), which might appear in a different block of code
            this.inParserLevelCommentBlock = true;

            // Disable parsing until we find the end of the parser-level comment block
            this.parseStatus.setParsingDisabled(PARSER_LEVEL_COMMENT_CLOSE);

        }


    }

    
    
    
}
