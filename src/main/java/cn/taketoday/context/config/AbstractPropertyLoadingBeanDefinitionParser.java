/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.util.StringUtils;

/**
 * Abstract parser for &lt;context:property-.../&gt; elements.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Dave Syer
 * @since 4.0
 */
abstract class AbstractPropertyLoadingBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

  @Override
  protected boolean shouldGenerateId() {
    return true;
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    String location = element.getAttribute("location");
    if (StringUtils.isNotEmpty(location)) {
      location = parserContext.getReaderContext().getEnvironment().resolvePlaceholders(location);
      String[] locations = StringUtils.commaDelimitedListToStringArray(location);
      builder.addPropertyValue("locations", locations);
    }

    String propertiesRef = element.getAttribute("properties-ref");
    if (StringUtils.isNotEmpty(propertiesRef)) {
      builder.addPropertyReference("properties", propertiesRef);
    }

    String fileEncoding = element.getAttribute("file-encoding");
    if (StringUtils.isNotEmpty(fileEncoding)) {
      builder.addPropertyValue("fileEncoding", fileEncoding);
    }

    String order = element.getAttribute("order");
    if (StringUtils.isNotEmpty(order)) {
      builder.addPropertyValue("order", Integer.valueOf(order));
    }

    builder.addPropertyValue("ignoreResourceNotFound",
            Boolean.valueOf(element.getAttribute("ignore-resource-not-found")));

    builder.addPropertyValue("localOverride",
            Boolean.valueOf(element.getAttribute("local-override")));

    builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
  }

}
