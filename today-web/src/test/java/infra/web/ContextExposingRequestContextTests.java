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

package infra.web;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import infra.context.ApplicationContext;
import infra.context.support.StaticApplicationContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/7 09:45
 */
class ContextExposingRequestContextTests {

  @Test
  void getAttributeReturnsBeanFromApplicationContext() {
    MockRequestContext mockRequest = new MockRequestContext();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("testBean", "testValue");

    ContextExposingRequestContext requestContext = new ContextExposingRequestContext(mockRequest, context, null);

    Object attribute = requestContext.getAttribute("testBean");

    assertThat(attribute).isEqualTo("testValue");
  }

  @Test
  void getAttributeReturnsNullForNonExistentBean() {
    MockRequestContext mockRequest = new MockRequestContext();
    StaticApplicationContext context = new StaticApplicationContext();

    ContextExposingRequestContext requestContext = new ContextExposingRequestContext(mockRequest, context, null);

    Object attribute = requestContext.getAttribute("nonExistentBean");

    assertThat(attribute).isNull();
  }

  @Test
  void getAttributeReturnsExplicitAttributeOverBean() {
    MockRequestContext mockRequest = new MockRequestContext();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("testBean", "contextValue");

    ContextExposingRequestContext requestContext = new ContextExposingRequestContext(mockRequest, context, null);
    requestContext.setAttribute("testBean", "explicitValue");

    Object attribute = requestContext.getAttribute("testBean");

    assertThat(attribute).isEqualTo("explicitValue");
  }

  @Test
  void getAttributeWithExposedBeanNamesOnlyReturnsExposedBeans() {
    MockRequestContext mockRequest = new MockRequestContext();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("exposedBean", "exposedValue");
    context.registerSingleton("hiddenBean", "hiddenValue");

    Set<String> exposedNames = new HashSet<>();
    exposedNames.add("exposedBean");

    ContextExposingRequestContext requestContext = new ContextExposingRequestContext(mockRequest, context, exposedNames);

    Object exposedAttribute = requestContext.getAttribute("exposedBean");
    Object hiddenAttribute = requestContext.getAttribute("hiddenBean");

    assertThat(exposedAttribute).isEqualTo("exposedValue");
    assertThat(hiddenAttribute).isNull();
  }

  @Test
  void setAttributeStoresExplicitAttribute() {
    MockRequestContext mockRequest = new MockRequestContext();
    StaticApplicationContext context = new StaticApplicationContext();

    ContextExposingRequestContext requestContext = new ContextExposingRequestContext(mockRequest, context, null);
    requestContext.setAttribute("explicitAttr", "explicitValue");

    Object attribute = requestContext.getAttribute("explicitAttr");

    assertThat(attribute).isEqualTo("explicitValue");
  }

  @Test
  void getApplicationContextReturnsCorrectContext() {
    MockRequestContext mockRequest = new MockRequestContext();
    StaticApplicationContext context = new StaticApplicationContext();

    ContextExposingRequestContext requestContext = new ContextExposingRequestContext(mockRequest, context, null);

    ApplicationContext returnedContext = requestContext.getApplicationContext();

    assertThat(returnedContext).isSameAs(context);
  }

  @Test
  void constructorWithNullContextThrowsException() {
    MockRequestContext mockRequest = new MockRequestContext();

    assertThatThrownBy(() -> new ContextExposingRequestContext(mockRequest, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WebApplicationContext is required");
  }

  @Test
  void getAttributeWorksWithEmptyExposedBeanNamesSet() {
    MockRequestContext mockRequest = new MockRequestContext();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("testBean", "testValue");

    ContextExposingRequestContext requestContext = new ContextExposingRequestContext(mockRequest, context, new HashSet<>());

    Object attribute = requestContext.getAttribute("testBean");

    assertThat(attribute).isNull();
  }

  @Test
  void multipleSetAttributeCallsWorkCorrectly() {
    MockRequestContext mockRequest = new MockRequestContext();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("bean1", "value1");
    context.registerSingleton("bean2", "value2");

    ContextExposingRequestContext requestContext = new ContextExposingRequestContext(mockRequest, context, null);
    requestContext.setAttribute("explicit1", "explicitValue1");
    requestContext.setAttribute("explicit2", "explicitValue2");

    Object attr1 = requestContext.getAttribute("explicit1");
    Object attr2 = requestContext.getAttribute("explicit2");
    Object bean1 = requestContext.getAttribute("bean1");
    Object bean2 = requestContext.getAttribute("bean2");

    assertThat(attr1).isEqualTo("explicitValue1");
    assertThat(attr2).isEqualTo("explicitValue2");
    assertThat(bean1).isEqualTo("value1");
    assertThat(bean2).isEqualTo("value2");
  }

}