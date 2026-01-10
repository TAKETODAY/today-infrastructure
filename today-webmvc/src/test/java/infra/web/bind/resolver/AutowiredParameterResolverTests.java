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

import java.lang.reflect.Method;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.DependencyInjector;
import infra.beans.factory.support.DependencyInjectorProvider;
import infra.core.MethodParameter;
import infra.web.RequestContext;
import infra.web.handler.method.ResolvableMethodParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 22:22
 */
class AutowiredParameterResolverTests {

  @Test
  public void supportsParameterWithAutowiredAnnotation() throws Exception {
    Method method = getClass().getDeclaredMethod("handleAutowiredParam", String.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    DependencyInjectorProvider provider = mock(DependencyInjectorProvider.class);
    AutowiredParameterResolver resolver = new AutowiredParameterResolver(provider);

    assertThat(resolver.supportsParameter(parameter)).isTrue();
  }

  @Test
  public void supportsParameterWithoutAutowiredAnnotation() throws Exception {
    Method method = getClass().getDeclaredMethod("handleNonAutowiredParam", String.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    DependencyInjectorProvider provider = mock(DependencyInjectorProvider.class);
    AutowiredParameterResolver resolver = new AutowiredParameterResolver(provider);

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  public void resolveArgumentSuccessfully() throws Throwable {
    Method method = getClass().getDeclaredMethod("handleAutowiredParam", String.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    DependencyInjectorProvider provider = mock(DependencyInjectorProvider.class);
    DependencyInjector injector = mock(DependencyInjector.class);

    when(provider.getInjector()).thenReturn(injector);
    when(injector.resolveValue(any(DependencyDescriptor.class))).thenReturn("injectedValue");

    AutowiredParameterResolver resolver = new AutowiredParameterResolver(provider);
    Object result = resolver.resolveArgument(mock(RequestContext.class), parameter);

    assertThat(result).isEqualTo("injectedValue");
    verify(injector).resolveValue(any(DependencyDescriptor.class));
  }

  @Test
  public void resolveArgumentReturnsNull() throws Throwable {
    Method method = getClass().getDeclaredMethod("handleAutowiredParam", String.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    DependencyInjectorProvider provider = mock(DependencyInjectorProvider.class);
    DependencyInjector injector = mock(DependencyInjector.class);

    when(provider.getInjector()).thenReturn(injector);
    when(injector.resolveValue(any(DependencyDescriptor.class))).thenReturn(null);

    AutowiredParameterResolver resolver = new AutowiredParameterResolver(provider);
    Object result = resolver.resolveArgument(mock(RequestContext.class), parameter);

    assertThat(result).isNull();
  }

  @SuppressWarnings("unused")
  private void handleAutowiredParam(@Autowired String param) { }

  @SuppressWarnings("unused")
  private void handleNonAutowiredParam(String param) { }

}