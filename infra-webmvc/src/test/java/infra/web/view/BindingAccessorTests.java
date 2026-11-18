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

import java.beans.PropertyEditorSupport;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import infra.beans.testfixture.beans.TestBean;
import infra.context.support.MessageSourceAccessor;
import infra.context.support.StaticMessageSource;
import infra.validation.BeanPropertyBindingResult;
import infra.validation.BindingResult;
import infra.validation.FieldError;
import infra.web.BindingContext;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class BindingAccessorTests {

  private final MockHttpContext context = new MockHttpContext();

  private final StaticMessageSource source = new StaticMessageSource();

  private BindingAccessor accessor(Map<String, ?> model) {
    return new BindingAccessor(context, model, new MessageSourceAccessor(source, Locale.GERMAN));
  }

  @Test
  void finalModelWinsAndValuesRemainUnescaped() {
    context.setAttribute("bean", new TestBean("request"));
    BindingContext binding = new BindingContext();
    binding.addAttribute("bean", new TestBean("binding"));
    BindingContext.set(context, binding);
    var bindings = accessor(Map.of("bean", new TestBean("<Tom & Sue>")));
    var status = bindings.getBindStatus("bean.name");
    assertThat(status.getValue()).isEqualTo("<Tom & Sue>");
    assertThat(status.getDisplayValue()).isEqualTo("<Tom & Sue>");
    assertThat(status.getActualValue()).isEqualTo("<Tom & Sue>");
    assertThat(status.getValueType()).isEqualTo(String.class);
    assertThat(bindings.getBindStatus("bean.name")).isNotSameAs(status);
  }

  @Test
  void bindingModelPrecedesRequestAndNullShadowsFallback() {
    context.setAttribute("bean", new TestBean("request"));
    assertThat(accessor(Map.of()).getBindStatus("bean.name").getValue()).isEqualTo("request");
    BindingContext binding = new BindingContext();
    binding.addAttribute("bean", new TestBean("binding"));
    BindingContext.set(context, binding);
    assertThat(accessor(Map.of()).getBindStatus("bean.name").getValue()).isEqualTo("binding");
    Map<String, Object> model = new HashMap<>();
    model.put("bean", null);
    assertThatIllegalStateException().isThrownBy(() -> accessor(model).getBindStatus("bean.name"));
    binding.addAttribute("bean", null);
    assertThatIllegalStateException().isThrownBy(() -> accessor(Map.of()).getBindStatus("bean.name"));
  }

  @Test
  void rejectedValueEditorAndLocalizedErrorsComeFromFinalModel() {
    var errors = new BeanPropertyBindingResult(new TestBean("actual"), "bean");
    var editor = new PropertyEditorSupport();
    errors.getPropertyEditorRegistry().registerCustomEditor(String.class, "name", editor);
    errors.addError(new FieldError("bean", "name", "<rejected>", false,
            new String[] { "invalid" }, new Object[] { "<value>" }, "fallback"));
    errors.reject("global", "<global>");
    source.addMessage("invalid", Locale.GERMAN, "Ungültig & {0}");
    var bindings = accessor(Map.of(BindingResult.MODEL_KEY_PREFIX + "bean", errors));
    var status = bindings.getBindStatus("bean.name");
    assertThat(status.getValue()).isEqualTo("<rejected>");
    assertThat(status.getActualValue()).isEqualTo("actual");
    assertThat(status.getEditor()).isSameAs(editor);
    assertThat(status.findEditor(String.class)).isSameAs(editor);
    assertThat(status.getErrors()).isSameAs(errors);
    assertThat(status.getErrorMessages()).containsExactly("Ungültig & <value>");
    assertThat(bindings.getBindStatus("bean").getErrorMessage()).isEqualTo("<global>");
    assertThat(bindings.getBindStatus("bean.*").getErrorCodes()).hasSize(2);
    assertThat(bindings.getBindStatus("bean.na*").getErrorCode()).isEqualTo("invalid");
  }

  @Test
  void explicitNullErrorsDoNotUseBindingErrors() {
    var binding = new BindingContext();
    var errors = new BeanPropertyBindingResult(new TestBean("binding"), "bean");
    binding.addAttribute(BindingResult.MODEL_KEY_PREFIX + "bean", errors);
    BindingContext.set(context, binding);
    Map<String, Object> model = new HashMap<>();
    model.put(BindingResult.MODEL_KEY_PREFIX + "bean", null);
    model.put("bean", new TestBean("model"));
    var status = accessor(model).getBindStatus("bean.name");
    assertThat(status.getErrors()).isNull();
    assertThat(status.getValue()).isEqualTo("model");
  }

  @Test
  void errorsAreNotCachedAcrossModelChanges() {
    var binding = new BindingContext();
    var first = new BeanPropertyBindingResult(new TestBean("first"), "bean");
    var second = new BeanPropertyBindingResult(new TestBean("second"), "bean");
    String key = BindingResult.MODEL_KEY_PREFIX + "bean";
    binding.addAttribute(key, first);
    assertThat(binding.getErrors("bean")).isSameAs(first);
    binding.addAttribute(key, second);
    assertThat(binding.getErrors("bean")).isSameAs(second);
  }
}
