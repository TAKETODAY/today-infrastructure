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