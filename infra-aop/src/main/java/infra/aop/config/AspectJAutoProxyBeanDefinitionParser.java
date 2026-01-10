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

package infra.aop.config;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.TypedStringValue;
import infra.beans.factory.support.ManagedList;
import infra.beans.factory.xml.BeanDefinitionParser;
import infra.beans.factory.xml.ParserContext;

/**
 * {@link BeanDefinitionParser} for the {@code aspectj-autoproxy} tag,
 * enabling the automatic application of @AspectJ-style aspects found in
 * the {@link BeanFactory}.
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
