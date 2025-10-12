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

package infra.web.handler.function;

import org.junit.jupiter.api.Test;

import infra.http.HttpStatus;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 14:41
 */
class RenderingResponseTests {

  @Test
  public void createWithName() {
    RenderingResponse.Builder builder = RenderingResponse.create("viewName");
    RenderingResponse response = builder.build();

    assertThat(response.name()).isEqualTo("viewName");
  }

  @Test
  public void createWithModelAndView() {
    ModelAndView modelAndView = new ModelAndView("viewName");
    modelAndView.addObject("key", "value");

    RenderingResponse.ViewBuilder builder = RenderingResponse.create(modelAndView);
    RenderingResponse response = builder.build();

    assertThat(response.name()).isEqualTo("viewName");
    assertThat(response.model()).containsEntry("key", "value");
  }

  @Test
  public void fromExistingResponse() {
    ModelAndView modelAndView = new ModelAndView("originalView");
    modelAndView.addObject("originalKey", "originalValue");

    RenderingResponse originalResponse = RenderingResponse.create(modelAndView)
            .status(HttpStatus.NOT_FOUND)
            .header("X-Custom", "original")
            .build();

    RenderingResponse.Builder builder = RenderingResponse.from(originalResponse);
    RenderingResponse response = builder.build();

    assertThat(response.name()).isEqualTo("originalView");
    assertThat(response.model()).containsEntry("originalKey", "originalValue");
    assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.headers().getFirst("X-Custom")).isEqualTo("original");
  }

  @Test
  public void modelBuilderAttributeWithGeneratedName() {
    RenderingResponse.Builder builder = RenderingResponse.create("viewName");
    builder.modelAttribute("testValue");

    RenderingResponse response = builder.build();
    assertThat(response.model()).containsKey("string");
    assertThat(response.model().get("string")).isEqualTo("testValue");
  }

  @Test
  public void modelBuilderAttributeWithNameAndValue() {
    RenderingResponse.Builder builder = RenderingResponse.create("viewName");
    builder.modelAttribute("customKey", "customValue");

    RenderingResponse response = builder.build();
    assertThat(response.model()).containsEntry("customKey", "customValue");
  }

  @Test
  public void modelBuilderAttributesFromArray() {
    RenderingResponse.Builder builder = RenderingResponse.create("viewName");
    builder.modelAttributes("value1", 123, true);

    RenderingResponse response = builder.build();
    assertThat(response.model()).containsKey("string");
    assertThat(response.model()).containsKey("integer");
    assertThat(response.model()).containsKey("boolean");
    assertThat(response.model().get("string")).isEqualTo("value1");
    assertThat(response.model().get("integer")).isEqualTo(123);
    assertThat(response.model().get("boolean")).isEqualTo(true);
  }

  @Test
  public void modelBuilderAttributesFromMap() {
    RenderingResponse.Builder builder = RenderingResponse.create("viewName");
    builder.modelAttributes(java.util.Map.of("key1", "value1", "key2", "value2"));

    RenderingResponse response = builder.build();
    assertThat(response.model()).containsEntry("key1", "value1");
    assertThat(response.model()).containsEntry("key2", "value2");
  }

  @Test
  public void builderChaining() {
    RenderingResponse.Builder builder = RenderingResponse.create("chainedView");
    RenderingResponse response = builder
            .modelAttribute("chainedKey", "chainedValue")
            .status(HttpStatus.ACCEPTED)
            .header("X-Chained", "true")
            .cookie("sessionId", "chain123")
            .build();

    assertThat(response.name()).isEqualTo("chainedView");
    assertThat(response.model()).containsEntry("chainedKey", "chainedValue");
    assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.headers().getFirst("X-Chained")).isEqualTo("true");
    assertThat(response.cookies().getFirst("sessionId").getValue()).isEqualTo("chain123");
  }

}