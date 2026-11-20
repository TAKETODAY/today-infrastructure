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

package infra.test.web.mock.result;

import org.junit.jupiter.api.Test;

import infra.test.web.mock.MvcResult;
import infra.validation.BeanPropertyBindingResult;
import infra.validation.BindingResult;
import infra.validation.FieldError;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ModelResultMatchersAdditionalTests {

  private final ModelResultMatchers matchers = MockMvcResultMatchers.model();

  @Test
  void modelAssertionsDistinguishOrdinaryAttributesAndBindingResults() throws Exception {
    ModelAndView model = new ModelAndView("view");
    model.addObject("item", "value");
    var errors = new BeanPropertyBindingResult(new Object(), "item");
    model.addObject(BindingResult.MODEL_KEY_PREFIX + "item", errors);
    MvcResult result = result(model);
    matchers.attribute("item", "value").match(result);
    matchers.attributeExists("item").match(result);
    matchers.attributeDoesNotExist("missing").match(result);
    matchers.size(1).match(result);
    matchers.hasNoErrors().match(result);
    matchers.attributeHasNoErrors("item").match(result);
    matchers.attributeErrorCount("item", 0).match(result);
    matchers.errorCount(0).match(result);
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.hasErrors().match(result));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.attributeHasErrors("item").match(result));

    errors.addError(new FieldError("item", "name", "required"));
    matchers.hasErrors().match(result);
    matchers.attributeHasErrors("item").match(result);
    matchers.attributeHasFieldErrors("item", "name").match(result);
    matchers.attributeHasFieldErrorCode("item", "name", (String) null).match(result);
    matchers.attributeErrorCount("item", 1).match(result);
    matchers.errorCount(1).match(result);
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.hasNoErrors().match(result));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.attributeHasNoErrors("item").match(result));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.attributeHasFieldErrors("item", "other").match(result));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.attributeHasFieldErrorCode("item", "other", "required").match(result));
  }

  @Test
  void missingModelAndBindingResultHaveUsefulDiagnostics() {
    MvcResult empty = mock(MvcResult.class);
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.size(0).match(empty))
            .withMessageContaining("No ModelAndView");
    MvcResult result = result(new ModelAndView("view"));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.attributeHasErrors("missing").match(result))
            .withMessageContaining("No BindingResult");
  }

  private MvcResult result(ModelAndView model) {
    MvcResult result = mock(MvcResult.class);
    given(result.getModelAndView()).willReturn(model);
    return result;
  }

}
