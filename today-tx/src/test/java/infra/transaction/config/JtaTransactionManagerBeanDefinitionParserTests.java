/*
 * Copyright 2017 - 2026 the TODAY authors.
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