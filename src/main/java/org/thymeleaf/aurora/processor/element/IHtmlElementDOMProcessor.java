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
package org.thymeleaf.aurora.processor.element;

import org.thymeleaf.aurora.context.IProcessorMatchingContext;
import org.thymeleaf.aurora.context.ITemplateProcessingContext;
import org.thymeleaf.aurora.dom.Node;
import org.thymeleaf.aurora.engine.AttributeName;
import org.thymeleaf.aurora.engine.ElementName;
import org.thymeleaf.aurora.engine.HtmlAttributeName;
import org.thymeleaf.aurora.engine.HtmlElementName;
import org.thymeleaf.aurora.processor.IProcessor;

/**
 *
 * @author Daniel Fern&aacute;ndez
 * @since 3.0.0
 * 
 */
public interface IHtmlElementDOMProcessor extends IProcessor {

    public HtmlElementName getHtmlElementName();
    public HtmlAttributeName getHtmlAttributeName();
    public String getHtmlAttributeValue();


    public void processHtmlElementBefore(
            final ITemplateProcessingContext processingContext, final IProcessorMatchingContext matchingContext, final Node dom);

    public void processHtmlElementAfter(
            final ITemplateProcessingContext processingContext, final IProcessorMatchingContext matchingContext, final Node dom);

}