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

package infra.ejb.config;

import org.w3c.dom.Element;

import infra.beans.factory.config.RuntimeBeanReference;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.xml.BeanDefinitionParser;
import infra.beans.factory.xml.ParserContext;
import infra.jndi.JndiObjectFactoryBean;
import infra.util.StringUtils;

/**
 * Simple {@link BeanDefinitionParser} implementation that
 * translates {@code jndi-lookup} tag into {@link JndiObjectFactoryBean} definitions.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see JndiObjectFactoryBean
 * @since 4.0
 */
class JndiLookupBeanDefinitionParser extends AbstractJndiLocatingBeanDefinitionParser {

  public static final String DEFAULT_VALUE = "default-value";

  public static final String DEFAULT_REF = "default-ref";

  public static final String DEFAULT_OBJECT = "defaultObject";

  @Override
  protected Class<?> getBeanClass(Element element) {
    return JndiObjectFactoryBean.class;
  }

  @Override
  protected boolean isEligibleAttribute(String attributeName) {
    return (super.isEligibleAttribute(attributeName)
            && !DEFAULT_VALUE.equals(attributeName) && !DEFAULT_REF.equals(attributeName));
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    super.doParse(element, parserContext, builder);

    String defaultValue = element.getAttribute(DEFAULT_VALUE);
    String defaultRef = element.getAttribute(DEFAULT_REF);
    if (StringUtils.isNotEmpty(defaultValue)) {
      if (StringUtils.isNotEmpty(defaultRef)) {
        parserContext.getReaderContext().error("<jndi-lookup> element is only allowed to contain either " +
                "'default-value' attribute OR 'default-ref' attribute, not both", element);
      }
      builder.addPropertyValue(DEFAULT_OBJECT, defaultValue);
    }
    else if (StringUtils.isNotEmpty(defaultRef)) {
      builder.addPropertyValue(DEFAULT_OBJECT, new RuntimeBeanReference(defaultRef));
    }
    builder.setEnableDependencyInjection(false);
  }

}
