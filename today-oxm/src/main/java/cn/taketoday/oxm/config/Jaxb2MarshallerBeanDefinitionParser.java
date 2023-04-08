/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.oxm.config;

import org.w3c.dom.Element;

import java.util.List;

import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.ManagedList;
import cn.taketoday.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.xml.DomUtils;

/**
 * Parser for the {@code <oxm:jaxb2-marshaller/>} element.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
class Jaxb2MarshallerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

  @Override
  protected String getBeanClassName(Element element) {
    return "cn.taketoday.oxm.jaxb.Jaxb2Marshaller";
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder beanDefinitionBuilder) {
    String contextPath = element.getAttribute("context-path");
    if (StringUtils.hasText(contextPath)) {
      beanDefinitionBuilder.addPropertyValue("contextPath", contextPath);
    }

    List<Element> classes = DomUtils.getChildElementsByTagName(element, "class-to-be-bound");
    if (!classes.isEmpty()) {
      ManagedList<String> classesToBeBound = new ManagedList<>(classes.size());
      for (Element classToBeBound : classes) {
        String className = classToBeBound.getAttribute("name");
        classesToBeBound.add(className);
      }
      beanDefinitionBuilder.addPropertyValue("classesToBeBound", classesToBeBound);
    }
  }

}
