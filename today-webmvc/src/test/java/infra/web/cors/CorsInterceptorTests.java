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

package infra.web.cors;

import org.junit.jupiter.api.Test;

import infra.web.RequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 22:23
 */
class CorsInterceptorTests {

  @Test
  void constructorWithNullConfigurationSourceThrowsException() {
    assertThatThrownBy(() -> new CorsInterceptor(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("CorsConfigurationSource is required");
  }

  @Test
  void constructorWithValidConfigurationSource() {
    CorsConfigurationSource configSource = mock(CorsConfigurationSource.class);

    CorsInterceptor interceptor = new CorsInterceptor(configSource);

    assertThat(interceptor).isNotNull();
  }

  @Test
  void setCorsProcessorWithNullProcessorThrowsException() {
    CorsConfigurationSource configSource = mock(CorsConfigurationSource.class);
    CorsInterceptor interceptor = new CorsInterceptor(configSource);

    assertThatThrownBy(() -> interceptor.setCorsProcessor(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("CorsProcessor is required");
  }

  @Test
  void setCorsProcessorWithValidProcessor() {
    CorsConfigurationSource configSource = mock(CorsConfigurationSource.class);
    CorsInterceptor interceptor = new CorsInterceptor(configSource);
    CorsProcessor processor = mock(CorsProcessor.class);

    interceptor.setCorsProcessor(processor);

    // Verification by checking that no exception is thrown
    assertThatCode(() -> interceptor.setCorsProcessor(processor)).doesNotThrowAnyException();
  }

  @Test
  void beforeProcessReturnsTrueWhenNotPreFlightRequest() throws Throwable {
    CorsConfigurationSource configSource = mock(CorsConfigurationSource.class);
    CorsProcessor processor = mock(CorsProcessor.class);
    RequestContext request = mock(RequestContext.class);
    Object handler = new Object();
    CorsConfiguration corsConfig = mock(CorsConfiguration.class);

    given(configSource.getCorsConfiguration(request)).willReturn(corsConfig);
    given(processor.process(corsConfig, request)).willReturn(true);
    given(request.isPreFlightRequest()).willReturn(false);

    CorsInterceptor interceptor = new CorsInterceptor(configSource);
    interceptor.setCorsProcessor(processor);

    boolean result = interceptor.beforeProcess(request, handler);

    assertThat(result).isTrue();
    verify(configSource).getCorsConfiguration(request);
    verify(processor).process(corsConfig, request);
    verify(request).isPreFlightRequest();
  }

  @Test
  void beforeProcessReturnsFalseWhenPreFlightRequest() throws Throwable {
    CorsConfigurationSource configSource = mock(CorsConfigurationSource.class);
    CorsProcessor processor = mock(CorsProcessor.class);
    RequestContext request = mock(RequestContext.class);
    Object handler = new Object();
    CorsConfiguration corsConfig = mock(CorsConfiguration.class);

    given(configSource.getCorsConfiguration(request)).willReturn(corsConfig);
    given(processor.process(corsConfig, request)).willReturn(true);
    given(request.isPreFlightRequest()).willReturn(true);

    CorsInterceptor interceptor = new CorsInterceptor(configSource);
    interceptor.setCorsProcessor(processor);

    boolean result = interceptor.beforeProcess(request, handler);

    assertThat(result).isFalse();
    verify(configSource).getCorsConfiguration(request);
    verify(processor).process(corsConfig, request);
    verify(request).isPreFlightRequest();
  }

  @Test
  void beforeProcessReturnsFalseWhenProcessorReturnsFalse() throws Throwable {
    CorsConfigurationSource configSource = mock(CorsConfigurationSource.class);
    CorsProcessor processor = mock(CorsProcessor.class);
    RequestContext request = mock(RequestContext.class);
    Object handler = new Object();
    CorsConfiguration corsConfig = mock(CorsConfiguration.class);

    given(configSource.getCorsConfiguration(request)).willReturn(corsConfig);
    given(processor.process(corsConfig, request)).willReturn(false);
    given(request.isPreFlightRequest()).willReturn(false);

    CorsInterceptor interceptor = new CorsInterceptor(configSource);
    interceptor.setCorsProcessor(processor);

    boolean result = interceptor.beforeProcess(request, handler);

    assertThat(result).isFalse();
    verify(configSource).getCorsConfiguration(request);
    verify(processor).process(corsConfig, request);
  }

}