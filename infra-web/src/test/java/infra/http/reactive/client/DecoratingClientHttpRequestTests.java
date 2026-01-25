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

package infra.http.reactive.client;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.core.AttributeAccessor;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 13:36
 */
@SuppressWarnings("cast")
class DecoratingClientHttpRequestTests {

  @Test
  void constructorWithValidDelegate() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.delegate()).isSameAs(delegate);
  }

  @Test
  void constructorWithNullDelegateThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DecoratingClientHttpRequest(null))
            .withMessage("Delegate is required");
  }

  @Test
  void getMethodDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    when(delegate.getMethod()).thenReturn(HttpMethod.POST);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.getMethod()).isEqualTo(HttpMethod.POST);
  }

  @Test
  void getURIDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    URI uri = URI.create("http://example.com");
    when(delegate.getURI()).thenReturn(uri);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.getURI()).isSameAs(uri);
  }

  @Test
  void getHeadersDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    when(delegate.getHeaders()).thenReturn(headers);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.getHeaders()).isSameAs(headers);
  }

  @Test
  void getCookiesDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
    when(delegate.getCookies()).thenReturn(cookies);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.getCookies()).isSameAs(cookies);
  }

  @Test
  void bufferFactoryDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    DataBufferFactory bufferFactory = mock(DataBufferFactory.class);
    when(delegate.bufferFactory()).thenReturn(bufferFactory);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.bufferFactory()).isSameAs(bufferFactory);
  }

  @Test
  void getNativeRequestDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    Object nativeRequest = new Object();
    when(delegate.getNativeRequest()).thenReturn(nativeRequest);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat((Object) decorator.getNativeRequest()).isSameAs(nativeRequest);
  }

  @Test
  void beforeCommitDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    Supplier<Mono<Void>> action = () -> Mono.empty();
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    decorator.beforeCommit(action);
    verify(delegate).beforeCommit(action);
  }

  @Test
  void isCommittedDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    when(delegate.isCommitted()).thenReturn(true);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.isCommitted()).isTrue();
  }

  @Test
  void writeWithDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    Publisher<DataBuffer> body = mock(Publisher.class);
    Mono<Void> result = Mono.empty();
    when(delegate.writeWith(body)).thenReturn(result);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.writeWith(body)).isSameAs(result);
  }

  @Test
  void writeAndFlushWithDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    Publisher<Publisher<DataBuffer>> body = mock(Publisher.class);
    Mono<Void> result = Mono.empty();
    when(delegate.writeAndFlushWith(body)).thenReturn(result);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.writeAndFlushWith(body)).isSameAs(result);
  }

  @Test
  void setCompleteDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    Mono<Void> result = Mono.empty();
    when(delegate.setComplete()).thenReturn(result);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.setComplete()).isSameAs(result);
  }

  @Test
  void setAttributesDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    Map<String, Object> attributes = Map.of("key", "value");
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    decorator.setAttributes(attributes);
    verify(delegate).setAttributes(attributes);
  }

  @Test
  void attributeNamesDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    Iterable<String> attributeNames = List.of("attr1", "attr2");
    when(delegate.attributeNames()).thenReturn(attributeNames);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.attributeNames()).isSameAs(attributeNames);
  }

  @Test
  void clearAttributesDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    decorator.clearAttributes();
    verify(delegate).clearAttributes();
  }

  @Test
  void computeAttributeDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    Function<String, String> computeFunction = s -> "computed";
    when(delegate.computeAttribute("name", computeFunction)).thenReturn("computed");
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.computeAttribute("name", computeFunction)).isEqualTo("computed");
  }

  @Test
  void copyFromDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    AttributeAccessor source = mock(AttributeAccessor.class);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    decorator.copyFrom(source);
    verify(delegate).copyFrom(source);
  }

  @Test
  void getAttributeDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    when(delegate.getAttribute("name")).thenReturn("value");
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.getAttribute("name")).isEqualTo("value");
  }

  @Test
  void getAttributeNamesDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    String[] attributeNames = { "attr1", "attr2" };
    when(delegate.getAttributeNames()).thenReturn(attributeNames);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.getAttributeNames()).isSameAs(attributeNames);
  }

  @Test
  void getAttributesDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    Map<String, Object> attributes = Map.of("key", "value");
    when(delegate.getAttributes()).thenReturn(attributes);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.getAttributes()).isSameAs(attributes);
  }

  @Test
  void hasAttributeDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    when(delegate.hasAttribute("name")).thenReturn(true);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.hasAttribute("name")).isTrue();
  }

  @Test
  void hasAttributesDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    when(delegate.hasAttributes()).thenReturn(true);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.hasAttributes()).isTrue();
  }

  @Test
  void removeAttributeDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    when(delegate.removeAttribute("name")).thenReturn("value");
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.removeAttribute("name")).isEqualTo("value");
  }

  @Test
  void setAttributeDelegatesToWrappedRequest() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    decorator.setAttribute("name", "value");
    verify(delegate).setAttribute("name", "value");
  }

  @Test
  void toStringReturnsFormattedString() {
    ClientHttpRequest delegate = mock(ClientHttpRequest.class);
    when(delegate.toString()).thenReturn("MockDelegate");
    DecoratingClientHttpRequest decorator = new DecoratingClientHttpRequest(delegate);
    assertThat(decorator.toString()).isEqualTo("ClientHttpRequestDecorator [delegate=MockDelegate]");
  }

}