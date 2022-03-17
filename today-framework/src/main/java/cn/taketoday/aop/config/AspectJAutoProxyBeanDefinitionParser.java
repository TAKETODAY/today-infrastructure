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

package cn.taketoday.aop.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.TypedStringValue;
import cn.taketoday.beans.factory.support.ManagedList;
import cn.taketoday.beans.factory.xml.BeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.lang.Nullable;

/**
 * {@link BeanDefinitionParser} for the {@code aspectj-autoproxy} tag,
 * enabling the automatic application of @AspectJ-style aspects found in
 * the {@link cn.taketoday.beans.factory.BeanFactory}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
class AspectJAutoProxyBeanDefinitionParser implements BeanDefinitionParser {

  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    AopNamespaceUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(parserContext, element);
    extendBeanDefinition(element, parserContext);
    return null;
  }

  private void extendBeanDefinition(Element element, ParserContext parserContext) {
    BeanDefinition beanDef = parserContext.getRegistry().getBeanDefinition(
            AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
    if (element.hasChildNodes()) {
      addIncludePatterns(element, parserContext, beanDef);
    }
  }

  private void addIncludePatterns(Element element, ParserContext parserContext, BeanDefinition beanDef) {
    ManagedList<TypedStringValue> includePatterns = new ManagedList<>();
    NodeList childNodes = element.getChildNodes();
    int length = childNodes.getLength();
    for (int i = 0; i < length; i++) {
      Node node = childNodes.item(i);
      if (node instanceof Element includeElement) {
        TypedStringValue valueHolder = new TypedStringValue(includeElement.getAttribute("name"));
        valueHolder.setSource(parserContext.extractSource(includeElement));
        includePatterns.add(valueHolder);
      }
    }
    if (!includePatterns.isEmpty()) {
      includePatterns.setSource(parserContext.extractSource(element));
      beanDef.getPropertyValues().add("includePatterns", includePatterns);
    }
  }

}
