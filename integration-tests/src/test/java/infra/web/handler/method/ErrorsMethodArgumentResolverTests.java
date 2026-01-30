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

package infra.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.core.MethodParameter;
import infra.mock.web.HttpMockRequestImpl;
import infra.validation.BindingResult;
import infra.validation.Errors;
import infra.web.BindingContext;
import infra.web.bind.RequestContextDataBinder;
import infra.web.bind.resolver.ErrorsMethodArgumentResolver;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture with {@link ErrorsMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class ErrorsMethodArgumentResolverTests {

  private final ErrorsMethodArgumentResolver resolver = new ErrorsMethodArgumentResolver();

  private BindingResult bindingResult;

  private ResolvableMethodParameter paramErrors;

  private MockRequestContext webRequest;

  @BeforeEach
  public void setup() throws Exception {
    paramErrors = new ResolvableMethodParameter(new MethodParameter(getClass().getDeclaredMethod("handle", Errors.class), 0));
    bindingResult = new RequestContextDataBinder(new Object(), "attr").getBindingResult();
    webRequest = new MockRequestContext(null, new HttpMockRequestImpl(), null);
  }

  @Test
  public void supports() {
    resolver.supportsParameter(paramErrors);
  }

  @Test
  public void bindingResult() throws Throwable {
    BindingContext mavContainer = new BindingContext();
    mavContainer.addAttribute("ignore1", "value1");
    mavContainer.addAttribute("ignore2", "value2");
    mavContainer.addAttribute("ignore3", "value3");
    mavContainer.addAttribute("ignore4", "value4");
    mavContainer.addAttribute("ignore5", "value5");
    mavContainer.addAllAttributes(bindingResult.getModel());
    webRequest.setBinding(mavContainer);

    Object actual = resolver.resolveArgument(webRequest, paramErrors);
    assertThat(bindingResult).isSameAs(actual);
  }

  @Test
  public void bindingResultNotFound() throws Exception {
    BindingContext mavContainer = new BindingContext();
    mavContainer.addAllAttributes(bindingResult.getModel());
    mavContainer.addAttribute("ignore1", "value1");
    webRequest.setBinding(mavContainer);

    assertThatIllegalStateException().isThrownBy(() ->
            resolver.resolveArgument(webRequest, paramErrors));
  }

  @Test
  public void noBindingResult() throws Exception {
    BindingContext mavContainer = new BindingContext();
    webRequest.setBinding(mavContainer);

    assertThatIllegalStateException().isThrownBy(() ->
            resolver.resolveArgument(webRequest, paramErrors));
  }

  @SuppressWarnings("unused")
  private void handle(Errors errors) {
  }

}
