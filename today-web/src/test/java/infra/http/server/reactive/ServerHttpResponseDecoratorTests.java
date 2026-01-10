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

package infra.http.server.reactive;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.util.function.Supplier;

import infra.core.io.buffer.DataBuffer;
import infra.http.HttpStatus;
import infra.http.ResponseCookie;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 14:12
 */
@SuppressWarnings("cast")
class ServerHttpResponseDecoratorTests {

  @Test
  void delegateMethodsAreCalled() {
    ServerHttpResponse delegate = mock(ServerHttpResponse.class);
    ServerHttpResponseDecorator decorator = new ServerHttpResponseDecorator(delegate);

    decorator.setStatusCode(HttpStatus.OK);
    verify(delegate).setStatusCode(HttpStatus.OK);

    decorator.getStatusCode();
    verify(delegate).getStatusCode();

    decorator.setRawStatusCode(200);
    verify(delegate).setRawStatusCode(200);

    decorator.getRawStatusCode();
    verify(delegate).getRawStatusCode();

    decorator.getHeaders();
    verify(delegate).getHeaders();

    decorator.getCookies();
    verify(delegate).getCookies();

    ResponseCookie cookie = ResponseCookie.from("name", "value").build();
    decorator.addCookie(cookie);
    verify(delegate).addCookie(cookie);

    decorator.bufferFactory();
    verify(delegate).bufferFactory();

    Supplier<Mono<Void>> action = () -> Mono.empty();
    decorator.beforeCommit(action);
    verify(delegate).beforeCommit(action);

    decorator.isCommitted();
    verify(delegate).isCommitted();

    @SuppressWarnings("unchecked")
    Publisher<DataBuffer> body = mock(Publisher.class);
    decorator.writeWith(body);
    verify(delegate).writeWith(body);

    @SuppressWarnings("unchecked")
    Publisher<Publisher<DataBuffer>> flushedBody = mock(Publisher.class);
    decorator.writeAndFlushWith(flushedBody);
    verify(delegate).writeAndFlushWith(flushedBody);

    decorator.setComplete();
    verify(delegate).setComplete();
  }

  @Test
  void getNativeResponseWithAbstractServerHttpResponse() {
    AbstractServerHttpResponse abstractResponse = mock(AbstractServerHttpResponse.class);
    Object nativeResponse = new Object();
    when(abstractResponse.getNativeResponse()).thenReturn(nativeResponse);

    assertThat((Object) ServerHttpResponseDecorator.getNativeResponse(abstractResponse)).isSameAs(nativeResponse);
  }

  @Test
  void getNativeResponseWithDecorator() {
    AbstractServerHttpResponse abstractResponse = mock(AbstractServerHttpResponse.class);
    Object nativeResponse = new Object();
    when(abstractResponse.getNativeResponse()).thenReturn(nativeResponse);

    ServerHttpResponseDecorator decorator1 = new ServerHttpResponseDecorator(abstractResponse);
    ServerHttpResponseDecorator decorator2 = new ServerHttpResponseDecorator(decorator1);

    assertThat((Object) ServerHttpResponseDecorator.getNativeResponse(decorator2)).isSameAs(nativeResponse);
  }

  @Test
  void getNativeResponseWithUnknownType() {
    ServerHttpResponse unknownResponse = mock(ServerHttpResponse.class);

    assertThatIllegalArgumentException()
            .isThrownBy(() -> ServerHttpResponseDecorator.getNativeResponse(unknownResponse))
            .withMessageContaining("Can't find native response");
  }

  @Test
  void constructorWithNullDelegate() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ServerHttpResponseDecorator(null))
            .withMessageContaining("Delegate is required");
  }

  @Test
  void toStringContainsDelegate() {
    ServerHttpResponse delegate = mock(ServerHttpResponse.class);
    when(delegate.toString()).thenReturn("MockDelegate");
    ServerHttpResponseDecorator decorator = new ServerHttpResponseDecorator(delegate);

    assertThat(decorator.toString()).contains("MockDelegate");
  }

}