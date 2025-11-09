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