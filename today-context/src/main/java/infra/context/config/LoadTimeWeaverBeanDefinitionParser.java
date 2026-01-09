/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.config;

import org.w3c.dom.Element;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.parsing.BeanComponentDefinition;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import infra.beans.factory.xml.ParserContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.weaving.AspectJWeavingEnabler;

/**
 * Parser for the &lt;context:load-time-weaver/&gt; element.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
class LoadTimeWeaverBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

  /**
   * The bean name of the internally managed AspectJ weaving enabler.
   */
  public static final String ASPECTJ_WEAVING_ENABLER_BEAN_NAME =
          "infra.context.config.internalAspectJWeavingEnabler";

  private static final String ASPECTJ_WEAVING_ENABLER_CLASS_NAME =
          "infra.context.weaving.AspectJWeavingEnabler";

  private static final String DEFAULT_LOAD_TIME_WEAVER_CLASS_NAME =
          "infra.context.weaving.DefaultContextLoadTimeWeaver";

  private static final String WEAVER_CLASS_ATTRIBUTE = "weaver-class";

  private static final String ASPECTJ_WEAVING_ATTRIBUTE = "aspectj-weaving";

  @Override
  protected String getBeanClassName(Element element) {
    if (element.hasAttribute(WEAVER_CLASS_ATTRIBUTE)) {
      return element.getAttribute(WEAVER_CLASS_ATTRIBUTE);
    }
    return DEFAULT_LOAD_TIME_WEAVER_CLASS_NAME;
  }

  @Override
  protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
    return ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME;
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

    if (isAspectJWeavingEnabled(element.getAttribute(ASPECTJ_WEAVING_ATTRIBUTE), parserContext)) {
      if (!parserContext.getRegistry().containsBeanDefinition(ASPECTJ_WEAVING_ENABLER_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(ASPECTJ_WEAVING_ENABLER_CLASS_NAME);
        parserContext.registerBeanComponent(
                new BeanComponentDefinition(def, ASPECTJ_WEAVING_ENABLER_BEAN_NAME));
      }
    }

    builder.setEnableDependencyInjection(false);
  }

  protected boolean isAspectJWeavingEnabled(String value, ParserContext parserContext) {
    if ("on".equals(value)) {
      return true;
    }
    else if ("off".equals(value)) {
      return false;
    }
    else {
      // Determine default...
      ClassLoader cl = parserContext.getReaderContext().getBeanClassLoader();
      return (cl != null && cl.getResource(AspectJWeavingEnabler.ASPECTJ_AOP_XML_RESOURCE) != null);
    }
  }

}
