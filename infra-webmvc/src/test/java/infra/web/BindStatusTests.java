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

package infra.web;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import infra.context.support.StaticMessageSource;
import infra.validation.BeanPropertyBindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class BindStatusTests {

  @Test
  void resolvesMessagesBeforeApplyingDisplayEscaping() {
    HttpContext context = mock(HttpContext.class);
    BindingContext binding = mock(BindingContext.class);
    var errors = new BeanPropertyBindingResult(new Object(), "bean");
    errors.reject("invalid", new Object[] { "<value>" }, "fallback");
    var messages = new StaticMessageSource();
    messages.addMessage("invalid", Locale.ENGLISH, "Invalid & {0}");
    given(context.getBinding()).willReturn(binding);
    given(context.getMessageSource()).willReturn(messages);
    given(context.getLocale()).willReturn(Locale.ENGLISH);
    given(binding.getErrors("bean", false)).willReturn(errors);
    given(binding.getErrors("bean", true)).willReturn(errors);

    assertThat(new BindStatus(context, "bean", false).getErrorMessages())
            .containsExactly("Invalid & <value>");
    BindStatus escaped = new BindStatus(context, "bean", true);
    assertThat(escaped.getErrorMessages()).containsExactly("Invalid &amp; &lt;value&gt;");
    assertThat(escaped.getErrorMessage()).isEqualTo("Invalid &amp; &lt;value&gt;");
  }
}
