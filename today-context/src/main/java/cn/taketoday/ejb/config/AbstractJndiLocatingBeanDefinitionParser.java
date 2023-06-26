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

package cn.taketoday.ejb.config;

import org.w3c.dom.Element;

import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.xml.DomUtils;

import static cn.taketoday.beans.factory.xml.BeanDefinitionParserDelegate.DEFAULT_VALUE;
import static cn.taketoday.beans.factory.xml.BeanDefinitionParserDelegate.LAZY_INIT_ATTRIBUTE;
import static cn.taketoday.beans.factory.xml.BeanDefinitionParserDelegate.TRUE_VALUE;

/**
 * Abstract base class for BeanDefinitionParsers which build
 * JNDI-locating beans, supporting an optional "jndiEnvironment"
 * bean property, populated from an "environment" XML sub-element.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class AbstractJndiLocatingBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

  public static final String ENVIRONMENT = "environment";

  public static final String ENVIRONMENT_REF = "environment-ref";

  public static final String JNDI_ENVIRONMENT = "jndiEnvironment";

  @Override
  protected boolean isEligibleAttribute(String attributeName) {
    return (super.isEligibleAttribute(attributeName) &&
            !ENVIRONMENT_REF.equals(attributeName) &&
            !LAZY_INIT_ATTRIBUTE.equals(attributeName));
  }

  @Override
  protected void postProcess(BeanDefinitionBuilder definitionBuilder, Element element) {
    Object envValue = DomUtils.getChildElementValueByTagName(element, ENVIRONMENT);
    if (envValue != null) {
      // Specific environment settings defined, overriding any shared properties.
      definitionBuilder.addPropertyValue(JNDI_ENVIRONMENT, envValue);
    }
    else {
      // Check whether there is a reference to shared environment properties...
      String envRef = element.getAttribute(ENVIRONMENT_REF);
      if (StringUtils.isNotEmpty(envRef)) {
        definitionBuilder.addPropertyValue(JNDI_ENVIRONMENT, new RuntimeBeanReference(envRef));
      }
    }

    String lazyInit = element.getAttribute(LAZY_INIT_ATTRIBUTE);
    if (StringUtils.hasText(lazyInit) && !DEFAULT_VALUE.equals(lazyInit)) {
      definitionBuilder.setLazyInit(TRUE_VALUE.equals(lazyInit));
    }

    definitionBuilder.setEnableDependencyInjection(false);
  }
}
