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

package infra.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import infra.mock.web.HttpMockRequestImpl;
import infra.ui.ModelMap;
import infra.web.BindingContext;
import infra.web.ResolvableMethod;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/20 10:43
 */
class MapMethodProcessorTests {

  private MapMethodProcessor processor;

  private BindingContext mavContainer;

  private MockRequestContext webRequest;

  private final ResolvableMethod resolvable =
          ResolvableMethod.on(getClass()).annotPresent(RequestMapping.class).build();

  HandlerMethod handlerMethod = new HandlerMethod(this, resolvable.method());

  @BeforeEach
  public void setUp() throws Exception {
    this.processor = new MapMethodProcessor();
    this.mavContainer = new BindingContext();
    this.webRequest = new MockRequestContext(null, new HttpMockRequestImpl(), null);
    webRequest.setBinding(mavContainer);

  }

  @Test
  public void supportsParameter() {
    assertThat(this.processor.supportsParameter(
            this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class))).isTrue();
    assertThat(this.processor.supportsParameter(
            this.resolvable.annotPresent(RequestBody.class).arg(Map.class, String.class, Object.class))).isFalse();
  }

  @Test
  public void supportsReturnType() {
    assertThat(this.processor.supportsHandler(handlerMethod)).isTrue();
  }

  @Test
  public void resolveArgumentValue() throws Throwable {
    ResolvableMethodParameter param = this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class);
    assertThat(this.processor.resolveArgument(webRequest, param)).isSameAs(this.mavContainer.getModel());
  }

  @Test
  public void handleMapReturnValue() throws Exception {
    this.mavContainer.addAttribute("attr1", "value1");
    Map<String, Object> returnValue = new ModelMap("attr2", "value2");

    this.processor.handleReturnValue(webRequest, handlerMethod, returnValue);

    assertThat(mavContainer.getModel().get("attr1")).isEqualTo("value1");
    assertThat(mavContainer.getModel().get("attr2")).isEqualTo("value2");
  }

  @Test
  public void supportsParameterWithAnnotation() {
    assertThat(this.processor.supportsParameter(
            this.resolvable.annotPresent(RequestBody.class).arg(Map.class, String.class, Object.class))).isFalse();
  }

  @Test
  public void resolveArgumentValueWithBindingContext() throws Throwable {
    ResolvableMethodParameter param = this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class);
    Object resolved = this.processor.resolveArgument(webRequest, param);
    assertThat(resolved).isSameAs(this.mavContainer.getModel());
  }

  @Test
  public void handleNullReturnValue() throws Exception {
    this.processor.handleReturnValue(webRequest, handlerMethod, null);
    assertThat(mavContainer.getModel()).isEmpty();
  }

  @Test
  public void handleNonMapReturnValue() throws Exception {
    try {
      this.processor.handleReturnValue(webRequest, handlerMethod, "not a map");
    }
    catch (UnsupportedOperationException e) {
      assertThat(e.getMessage()).contains("Unexpected return type");
    }
  }

  @Test
  public void supportsHandlerMethodWithMapReturnType() {
    assertThat(this.processor.supportsHandlerMethod(handlerMethod)).isTrue();
  }

  @Test
  public void supportsHandlerMethodWithNonMapReturnType() {
    HandlerMethod nonMapHandler = new HandlerMethod(this,
            ResolvableMethod.on(TestC.class).named("nonMapReturn").build().method());
    assertThat(this.processor.supportsHandlerMethod(nonMapHandler)).isFalse();
    assertThat(processor.supportsReturnValue(new HashMap<>())).isTrue();
  }

  @Test
  public void supportsParameterWithNonStringKey() {
    ResolvableMethodParameter param = this.resolvable.annotNotPresent().arg(Map.class, Integer.class, Object.class);
    assertThat(this.processor.supportsParameter(param)).isFalse();
  }

  @Test
  public void supportsParameterWithNonObjectValue() {
    ResolvableMethodParameter param = this.resolvable.annotNotPresent().arg(Map.class, String.class, String.class);
    assertThat(this.processor.supportsParameter(param)).isFalse();
  }

  @Test
  public void supportsParameterWithRawMap() {
    ResolvableMethodParameter param = this.resolvable.annotNotPresent().arg(Map.class);
    assertThat(this.processor.supportsParameter(param)).isFalse();
  }

  @Test
  public void supportsParameterWithSubclassOfMap() {
    ResolvableMethodParameter param = this.resolvable.annotNotPresent().arg(ModelMap.class);
    assertThat(this.processor.supportsParameter(param)).isFalse();
  }

  @Test
  public void supportsReturnValueWithMap() {
    assertThat(this.processor.supportsReturnValue(new ModelMap())).isTrue();
  }

  @Test
  public void supportsReturnValueWithNull() {
    assertThat(this.processor.supportsReturnValue(null)).isFalse();
  }

  @Test
  public void supportsReturnValueWithNonMap() {
    assertThat(this.processor.supportsReturnValue("string")).isFalse();
  }

  @Test
  public void handleMapReturnValueMergesAttributes() throws Exception {
    this.mavContainer.addAttribute("existing", "value");
    Map<String, Object> returnValue = Map.of("new", "attribute");

    this.processor.handleReturnValue(webRequest, handlerMethod, returnValue);

    assertThat(mavContainer.getModel()).containsEntry("existing", "value");
    assertThat(mavContainer.getModel()).containsEntry("new", "attribute");
  }

  @Test
  public void handleReturnValueWhenBindingContextIsNull() throws Exception {
    MockRequestContext request = new MockRequestContext(null, new HttpMockRequestImpl(), null);
    // Not setting binding context to simulate null case

    Map<String, Object> returnValue = Map.of("key", "value");
    this.processor.handleReturnValue(request, handlerMethod, returnValue);
    // Should not throw exception
  }

  @Test
  public void resolveArgumentReturnsSameInstance() throws Throwable {
    ResolvableMethodParameter param = this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class);
    Object result = this.processor.resolveArgument(webRequest, param);
    assertThat(result).isInstanceOf(Map.class);
    assertThat(result).isSameAs(webRequest.binding().getModel());
  }

  @SuppressWarnings("unused")
  @RequestMapping
  private Map<String, Object> handle(
          Map<String, Object> map,
          @RequestBody Map<String, Object> annotMap,
          Map<Integer, Object> annotMap2,
          Map<String, String> annotMap3,
          Map map1, ModelMap modelMap) {

    return null;
  }

  static class TestC {
    @RequestMapping("/1")
    private String nonMapReturn() {
      return null;
    }

  }

}
