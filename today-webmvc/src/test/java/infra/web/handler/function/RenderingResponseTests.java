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