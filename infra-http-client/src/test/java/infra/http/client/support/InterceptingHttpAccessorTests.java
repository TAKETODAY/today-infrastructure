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

package infra.http.client.support;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.client.BufferingClientHttpRequestFactory;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestExecution;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInitializer;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.client.ClientHttpResponse;
import infra.http.client.InterceptingClientHttpRequestFactory;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link InterceptingHttpAccessor}.
 *
 * @author Brian Clozel
 */
public class InterceptingHttpAccessorTests {

  @Test
  public void getInterceptors() {
    TestInterceptingHttpAccessor accessor = new TestInterceptingHttpAccessor();
    List<ClientHttpRequestInterceptor> interceptors = Arrays.asList(
            new SecondClientHttpRequestInterceptor(),
            new ThirdClientHttpRequestInterceptor(),
            new FirstClientHttpRequestInterceptor()

    );
    accessor.setInterceptors(interceptors);

    assertThat(accessor.getInterceptors().get(0)).isInstanceOf(FirstClientHttpRequestInterceptor.class);
    assertThat(accessor.getInterceptors().get(1)).isInstanceOf(SecondClientHttpRequestInterceptor.class);
    assertThat(accessor.getInterceptors().get(2)).isInstanceOf(ThirdClientHttpRequestInterceptor.class);
  }

  @Test
  void sortsVarargsAndKeepsTheLiveListWhenSetAgain() {
    var accessor = new TestInterceptingHttpAccessor();
    var first = new FirstClientHttpRequestInterceptor();
    var last = new ThirdClientHttpRequestInterceptor();
    accessor.setInterceptors(last, first);
    accessor.setInterceptors(accessor.getInterceptors());

    assertThat(accessor.getInterceptors()).containsExactly(first, last);
  }

  @Test
  void cachesInterceptingFactoryUntilTransportIsReplaced() throws Exception {
    var accessor = new TestInterceptingHttpAccessor();
    var uri = URI.create("https://example.test/");
    ClientHttpRequestFactory transport = mock();
    ClientHttpRequestFactory replacement = mock();
    accessor.setRequestFactory(transport);
    assertThat(accessor.getRequestFactory()).isSameAs(transport);

    accessor.setInterceptors((request, body, execution) -> execution.execute(request, body));
    var wrapped = accessor.getRequestFactory();
    assertThat(wrapped).isInstanceOf(InterceptingClientHttpRequestFactory.class);
    assertThat(accessor.getRequestFactory()).isSameAs(wrapped);

    ClientHttpResponse expected = mock();
    ClientHttpRequest request = mock();
    given(replacement.createRequest(uri, HttpMethod.GET)).willReturn(request);
    given(request.getHeaders()).willReturn(HttpHeaders.forWritable());
    given(request.execute()).willReturn(expected);
    accessor.setRequestFactory(replacement);
    assertThat(accessor.getRequestFactory()).isNotSameAs(wrapped);
    assertThat(accessor.createRequest(uri, HttpMethod.GET).execute()).isSameAs(expected);
  }

  @Test
  void configuresBufferingAndRunsInitializersInOrder() throws Exception {
    var accessor = new TestInterceptingHttpAccessor();
    var uri = URI.create("https://example.test/");
    ClientHttpRequestFactory transport = mock();
    ClientHttpRequest request = mock();
    given(transport.createRequest(uri, HttpMethod.GET)).willReturn(request);
    accessor.setRequestFactory(transport);
    assertThatIllegalArgumentException().isThrownBy(() -> accessor.setRequestFactory(null));
    Predicate<HttpRequest> neverBuffer = candidate -> false;
    accessor.setBufferingPredicate(neverBuffer);
    assertThat(accessor.getBufferingPredicate()).isSameAs(neverBuffer);
    assertThat(accessor.getRequestFactory()).isInstanceOf(BufferingClientHttpRequestFactory.class);

    var calls = new ArrayList<String>();
    ClientHttpRequestInitializer first = new OrderedInitializer(1, candidate -> {
      assertThat(candidate).isSameAs(request);
      calls.add("first");
    });
    ClientHttpRequestInitializer second = new OrderedInitializer(2, candidate -> calls.add("second"));
    accessor.setHttpRequestInitializers(List.of(second, first));
    accessor.setHttpRequestInitializers(accessor.getHttpRequestInitializers());

    assertThat(accessor.createRequest(uri, HttpMethod.GET)).isSameAs(request);
    assertThat(calls).containsExactly("first", "second");
  }

  @Test
  void logsTheMethodAndUriWhenRequestDebugLoggingIsEnabled() throws Exception {
    var uri = URI.create("https://example.test/");
    Logger logger = mock();
    given(logger.isDebugEnabled()).willReturn(true);
    try (var loggerFactory = mockStatic(LoggerFactory.class)) {
      loggerFactory.when(() -> LoggerFactory.getLogger(TestInterceptingHttpAccessor.class)).thenReturn(logger);
      var accessor = new TestInterceptingHttpAccessor();
      ClientHttpRequest request = mock();
      accessor.setRequestFactory((url, method) -> request);

      assertThat(accessor.createRequest(uri, HttpMethod.GET)).isSameAs(request);
      verify(logger).debug("HTTP {} {}", "GET", uri);
    }
  }

  private record OrderedInitializer(int getOrder, ClientHttpRequestInitializer delegate)
          implements ClientHttpRequestInitializer, Ordered {

    @Override
    public void initialize(ClientHttpRequest request) {
      this.delegate.initialize(request);
    }
  }

  private static class TestInterceptingHttpAccessor extends InterceptingHttpAccessor {
  }

  @Order(1)
  private class FirstClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
      return null;
    }
  }

  private class SecondClientHttpRequestInterceptor implements ClientHttpRequestInterceptor, Ordered {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
      return null;
    }

    @Override
    public int getOrder() {
      return 2;
    }
  }

  private class ThirdClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
      return null;
    }
  }

}
