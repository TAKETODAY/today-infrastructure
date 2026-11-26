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

import java.util.Locale;
import java.util.Map;

import infra.context.support.MessageSourceAccessor;
import infra.context.support.StaticMessageSource;
import infra.web.ServerHttpExchange;
import infra.web.mock.MockServerHttpExchange;
import infra.web.view.freemarker.FreeMarkerView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AbstractViewTests {

  @Test
  void bindingExposureDefaultsAndResolverOverrides() throws Exception {
    var ordinary = new CapturingView();
    assertThat(ordinary.isExposeBindings()).isFalse();
    ordinary.render(Map.of("bindings", "business"), mock(ServerHttpExchange.class));
    assertThat(ordinary.model).containsEntry("bindings", "business");
    var resolver = new UrlBasedViewResolver();
    resolver.setViewClass(CapturingView.class);
    assertThat(resolver.buildView("home").isExposeBindings()).isFalse();
    resolver.setViewClass(FreeMarkerView.class);
    assertThat(resolver.buildView("home").isExposeBindings()).isTrue();
    resolver.setExposeBindings(false);
    assertThat(resolver.buildView("home").isExposeBindings()).isFalse();
    resolver.setViewClass(CapturingView.class);
    resolver.setExposeBindings(true);
    assertThat(resolver.buildView("home").isExposeBindings()).isTrue();
  }

  @Test
  void bindingExposureRejectsRequestAndMergedTemplateConflicts() {
    var context = new MockServerHttpExchange();
    context.setAttribute("bindings", "request");
    var view = new FreeMarkerView();
    view.setExposeRequestAttributes(true);
    view.setAllowRequestOverride(true);
    assertThatExceptionOfType(ViewRenderingException.class)
            .isThrownBy(() -> view.render(Map.of(), context))
            .withMessageContaining("Cannot expose bindings");
  }

  @Test
  void disabledBindingsDoNotCreateAccessor() throws Exception {
    var view = new CapturingView() {
      @Override
      protected BindingAccessor createBindingAccessor(ServerHttpExchange context, Map<String, Object> model, Locale locale) {
        throw new AssertionError("Binding accessor must not be created");
      }
    };
    view.render(Map.of(), mock(ServerHttpExchange.class));
  }

  @Test
  void ordinaryViewsLeaveMessagesNameAvailableByDefault() throws Exception {
    var view = new CapturingView();
    assertThat(view.isExposeMessages()).isFalse();
    view.render(Map.of("messages", "business value"), mock(ServerHttpExchange.class));
    assertThat(view.model).containsEntry("messages", "business value");
    assertThat(new FreeMarkerView().isExposeMessages()).isTrue();
  }

  @Test
  void ordinaryViewsCanOptIntoUnescapedMessages() throws Exception {
    var context = mock(ServerHttpExchange.class);
    var source = new StaticMessageSource();
    source.addMessage("hello", Locale.GERMANY, "Hallo & <world>");
    given(context.getMessageSource()).willReturn(source);
    given(context.getPreferredLocale()).willReturn(Locale.GERMANY);
    var view = new CapturingView();
    view.setExposeMessages(true);
    view.render(Map.of(), context);
    var messages = (MessageSourceAccessor) view.model.get(AbstractView.MESSAGES_ATTRIBUTE);
    assertThat(messages.getMessage("hello")).isEqualTo("Hallo & <world>");
  }

  @Test
  void enabledExposureRejectsModelAndRequestConflicts() {
    var view = new CapturingView();
    view.setExposeMessages(true);
    var context = mock(ServerHttpExchange.class);
    assertThatExceptionOfType(ViewRenderingException.class)
            .isThrownBy(() -> view.render(Map.of("messages", "business"), context));
    given(context.getAttribute("messages")).willReturn("request value");
    assertThatExceptionOfType(ViewRenderingException.class)
            .isThrownBy(() -> view.render(Map.of(), context));
  }

  @Test
  void resolverPreservesViewDefaultsUnlessExplicitlyConfigured() throws Exception {
    var resolver = new UrlBasedViewResolver();
    resolver.setViewClass(FreeMarkerView.class);
    assertThat(resolver.buildView("home").isExposeMessages()).isTrue();
    resolver.setExposeMessages(false);
    assertThat(resolver.buildView("home").isExposeMessages()).isFalse();
    resolver.setViewClass(CapturingView.class);
    resolver.setExposeMessages(true);
    assertThat(resolver.buildView("home").isExposeMessages()).isTrue();
  }

  @Test
  void templateAttributeMergingCannotOverrideAutomaticMessages() {
    var context = new MockServerHttpExchange();
    context.setAttribute("messages", "request value");
    var view = new FreeMarkerView();
    view.setExposeRequestAttributes(true);
    view.setAllowRequestOverride(true);
    assertThatExceptionOfType(ViewRenderingException.class)
            .isThrownBy(() -> view.render(Map.of(), context))
            .withMessageContaining("Cannot expose messages");
  }

  public static class CapturingView extends AbstractUrlBasedView {

    Map<String, Object> model = Map.of();

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, ServerHttpExchange context) {
      this.model = model;
    }
  }
}
