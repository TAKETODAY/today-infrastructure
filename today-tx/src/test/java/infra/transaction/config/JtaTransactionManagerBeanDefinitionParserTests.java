/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.transaction.config;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.xml.ParserContext;
import infra.transaction.jta.JtaTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:21
 */
class JtaTransactionManagerBeanDefinitionParserTests {

  @Test
  void getBeanClassReturnsJtaTransactionManager() {
    JtaTransactionManagerBeanDefinitionParser parser = new JtaTransactionManagerBeanDefinitionParser();
    Element element = null; // Element参数在实际使用中不会为null，这里仅用于测试方法签名

    Class<?> beanClass = parser.getBeanClass(element);

    assertThat(beanClass).isEqualTo(JtaTransactionManager.class);
  }

  @Test
  void resolveIdReturnsDefaultTransactionManagerBeanName() {
    JtaTransactionManagerBeanDefinitionParser parser = new JtaTransactionManagerBeanDefinitionParser();
    Element element = null; // Element参数在实际使用中不会为null，这里仅用于测试方法签名
    AbstractBeanDefinition definition = null; // AbstractBeanDefinition参数在实际使用中不会为null，这里仅用于测试方法签名
    ParserContext parserContext = null; // ParserContext参数在实际使用中不会为null，这里仅用于测试方法签名

    String id = parser.resolveId(element, definition, parserContext);

    assertThat(id).isEqualTo(TxNamespaceHandler.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME);
  }

}