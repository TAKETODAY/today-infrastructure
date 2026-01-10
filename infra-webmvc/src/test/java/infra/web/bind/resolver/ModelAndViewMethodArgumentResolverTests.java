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