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