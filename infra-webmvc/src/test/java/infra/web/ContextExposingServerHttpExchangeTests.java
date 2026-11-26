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

package infra.web;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import infra.context.ApplicationContext;
import infra.context.support.StaticApplicationContext;
import infra.web.mock.MockServerHttpExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/7 09:45
 */
class ContextExposingServerHttpExchangeTests {

  @Test
  void getAttributeReturnsBeanFromApplicationContext() {
    MockServerHttpExchange mockRequest = new MockServerHttpExchange();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("testBean", "testValue");

    ContextExposingServerHttpExchange exchange = new ContextExposingServerHttpExchange(mockRequest, context, null);

    Object attribute = exchange.getAttribute("testBean");

    assertThat(attribute).isEqualTo("testValue");
  }

  @Test
  void getAttributeReturnsNullForNonExistentBean() {
    MockServerHttpExchange mockRequest = new MockServerHttpExchange();
    StaticApplicationContext context = new StaticApplicationContext();

    ContextExposingServerHttpExchange exchange = new ContextExposingServerHttpExchange(mockRequest, context, null);

    Object attribute = exchange.getAttribute("nonExistentBean");

    assertThat(attribute).isNull();
  }

  @Test
  void getAttributeReturnsExplicitAttributeOverBean() {
    MockServerHttpExchange mockRequest = new MockServerHttpExchange();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("testBean", "contextValue");

    ContextExposingServerHttpExchange exchange = new ContextExposingServerHttpExchange(mockRequest, context, null);
    exchange.setAttribute("testBean", "explicitValue");

    Object attribute = exchange.getAttribute("testBean");

    assertThat(attribute).isEqualTo("explicitValue");
  }

  @Test
  void getAttributeWithExposedBeanNamesOnlyReturnsExposedBeans() {
    MockServerHttpExchange mockRequest = new MockServerHttpExchange();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("exposedBean", "exposedValue");
    context.registerSingleton("hiddenBean", "hiddenValue");

    Set<String> exposedNames = new HashSet<>();
    exposedNames.add("exposedBean");

    ContextExposingServerHttpExchange exchange = new ContextExposingServerHttpExchange(mockRequest, context, exposedNames);

    Object exposedAttribute = exchange.getAttribute("exposedBean");
    Object hiddenAttribute = exchange.getAttribute("hiddenBean");

    assertThat(exposedAttribute).isEqualTo("exposedValue");
    assertThat(hiddenAttribute).isNull();
  }

  @Test
  void setAttributeStoresExplicitAttribute() {
    MockServerHttpExchange mockRequest = new MockServerHttpExchange();
    StaticApplicationContext context = new StaticApplicationContext();

    ContextExposingServerHttpExchange exchange = new ContextExposingServerHttpExchange(mockRequest, context, null);
    exchange.setAttribute("explicitAttr", "explicitValue");

    Object attribute = exchange.getAttribute("explicitAttr");

    assertThat(attribute).isEqualTo("explicitValue");
  }

  @Test
  void getApplicationContextReturnsCorrectContext() {
    MockServerHttpExchange mockRequest = new MockServerHttpExchange();
    StaticApplicationContext context = new StaticApplicationContext();

    ContextExposingServerHttpExchange exchange = new ContextExposingServerHttpExchange(mockRequest, context, null);

    ApplicationContext returnedContext = exchange.getApplicationContext();

    assertThat(returnedContext).isSameAs(context);
  }

  @Test
  void constructorWithNullContextThrowsException() {
    MockServerHttpExchange mockRequest = new MockServerHttpExchange();

    assertThatThrownBy(() -> new ContextExposingServerHttpExchange(mockRequest, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ApplicationContext is required");
  }

  @Test
  void getAttributeWorksWithEmptyExposedBeanNamesSet() {
    MockServerHttpExchange mockRequest = new MockServerHttpExchange();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("testBean", "testValue");

    ContextExposingServerHttpExchange exchange = new ContextExposingServerHttpExchange(mockRequest, context, new HashSet<>());

    Object attribute = exchange.getAttribute("testBean");

    assertThat(attribute).isNull();
  }

  @Test
  void multipleSetAttributeCallsWorkCorrectly() {
    MockServerHttpExchange mockRequest = new MockServerHttpExchange();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("bean1", "value1");
    context.registerSingleton("bean2", "value2");

    ContextExposingServerHttpExchange exchange = new ContextExposingServerHttpExchange(mockRequest, context, null);
    exchange.setAttribute("explicit1", "explicitValue1");
    exchange.setAttribute("explicit2", "explicitValue2");

    Object attr1 = exchange.getAttribute("explicit1");
    Object attr2 = exchange.getAttribute("explicit2");
    Object bean1 = exchange.getAttribute("bean1");
    Object bean2 = exchange.getAttribute("bean2");

    assertThat(attr1).isEqualTo("explicitValue1");
    assertThat(attr2).isEqualTo("explicitValue2");
    assertThat(bean1).isEqualTo("value1");
    assertThat(bean2).isEqualTo("value2");
  }

}