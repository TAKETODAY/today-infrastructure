/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.Errors;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.resolver.ErrorsMethodArgumentResolver;
import cn.taketoday.web.mock.ServletRequestContext;

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

  private ServletRequestContext webRequest;

  @BeforeEach
  public void setup() throws Exception {
    paramErrors = new ResolvableMethodParameter(new MethodParameter(getClass().getDeclaredMethod("handle", Errors.class), 0));
    bindingResult = new WebDataBinder(new Object(), "attr").getBindingResult();
    webRequest = new ServletRequestContext(null, new MockHttpServletRequest(), null);
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
