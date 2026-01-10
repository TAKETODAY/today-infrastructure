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

package infra.web.view;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.web.RequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:44
 */
class ViewTests {

  @Test
  void shouldHaveCorrectResponseStatusAttributeName() {
    assertThat(View.RESPONSE_STATUS_ATTRIBUTE).isEqualTo("infra.web.view.View.responseStatus");
  }

  @Test
  void shouldHaveCorrectSelectedContentTypeAttributeName() {
    assertThat(View.SELECTED_CONTENT_TYPE).isEqualTo("infra.web.view.View.selectedContentType");
  }

  @Test
  void shouldReturnNullContentTypeByDefault() {
    // given
    View view = new TestView();

    String contentType = view.getContentType();
    assertThat(contentType).isNull();
  }

  static class TestView implements View {

    @Override
    public void render(Map<String, ?> model, RequestContext context) throws Exception {
      // no-op implementation for testing
    }
  }

}