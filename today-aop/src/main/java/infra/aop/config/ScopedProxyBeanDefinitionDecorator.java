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

package infra.aop.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import infra.aop.scope.ScopedProxyUtils;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.parsing.BeanComponentDefinition;
import infra.beans.factory.xml.BeanDefinitionDecorator;
import infra.beans.factory.xml.ParserContext;

/**
 * {@link BeanDefinitionDecorator} responsible for parsing the
 * {@code <aop:scoped-proxy/>} tag.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:23
 */
class ScopedProxyBeanDefinitionDecorator implements BeanDefinitionDecorator {

  private static final String PROXY_TARGET_CLASS = "proxy-target-class";

  @Override
  public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
    boolean proxyTargetClass = true;
    if (node instanceof Element ele) {
      if (ele.hasAttribute(PROXY_TARGET_CLASS)) {
        proxyTargetClass = Boolean.parseBoolean(ele.getAttribute(PROXY_TARGET_CLASS));
      }
    }

    // Register the original bean definition as it will be referenced by the scoped proxy
    // and is relevant for tooling (validation, navigation).
    BeanDefinitionHolder holder =
            ScopedProxyUtils.createScopedProxy(definition, parserContext.getRegistry(), proxyTargetClass);
    String targetBeanName = ScopedProxyUtils.getTargetBeanName(definition.getBeanName());
    parserContext.getReaderContext().fireComponentRegistered(
            new BeanComponentDefinition(definition.getBeanDefinition(), targetBeanName));
    return holder;
  }

}
