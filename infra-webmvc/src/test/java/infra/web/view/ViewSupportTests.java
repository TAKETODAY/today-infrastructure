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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import infra.beans.factory.InitializingBean;
import infra.validation.BeanPropertyBindingResult;
import infra.validation.BindingResult;
import infra.web.HttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.withSettings;

class ViewSupportTests {

  @Test
  void compositeInitializesDelegatesAndReturnsNullWhenNoneResolve() throws Exception {
    ViewResolver delegate = mock(ViewResolver.class, withSettings().extraInterfaces(InitializingBean.class));
    ViewResolverComposite composite = new ViewResolverComposite();
    composite.setViewResolvers(List.of(delegate));
    composite.afterPropertiesSet();
    then((InitializingBean) delegate).should().afterPropertiesSet();
    assertThat(composite.resolveViewName("missing", Locale.US)).isNull();
    then(delegate).should().resolveViewName("missing", Locale.US);
  }

  @Test
  void replacingModelAttributeInvalidatesOnlyItsBindingResult() {
    BindingAwareModelMap model = new BindingAwareModelMap();
    Object target = new Object();
    Object other = new Object();
    BindingResult result = new BeanPropertyBindingResult(target, "item");
    BindingResult otherResult = new BeanPropertyBindingResult(other, "other");
    model.put("item", target);
    model.put(BindingResult.MODEL_KEY_PREFIX + "item", result);
    model.put("other", other);
    model.put(BindingResult.MODEL_KEY_PREFIX + "other", otherResult);
    model.put("item", target);
    assertThat(model).containsEntry(BindingResult.MODEL_KEY_PREFIX + "item", result);
    Object replacement = new Object();
    model.putAll(Map.of("item", replacement));
    assertThat(model).containsEntry("item", replacement)
            .doesNotContainKey(BindingResult.MODEL_KEY_PREFIX + "item")
            .containsEntry(BindingResult.MODEL_KEY_PREFIX + "other", otherResult);
  }

  @Test
  void internalResourceViewExposesModelBeforeForwardingWithoutAnApplicationContext() throws Exception {
    InternalResourceView view = new InternalResourceView("/views/sample.html");
    view.setPreventDispatchLoop(true);
    assertThat(view.getApplicationContext()).isNull();
    HttpContext request = mock(HttpContext.class);
    given(request.getRequestURI()).willReturn("/sample");

    view.render(Map.of("title", "Sample"), request);

    var order = inOrder(request);
    order.verify(request).setAttribute("title", "Sample");
    order.verify(request).forward("/views/sample.html");
  }

  @Test
  void internalResourceViewRejectsCircularForward() throws Exception {
    InternalResourceView view = new InternalResourceView("sample");
    view.setPreventDispatchLoop(true);
    HttpContext request = mock(HttpContext.class);
    given(request.getRequestURI()).willReturn("/sample");

    assertThatIllegalStateException().isThrownBy(() -> view.render(Map.of(), request))
            .withMessageContaining("Circular view path [/sample]");
    then(request).should(never()).forward("sample");
  }

  @Test
  void internalResourceResolverSupportsCustomViewType() throws Exception {
    InternalResourceViewResolver resolver = new InternalResourceViewResolver("/views/", ".html");
    assertThat(resolver.getPrefix()).isEqualTo("/views/");
    assertThat(resolver.getSuffix()).isEqualTo(".html");
    assertThat(resolver.instantiateView()).isExactlyInstanceOf(InternalResourceView.class);
    resolver.setViewClass(CustomInternalView.class);
    assertThat(resolver.buildView("sample")).isExactlyInstanceOf(CustomInternalView.class);
  }

  public static class CustomInternalView extends InternalResourceView {
  }

}
