/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.http.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InterceptingClientHttpRequestTests {

  private static final URI URI_VALUE = URI.create("https://example.test/orders");

  @Test
  void forwardsRewrittenBodyAndAttributesAndBuffersTheResponseSelectedByThePredicate() throws Exception {
    var calls = new ArrayList<String>();
    var output = new ByteArrayOutputStream();
    var headers = HttpHeaders.forWritable();
    ClientHttpRequest delegate = mock();
    ClientHttpResponse response = mock();
    given(delegate.getHeaders()).willReturn(headers);
    given(delegate.getBody()).willReturn(output);
    given(delegate.getContentLength()).willAnswer(invocation -> headers.getContentLength());
    given(delegate.execute()).willAnswer(invocation -> {
      calls.add("delegate");
      return response;
    });
    given(response.getBody()).willReturn(new ByteArrayInputStream(new byte[] { 7, 9 }));
    given(response.getStatusCode()).willReturn(HttpStatus.ACCEPTED);
    given(response.getRawStatusCode()).willReturn(202);
    given(response.getStatusText()).willReturn("Accepted");
    ClientHttpRequestFactory transport = (uri, method) -> {
      assertThat(uri).isEqualTo(URI_VALUE);
      assertThat(method).isEqualTo(HttpMethod.POST);
      return delegate;
    };
    ClientHttpRequestInterceptor first = (request, body, next) -> {
      calls.add("first");
      assertThat(body).containsExactly(1);
      request.setAttribute("trace", "local");
      return next.execute(request, "rewritten".getBytes(StandardCharsets.UTF_8));
    };
    ClientHttpRequestInterceptor second = (request, body, next) -> {
      calls.add("second");
      request.getHeaders().add("X-Buffer", "yes");
      return next.execute(request, body);
    };
    var factory = new InterceptingClientHttpRequestFactory(transport, List.of(first, second),
            request -> "yes".equals(request.getHeaders().getFirst("X-Buffer")));
    var request = factory.createRequest(URI_VALUE, HttpMethod.POST);
    request.getBody().write(1);

    try (var result = request.execute()) {
      assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
      assertThat(result.getRawStatusCode()).isEqualTo(202);
      assertThat(result.getStatusText()).isEqualTo("Accepted");
      assertThat(result.getBody().readAllBytes()).containsExactly(7, 9);
      assertThat(result.getBody().readAllBytes()).containsExactly(7, 9);
    }
    assertThat(calls).containsExactly("first", "second", "delegate");
    assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("rewritten");
    assertThat(headers.getFirst("X-Buffer")).isEqualTo("yes");
    verify(delegate).copyAttributeFrom(request);
    verify(delegate).setContentLength(9);
    verify(response).getBody();
    verify(response).close();
  }

  @Test
  void executesAnEmptyChainAsynchronouslyAndExposesReadOnlyHeadersAfterwards() throws Exception {
    ClientHttpRequest delegate = mock();
    ClientHttpResponse response = mock();
    given(delegate.getHeaders()).willReturn(HttpHeaders.forWritable());
    given(delegate.execute()).willReturn(response);
    var factory = new InterceptingClientHttpRequestFactory((uri, method) -> delegate, null);
    var request = factory.createRequest(URI_VALUE, HttpMethod.GET);
    request.getHeaders().set("X-Test", "original");

    assertThat(request.async(Runnable::run).get(1, TimeUnit.SECONDS)).isSameAs(response);
    assertThat(delegate.getHeaders().getFirst("X-Test")).isEqualTo("original");
    assertThat(request.getHeaders()).isSameAs(request.getHeaders());
    assertThatIllegalStateException().isThrownBy(request::getBody).withMessageContaining("already executed");
    assertThatIllegalStateException().isThrownBy(request::execute).withMessageContaining("already executed");
  }

  @Test
  void preservesAnUnspecifiedContentLengthWhenWritingTheInterceptedBody() throws IOException {
    ClientHttpRequest delegate = mock();
    ClientHttpResponse response = mock();
    var output = new ByteArrayOutputStream();
    given(delegate.getHeaders()).willReturn(HttpHeaders.forWritable());
    given(delegate.getContentLength()).willReturn(-1L);
    given(delegate.getBody()).willReturn(output);
    given(delegate.execute()).willReturn(response);
    var factory = new InterceptingClientHttpRequestFactory((uri, method) -> delegate,
            List.of((request, body, next) -> {
              request.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
              return next.execute(request, body);
            }));
    var request = factory.createRequest(URI_VALUE, HttpMethod.POST);
    request.getBody().write(7);

    assertThat(request.execute()).isSameAs(response);
    assertThat(output.toByteArray()).containsExactly(7);
    assertThat(delegate.getHeaders().contains(HttpHeaders.CONTENT_LENGTH)).isFalse();
    verify(delegate, never()).setContentLength(1);
  }

  @Test
  void wrapsOnlyTheRequestsSelectedForBufferingAndDelegatesTheMethodName() throws IOException {
    ClientHttpRequest delegate = mock();
    ClientHttpRequest get = mock();
    given(get.getMethod()).willReturn(HttpMethod.GET);
    given(delegate.getMethod()).willReturn(HttpMethod.POST);
    given(delegate.getMethodAsString()).willReturn("POST");
    ClientHttpRequestFactory transport = (uri, method) -> method == HttpMethod.POST ? delegate : get;
    var wrapper = new ClientHttpRequestFactoryWrapper(transport);
    var buffering = new BufferingClientHttpRequestFactory(wrapper, request -> request.getMethod() == HttpMethod.POST);

    assertThat(wrapper.createRequest(URI_VALUE, HttpMethod.POST)).isSameAs(delegate);
    var buffered = buffering.createRequest(URI_VALUE, HttpMethod.POST);
    assertThat(buffered).isInstanceOf(BufferingClientHttpRequestWrapper.class);
    assertThat(buffered.getMethodAsString()).isEqualTo("POST");
    assertThat(buffering.createRequest(URI_VALUE, HttpMethod.GET)).isSameAs(get);
  }

  @Test
  void abortsWithoutCreatingATransportRequestWhenInterceptorRejectsTheRequest() throws IOException {
    ClientHttpResponse response = mock();
    ClientHttpRequestFactory transport = (uri, method) -> {
      throw new AssertionError("Transport must not be called");
    };
    var factory = new InterceptingClientHttpRequestFactory(transport, List.of((request, body, next) -> response));
    assertThat(factory.createRequest(URI_VALUE, HttpMethod.GET).execute()).isSameAs(response);
  }

}
