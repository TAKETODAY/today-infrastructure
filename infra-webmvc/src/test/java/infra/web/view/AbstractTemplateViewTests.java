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

import infra.web.HttpContext;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:51
 */
class AbstractTemplateViewTests {

  private final TestAbstractTemplateView view = new TestAbstractTemplateView();

  @Test
  void exposesRequestAndSessionAttributesWithConfiguredOverrides() throws Exception {
    MockHttpContext context = new MockHttpContext();
    context.setAttribute("requestOnly", "request");
    context.setAttribute("title", "request title");
    context.setAttribute("theme", "request theme");
    MockSession session = new MockSession();
    session.setAttribute("theme", "session theme");
    session.setAttribute("sessionOnly", "session");
    context.request.setSession(session);
    view.setExposeRequestAttributes(true);
    view.setAllowRequestOverride(true);
    view.setExposeSessionAttributes(true);
    view.setAllowSessionOverride(true);
    Map<String, Object> model = Map.of("title", "model title", "theme", "model theme", "body", "content");

    view.render(model, context);

    assertThat(view.renderedModel).containsExactlyInAnyOrderEntriesOf(Map.of(
            "requestOnly", "request", "title", "request title", "theme", "session theme",
            "sessionOnly", "session", "body", "content"));
  }

  @Test
  void rejectsRequestAttributeCollisionByDefault() {
    MockHttpContext context = new MockHttpContext();
    context.setAttribute("title", "request title");
    view.setExposeRequestAttributes(true);

    assertThatExceptionOfType(ViewRenderingException.class)
            .isThrownBy(() -> view.render(Map.of("title", "model title"), context))
            .withMessageContaining("Cannot expose request attribute 'title'");
  }

  @Test
  void rejectsSessionAttributeCollisionByDefault() {
    MockHttpContext context = new MockHttpContext();
    MockSession session = new MockSession();
    session.setAttribute("theme", "session theme");
    context.request.setSession(session);
    view.setExposeSessionAttributes(true);

    assertThatExceptionOfType(ViewRenderingException.class)
            .isThrownBy(() -> view.render(Map.of("theme", "model theme"), context))
            .withMessageContaining("Cannot expose session attribute 'theme'");
  }

  @Test
  void sessionExposureDoesNotCreateASession() throws Exception {
    MockHttpContext context = new MockHttpContext();
    view.setExposeSessionAttributes(true);

    view.render(Map.of("title", "model title"), context);

    assertThat(view.renderedModel).containsExactlyEntriesOf(Map.of("title", "model title"));
    assertThat(context.getSession(false)).isNull();
  }

  @Test
  void appliesConfiguredContentType() throws Exception {
    MockHttpContext context = new MockHttpContext();
    view.setContentType("text/html");

    view.render(null, context);

    assertThat(context.getResponseContentType()).isEqualTo("text/html");
  }

  @Test
  void preservesExistingContentType() throws Exception {
    MockHttpContext context = new MockHttpContext();
    context.setContentType("application/json");
    view.setContentType("text/html");

    view.render(null, context);

    assertThat(context.getResponseContentType()).isEqualTo("application/json");
  }

  static class TestAbstractTemplateView extends AbstractTemplateView {

    Map<String, Object> renderedModel = Map.of();

    TestAbstractTemplateView() {
      setExposeMessages(false);
      setExposeBindings(false);
    }

    @Override
    protected void renderMergedTemplateModel(Map<String, Object> model, HttpContext context) {
      this.renderedModel = model;
    }
  }

}
