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

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.ResolvableMethod;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.mock.MockRequestContext;

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

  @SuppressWarnings("unused")
  @RequestMapping
  private Map<String, Object> handle(
          Map<String, Object> map,
          @RequestBody Map<String, Object> annotMap) {

    return null;
  }

}
