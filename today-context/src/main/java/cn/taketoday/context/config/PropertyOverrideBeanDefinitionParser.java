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

import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.config.PropertyOverrideConfigurer;
import cn.taketoday.beans.factory.xml.ParserContext;

/**
 * Parser for the &lt;context:property-override/&gt; element.
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @since 4.0
 */
class PropertyOverrideBeanDefinitionParser extends AbstractPropertyLoadingBeanDefinitionParser {

  @Override
  protected Class<?> getBeanClass(Element element) {
    return PropertyOverrideConfigurer.class;
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    super.doParse(element, parserContext, builder);

    builder.addPropertyValue("ignoreInvalidKeys",
            Boolean.valueOf(element.getAttribute("ignore-unresolvable")));

  }

}
