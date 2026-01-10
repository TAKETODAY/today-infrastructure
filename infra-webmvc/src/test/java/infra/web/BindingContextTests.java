/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.core.Conventions;
import infra.core.ResolvableType;
import infra.ui.ModelMap;
import infra.web.bind.WebDataBinder;
import infra.web.bind.support.WebBindingInitializer;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 16:32
 */
class BindingContextTests {
  @Test
  void constructor_ShouldCreateEmptyBindingContext() {
    BindingContext context = new BindingContext();

    assertThat(context.hasModel()).isFalse();
    assertThat(context.hasModelAndView()).isFalse();
    assertThat((Object) context.getRedirectModel()).isNull();
  }

  @Test
  void constructorWithInitializer_ShouldSetInitializer() {
    WebBindingInitializer initializer = mock(WebBindingInitializer.class);
    BindingContext context = new BindingContext(initializer);

    assertThat(context).isNotNull();
  }

  @Test
  void getModel_ShouldCreateNewModelWhenNotExists() {
    BindingContext context = new BindingContext();

    ModelMap model = context.getModel();

    assertThat(model).isNotNull();
    assertThat(context.hasModel()).isTrue();
  }

  @Test
  void getModel_ShouldReturnSameInstance() {
    BindingContext context = new BindingContext();

    ModelMap model1 = context.getModel();
    ModelMap model2 = context.getModel();

    assertThat(model1).isSameAs(model2);
  }

  @Test
  void addAttributeWithNameAndValue_ShouldAddToModel() {
    BindingContext context = new BindingContext();

    context.addAttribute("name", "value");

    assertThat(context.containsAttribute("name")).isTrue();
    assertThat(context.getModel().getAttribute("name")).isEqualTo("value");
  }

  @Test
  void addAllAttributes_ShouldAddAllToModel() {
    BindingContext context = new BindingContext();
    Map<String, Object> attributes = Map.of("key1", "value1", "key2", "value2");

    context.addAllAttributes(attributes);

    assertThat(context.containsAttribute("key1")).isTrue();
    assertThat(context.containsAttribute("key2")).isTrue();
    assertThat(context.getModel().getAttribute("key1")).isEqualTo("value1");
    assertThat(context.getModel().getAttribute("key2")).isEqualTo("value2");
  }

  @Test
  void addAllAttributesWithNull_ShouldNotThrowException() {
    BindingContext context = new BindingContext();

    assertThatNoException().isThrownBy(() -> context.addAllAttributes(null));
  }

  @Test
  void mergeAttributes_ShouldMergeAttributes() {
    BindingContext context = new BindingContext();
    context.addAttribute("existing", "original");

    Map<String, Object> newAttributes = Map.of("existing", "new", "newKey", "newValue");

    context.mergeAttributes(newAttributes);

    assertThat(context.getModel().getAttribute("existing")).isEqualTo("original");
    assertThat(context.getModel().getAttribute("newKey")).isEqualTo("newValue");
  }

  @Test
  void mergeAttributesWithNull_ShouldNotThrowException() {
    BindingContext context = new BindingContext();

    assertThatNoException().isThrownBy(() -> context.mergeAttributes(null));
  }

  @Test
  void removeAttributes_ShouldRemoveSpecifiedAttributes() {
    BindingContext context = new BindingContext();
    context.addAttribute("key1", "value1");
    context.addAttribute("key2", "value2");

    Map<String, Object> toRemove = Map.of("key1", "value1");

    context.removeAttributes(toRemove);

    assertThat(context.containsAttribute("key1")).isFalse();
    assertThat(context.containsAttribute("key2")).isTrue();
  }

  @Test
  void removeAttributesWithNull_ShouldNotThrowException() {
    BindingContext context = new BindingContext();

    assertThatNoException().isThrownBy(() -> context.removeAttributes(null));
  }

  @Test
  void containsAttribute_ShouldReturnTrueWhenAttributeExists() {
    BindingContext context = new BindingContext();
    context.addAttribute("key", "value");

    boolean result = context.containsAttribute("key");

    assertThat(result).isTrue();
  }

  @Test
  void containsAttribute_ShouldReturnFalseWhenAttributeNotExists() {
    BindingContext context = new BindingContext();

    boolean result = context.containsAttribute("nonexistent");

    assertThat(result).isFalse();
  }

  @Test
  void containsAttribute_ShouldReturnFalseWhenNoModel() {
    BindingContext context = new BindingContext();

    boolean result = context.containsAttribute("key");

    assertThat(result).isFalse();
  }

  @Test
  void getModelAndView_ShouldCreateNewWhenNotExists() {
    BindingContext context = new BindingContext();

    ModelAndView mav = context.getModelAndView();

    assertThat(mav).isNotNull();
    assertThat(context.hasModelAndView()).isTrue();
  }

  @Test
  void getModelAndView_ShouldReturnSameInstance() {
    BindingContext context = new BindingContext();

    ModelAndView mav1 = context.getModelAndView();
    ModelAndView mav2 = context.getModelAndView();

    assertThat(mav1).isSameAs(mav2);
  }

  @Test
  void setAndGetRedirectModel_ShouldWorkCorrectly() {
    BindingContext context = new BindingContext();
    RedirectModel redirectModel = new RedirectModel();

    context.setRedirectModel(redirectModel);

    assertThat((Object) context.getRedirectModel()).isSameAs(redirectModel);
  }

  @Test
  void setRedirectModelToNull_ShouldWork() {
    BindingContext context = new BindingContext();
    context.setRedirectModel(new RedirectModel());

    context.setRedirectModel(null);

    assertThat((Object) context.getRedirectModel()).isNull();
  }

  @Test
  void setBindingDisabled_ShouldMarkAttributeAsDisabled() {
    BindingContext context = new BindingContext();

    context.setBindingDisabled("disabledAttribute");

    assertThat(context.isBindingDisabled("disabledAttribute")).isTrue();
    assertThat(context.isBindingDisabled("normalAttribute")).isFalse();
  }

  @Test
  void setBinding_ShouldControlBindingEnabledState() {
    BindingContext context = new BindingContext();

    context.setBinding("disabledAttribute", false);
    context.setBinding("enabledAttribute", true);

    assertThat(context.isBindingDisabled("disabledAttribute")).isTrue();
    assertThat(context.isBindingDisabled("enabledAttribute")).isFalse();
  }

  @Test
  void isBindingDisabled_ShouldReturnFalseWhenNoSets() {
    BindingContext context = new BindingContext();

    boolean result = context.isBindingDisabled("anyAttribute");

    assertThat(result).isFalse();
  }

  @Test
  void isBindingDisabled_ShouldHandleBothSets() {
    BindingContext context = new BindingContext();

    context.setBindingDisabled("hardDisabled");
    context.setBinding("softDisabled", false);
    context.setBinding("enabled", true);

    assertThat(context.isBindingDisabled("hardDisabled")).isTrue();
    assertThat(context.isBindingDisabled("softDisabled")).isTrue();
    assertThat(context.isBindingDisabled("enabled")).isFalse();
  }

  @Test
  void bindingDisabledShouldOverrideBinding() {
    BindingContext context = new BindingContext();

    context.setBinding("testAttribute", false);
    context.setBindingDisabled("testAttribute");

    assertThat(context.isBindingDisabled("testAttribute")).isTrue();
  }

  @Test
  void toString_ShouldProvideDiagnosticInfo() {
    BindingContext context = new BindingContext();

    String result = context.toString();

    assertThat(result).contains("BindingContext: model:");
  }

  @Test
  void toString_ShouldIncludeRedirectModelWhenPresent() {
    BindingContext context = new BindingContext();
    RedirectModel redirectModel = new RedirectModel();
    context.setRedirectModel(redirectModel);

    String result = context.toString();

    assertThat(result).contains("redirect model");
  }

  @Test
  void addAttributeWithValueOnly_ShouldAddToModel() {
    BindingContext context = new BindingContext();
    Object value = new Object();

    BindingContext result = context.addAttribute(value);

    assertThat(result).isSameAs(context);
    assertThat(context.containsAttribute(Conventions.getVariableName(value))).isTrue();
  }

  @Test
  void addAttributeWithNameAndValue_ShouldReturnSelf() {
    BindingContext context = new BindingContext();

    BindingContext result = context.addAttribute("name", "value");

    assertThat(result).isSameAs(context);
  }

  @Test
  void createBinderWithoutTarget_ShouldCreateBinder() throws Throwable {
    BindingContext bindingContext = new BindingContext();
    RequestContext request = mock(RequestContext.class);

    WebDataBinder binder = bindingContext.createBinder(request, "objectName");

    assertThat(binder).isNotNull();
    assertThat(binder.getObjectName()).isEqualTo("objectName");
    assertThat(binder.getTarget()).isNull();
  }

  @Test
  void createBinderWithTarget_ShouldCreateBinder() throws Throwable {
    BindingContext bindingContext = new BindingContext();
    RequestContext request = mock(RequestContext.class);
    Object target = new Object();

    WebDataBinder binder = bindingContext.createBinder(request, target, "objectName");

    assertThat(binder).isNotNull();
    assertThat(binder.getObjectName()).isEqualTo("objectName");
    assertThat(binder.getTarget()).isSameAs(target);
  }

  @Test
  void createBinderWithTargetAndType_ShouldCreateBinder() throws Throwable {
    BindingContext bindingContext = new BindingContext();
    RequestContext request = mock(RequestContext.class);
    Object target = new Object();
    ResolvableType targetType = ResolvableType.forClass(String.class);

    WebDataBinder binder = bindingContext.createBinder(request, target, "objectName", targetType);

    assertThat(binder).isNotNull();
    assertThat(binder.getObjectName()).isEqualTo("objectName");
    assertThat(binder.getTarget()).isSameAs(target);
  }

  @Test
  void createBinderWithoutTargetButWithType_ShouldCreateBinder() throws Throwable {
    BindingContext bindingContext = new BindingContext();
    RequestContext request = mock(RequestContext.class);
    ResolvableType targetType = ResolvableType.forClass(String.class);

    WebDataBinder binder = bindingContext.createBinder(request, null, "objectName", targetType);

    assertThat(binder).isNotNull();
    assertThat(binder.getObjectName()).isEqualTo("objectName");
    assertThat(binder.getTarget()).isNull();
    assertThat(binder.getTargetType()).isEqualTo(targetType);
  }

  @Test
  void createBinderInstance_ShouldCreateWebDataBinder() throws Exception {
    BindingContext bindingContext = new BindingContext();
    RequestContext request = mock(RequestContext.class);
    Object target = new Object();

    WebDataBinder binder = bindingContext.createBinderInstance(target, "objectName", request);

    assertThat(binder).isNotNull();
    assertThat(binder).isInstanceOf(WebDataBinder.class);
    assertThat(binder.getObjectName()).isEqualTo("objectName");
    assertThat(binder.getTarget()).isSameAs(target);
  }

  @Test
  void initBinder_ShouldNotThrowException() throws Throwable {
    BindingContext bindingContext = new BindingContext();
    WebDataBinder dataBinder = mock(WebDataBinder.class);
    RequestContext request = mock(RequestContext.class);

    assertThatNoException().isThrownBy(() -> bindingContext.initBinder(dataBinder, request));
  }

  @Test
  void updateModel_ShouldNotThrowException() throws Throwable {
    BindingContext bindingContext = new BindingContext();
    RequestContext request = mock(RequestContext.class);

    assertThatNoException().isThrownBy(() -> bindingContext.updateModel(request));
  }

  @Test
  void initModel_ShouldNotThrowException() throws Throwable {
    BindingContext bindingContext = new BindingContext();
    RequestContext request = mock(RequestContext.class);

    assertThatNoException().isThrownBy(() -> bindingContext.initModel(request));
  }

  @Test
  void hasModelAndView_ShouldReturnFalseWhenNoModelAndView() {
    BindingContext context = new BindingContext();

    boolean result = context.hasModelAndView();

    assertThat(result).isFalse();
  }

  @Test
  void hasModel_ShouldReturnFalseWhenNoModel() {
    BindingContext context = new BindingContext();

    boolean result = context.hasModel();

    assertThat(result).isFalse();
  }

  @Test
  void setBindingWithEnabledTrue_ShouldRemoveFromNoBindingSet() {
    BindingContext context = new BindingContext();
    context.setBinding("attribute", false);

    context.setBinding("attribute", true);

    assertThat(context.isBindingDisabled("attribute")).isFalse();
  }

}