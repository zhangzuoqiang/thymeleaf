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
package org.thymeleaf.engine;

import java.io.IOException;
import java.io.Writer;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.model.IUnmatchedCloseElementTag;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.Validate;

/**
 *
 * @author Daniel Fern&aacute;ndez
 * @since 3.0.0
 *
 */
final class UnmatchedCloseElementTag
            extends AbstractElementTag
            implements IUnmatchedCloseElementTag, IEngineTemplateHandlerEvent {


    /*
     * Objects of this class are meant to both be reused by the engine and also created fresh by the processors. This
     * should allow reducing the number of instances of this class to the minimum.
     */


    // Meant to be called only from the template handler adapter
    UnmatchedCloseElementTag(
            final TemplateMode templateMode,
            final ElementDefinitions elementDefinitions) {
        super(templateMode, elementDefinitions);
    }



    // Meant to be called only from the model factory
    UnmatchedCloseElementTag(
            final TemplateMode templateMode,
            final ElementDefinitions elementDefinitions,
            final String elementName) {
        super(templateMode, elementDefinitions, elementName);
    }



    // Meant to be called only from the cloneElementTag method
    private UnmatchedCloseElementTag() {
        super();
    }




    // Meant to be called only from within the engine
    void setUnmatchedCloseElementTag(
            final String elementName,
            final String templateName, final int line, final int col) {
        resetElementTag(elementName, templateName, line, col);
    }





    public void write(final Writer writer) throws IOException {
        // We will write exactly the same as for non-unmatched close elements, because that does not matter from the markup point
        Validate.notNull(writer, "Writer cannot be null");
        writer.write("</");
        writer.write(this.elementName);
        writer.write('>');
    }





    public UnmatchedCloseElementTag cloneElementTag() {
        final UnmatchedCloseElementTag clone = new UnmatchedCloseElementTag();
        clone.resetAsCloneOf(this);
        return clone;
    }



    // Meant to be called only from within the engine
    void resetAsCloneOf(final UnmatchedCloseElementTag from) {
        super.resetAsCloneOfElementTag(from);
    }



    // Meant to be called only from within the engine
    static UnmatchedCloseElementTag asEngineUnmatchedCloseElementTag(
            final TemplateMode templateMode, final IEngineConfiguration configuration,
            final IUnmatchedCloseElementTag unmatchedCloseElementTag, final boolean cloneAlways) {

        if (unmatchedCloseElementTag instanceof UnmatchedCloseElementTag) {
            if (cloneAlways) {
                return ((UnmatchedCloseElementTag) unmatchedCloseElementTag).cloneElementTag();
            }
            return (UnmatchedCloseElementTag) unmatchedCloseElementTag;
        }

        final UnmatchedCloseElementTag newInstance = new UnmatchedCloseElementTag(templateMode, configuration.getElementDefinitions());
        newInstance.setUnmatchedCloseElementTag(unmatchedCloseElementTag.getElementName(), unmatchedCloseElementTag.getTemplateName(), unmatchedCloseElementTag.getLine(), unmatchedCloseElementTag.getCol());
        return newInstance;

    }

}
