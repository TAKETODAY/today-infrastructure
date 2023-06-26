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

import cn.taketoday.beans.factory.config.PropertyPlaceholderConfigurer;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.util.StringUtils;

/**
 * Parser for the {@code <context:property-placeholder/>} element.
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @since 4.0
 */
class PropertyPlaceholderBeanDefinitionParser extends AbstractPropertyLoadingBeanDefinitionParser {

  private static final String SYSTEM_PROPERTIES_MODE_ATTRIBUTE = "system-properties-mode";

  private static final String SYSTEM_PROPERTIES_MODE_DEFAULT = "ENVIRONMENT";

  @Override
  protected Class<?> getBeanClass(Element element) {
    // the default value of system-properties-mode has changed from
    // 'FALLBACK' to 'ENVIRONMENT'. This latter value indicates that resolution of
    // placeholders against system properties is a function of the Environment and
    // its current set of PropertySources.
    if (SYSTEM_PROPERTIES_MODE_DEFAULT.equals(element.getAttribute(SYSTEM_PROPERTIES_MODE_ATTRIBUTE))) {
      return PropertySourcesPlaceholderConfigurer.class;
    }

    // The user has explicitly specified a value for system-properties-mode: revert to
    // PropertyPlaceholderConfigurer to ensure backward compatibility with 3.0 and earlier.
    // This is deprecated; to be removed along with PropertyPlaceholderConfigurer itself.
    return PropertyPlaceholderConfigurer.class;
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    super.doParse(element, parserContext, builder);

    builder.addPropertyValue("ignoreUnresolvablePlaceholders",
            Boolean.valueOf(element.getAttribute("ignore-unresolvable")));

    String systemPropertiesModeName = element.getAttribute(SYSTEM_PROPERTIES_MODE_ATTRIBUTE);
    if (StringUtils.isNotEmpty(systemPropertiesModeName)
            && !systemPropertiesModeName.equals(SYSTEM_PROPERTIES_MODE_DEFAULT)) {
      builder.addPropertyValue("systemPropertiesModeName", "SYSTEM_PROPERTIES_MODE_" + systemPropertiesModeName);
    }

    if (element.hasAttribute("value-separator")) {
      builder.addPropertyValue("valueSeparator", element.getAttribute("value-separator"));
    }
    if (element.hasAttribute("trim-values")) {
      builder.addPropertyValue("trimValues", element.getAttribute("trim-values"));
    }
    if (element.hasAttribute("null-value")) {
      builder.addPropertyValue("nullValue", element.getAttribute("null-value"));
    }
  }

}
