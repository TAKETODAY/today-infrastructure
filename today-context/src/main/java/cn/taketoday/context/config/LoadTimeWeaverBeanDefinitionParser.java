/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.config;

import org.w3c.dom.Element;

import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.weaving.AspectJWeavingEnabler;

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
          "cn.taketoday.context.config.internalAspectJWeavingEnabler";

  private static final String ASPECTJ_WEAVING_ENABLER_CLASS_NAME =
          "cn.taketoday.context.weaving.AspectJWeavingEnabler";

  private static final String DEFAULT_LOAD_TIME_WEAVER_CLASS_NAME =
          "cn.taketoday.context.weaving.DefaultContextLoadTimeWeaver";

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
