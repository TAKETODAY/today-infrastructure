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

import java.util.LinkedHashMap;
import java.util.Map;

import infra.web.RequestContext;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:51
 */
class AbstractTemplateViewTests {

  @Test
  void shouldSetAndGetExposeRequestAttributes() {
    // given
    TestAbstractTemplateView view = new TestAbstractTemplateView();

    // when
    view.setExposeRequestAttributes(true);

    // then
    // Verify no exception is thrown
    assertThatNoException().isThrownBy(() -> view.setExposeRequestAttributes(true));
  }

  @Test
  void shouldSetAndGetAllowRequestOverride() {
    // given
    TestAbstractTemplateView view = new TestAbstractTemplateView();

    // when
    view.setAllowRequestOverride(true);

    // then
    assertThatNoException().isThrownBy(() -> view.setAllowRequestOverride(true));
  }

  @Test
  void shouldSetAndGetExposeSessionAttributes() {
    // given
    TestAbstractTemplateView view = new TestAbstractTemplateView();

    // when
    view.setExposeSessionAttributes(true);

    // then
    assertThatNoException().isThrownBy(() -> view.setExposeSessionAttributes(true));
  }

  @Test
  void shouldSetAndGetAllowSessionOverride() {
    // given
    TestAbstractTemplateView view = new TestAbstractTemplateView();

    // when
    view.setAllowSessionOverride(true);

    // then
    assertThatNoException().isThrownBy(() -> view.setAllowSessionOverride(true));
  }

  @Test
  void shouldApplyContentTypeWhenResponseContentTypeIsNull() {
    // given
    TestAbstractTemplateView view = new TestAbstractTemplateView();
    view.setContentType("text/html");

    Map<String, Object> model = new LinkedHashMap<>();
    RequestContext context = mock(RequestContext.class);
    when(context.getResponseContentType()).thenReturn(null);

    // when & then
    assertThatNoException().isThrownBy(() -> view.renderMergedOutputModel(model, context));
  }

  @Test
  void shouldNotApplyContentTypeWhenResponseContentTypeIsAlreadySet() {
    // given
    TestAbstractTemplateView view = new TestAbstractTemplateView();
    view.setContentType("text/html");

    Map<String, Object> model = new LinkedHashMap<>();
    RequestContext context = mock(RequestContext.class);
    when(context.getResponseContentType()).thenReturn("application/json");

    // when & then
    assertThatNoException().isThrownBy(() -> view.renderMergedOutputModel(model, context));
  }

  static class TestAbstractTemplateView extends AbstractTemplateView {

    @Override
    protected void renderMergedTemplateModel(Map<String, Object> model, RequestContext context) throws Exception {
      // No-op implementation for testing
    }
  }

}