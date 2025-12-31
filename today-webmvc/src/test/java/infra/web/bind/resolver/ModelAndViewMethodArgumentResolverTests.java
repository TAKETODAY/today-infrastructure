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

package infra.web.bind.resolver;

import org.junit.jupiter.api.Test;

import infra.web.BindingContext;
import infra.web.RequestContext;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 21:23
 */
class ModelAndViewMethodArgumentResolverTests {

  @Test
  void resolveArgumentReturnsModelAndViewFromContext() throws Throwable {
    RequestContext context = mock(RequestContext.class);
    BindingContext binding = mock(BindingContext.class);
    ModelAndView modelAndView = new ModelAndView();

    when(context.binding()).thenReturn(binding);
    when(binding.getModelAndView()).thenReturn(modelAndView);

    ResolvableMethodParameter resolvable = mock(ResolvableMethodParameter.class);
    ModelAndViewMethodArgumentResolver resolver = new ModelAndViewMethodArgumentResolver();

    Object result = resolver.resolveArgument(context, resolvable);
    assertThat(result).isSameAs(modelAndView);
  }

}