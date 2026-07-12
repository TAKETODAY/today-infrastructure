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
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/7 09:45
 */
class ContextExposingHttpContextTests {

  @Test
  void getAttributeReturnsBeanFromApplicationContext() {
    MockHttpContext mockRequest = new MockHttpContext();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("testBean", "testValue");

    ContextExposingHttpContext httpContext = new ContextExposingHttpContext(mockRequest, context, null);

    Object attribute = httpContext.getAttribute("testBean");

    assertThat(attribute).isEqualTo("testValue");
  }

  @Test
  void getAttributeReturnsNullForNonExistentBean() {
    MockHttpContext mockRequest = new MockHttpContext();
    StaticApplicationContext context = new StaticApplicationContext();

    ContextExposingHttpContext httpContext = new ContextExposingHttpContext(mockRequest, context, null);

    Object attribute = httpContext.getAttribute("nonExistentBean");

    assertThat(attribute).isNull();
  }

  @Test
  void getAttributeReturnsExplicitAttributeOverBean() {
    MockHttpContext mockRequest = new MockHttpContext();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("testBean", "contextValue");

    ContextExposingHttpContext httpContext = new ContextExposingHttpContext(mockRequest, context, null);
    httpContext.setAttribute("testBean", "explicitValue");

    Object attribute = httpContext.getAttribute("testBean");

    assertThat(attribute).isEqualTo("explicitValue");
  }

  @Test
  void getAttributeWithExposedBeanNamesOnlyReturnsExposedBeans() {
    MockHttpContext mockRequest = new MockHttpContext();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("exposedBean", "exposedValue");
    context.registerSingleton("hiddenBean", "hiddenValue");

    Set<String> exposedNames = new HashSet<>();
    exposedNames.add("exposedBean");

    ContextExposingHttpContext httpContext = new ContextExposingHttpContext(mockRequest, context, exposedNames);

    Object exposedAttribute = httpContext.getAttribute("exposedBean");
    Object hiddenAttribute = httpContext.getAttribute("hiddenBean");

    assertThat(exposedAttribute).isEqualTo("exposedValue");
    assertThat(hiddenAttribute).isNull();
  }

  @Test
  void setAttributeStoresExplicitAttribute() {
    MockHttpContext mockRequest = new MockHttpContext();
    StaticApplicationContext context = new StaticApplicationContext();

    ContextExposingHttpContext httpContext = new ContextExposingHttpContext(mockRequest, context, null);
    httpContext.setAttribute("explicitAttr", "explicitValue");

    Object attribute = httpContext.getAttribute("explicitAttr");

    assertThat(attribute).isEqualTo("explicitValue");
  }

  @Test
  void getApplicationContextReturnsCorrectContext() {
    MockHttpContext mockRequest = new MockHttpContext();
    StaticApplicationContext context = new StaticApplicationContext();

    ContextExposingHttpContext httpContext = new ContextExposingHttpContext(mockRequest, context, null);

    ApplicationContext returnedContext = httpContext.getApplicationContext();

    assertThat(returnedContext).isSameAs(context);
  }

  @Test
  void constructorWithNullContextThrowsException() {
    MockHttpContext mockRequest = new MockHttpContext();

    assertThatThrownBy(() -> new ContextExposingHttpContext(mockRequest, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ApplicationContext is required");
  }

  @Test
  void getAttributeWorksWithEmptyExposedBeanNamesSet() {
    MockHttpContext mockRequest = new MockHttpContext();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("testBean", "testValue");

    ContextExposingHttpContext httpContext = new ContextExposingHttpContext(mockRequest, context, new HashSet<>());

    Object attribute = httpContext.getAttribute("testBean");

    assertThat(attribute).isNull();
  }

  @Test
  void multipleSetAttributeCallsWorkCorrectly() {
    MockHttpContext mockRequest = new MockHttpContext();
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("bean1", "value1");
    context.registerSingleton("bean2", "value2");

    ContextExposingHttpContext httpContext = new ContextExposingHttpContext(mockRequest, context, null);
    httpContext.setAttribute("explicit1", "explicitValue1");
    httpContext.setAttribute("explicit2", "explicitValue2");

    Object attr1 = httpContext.getAttribute("explicit1");
    Object attr2 = httpContext.getAttribute("explicit2");
    Object bean1 = httpContext.getAttribute("bean1");
    Object bean2 = httpContext.getAttribute("bean2");

    assertThat(attr1).isEqualTo("explicitValue1");
    assertThat(attr2).isEqualTo("explicitValue2");
    assertThat(bean1).isEqualTo("value1");
    assertThat(bean2).isEqualTo("value2");
  }

}