/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.config;

import org.w3c.dom.Element;

import infra.beans.factory.parsing.BeanComponentDefinition;
import infra.beans.factory.config.BeanDefinition;
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
