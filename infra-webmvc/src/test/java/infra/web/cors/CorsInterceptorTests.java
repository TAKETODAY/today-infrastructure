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