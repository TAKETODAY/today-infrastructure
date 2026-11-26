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

package infra.web.handler.result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.web.ReturnValueHandler;
import infra.web.handler.function.RouterFunctions;
import infra.web.handler.function.ServerRequest;
import infra.web.handler.function.ServerResponse;
import infra.web.mock.MockServerHttpExchange;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

/**
 * Tests for {@link ServerResponseReturnValueHandler}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class ServerResponseReturnValueHandlerTests {

  private final List<HttpMessageConverter<?>> messageConverters = List.of(new StringHttpMessageConverter());

  private MockServerHttpExchange exchange;

  private ServerRequest serverRequest;

  private ReturnValueHandler delegate;

  private ServerResponseReturnValueHandler handler;

  @BeforeEach
  void setUp() {
    this.exchange = new MockServerHttpExchange(new MockRequest(), new MockResponse());
    this.serverRequest = ServerRequest.create(this.exchange, this.messageConverters);
    this.exchange.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, this.serverRequest);

    this.delegate = mock(ReturnValueHandler.class);
    this.handler = new ServerResponseReturnValueHandler(this.delegate);
  }

  @Test
  void supportsServerResponse() {
    ServerResponse response = mock(ServerResponse.class);

    assertThat(this.handler.supportsReturnValue(response)).isTrue();
    assertThat(this.handler.supportsHandler(new Object(), response)).isTrue();
    assertThat(this.handler.supportsReturnValue("other")).isFalse();
    assertThat(this.handler.supportsHandler(new Object(), null)).isFalse();
  }

  @Test
  void writesResponseAndContinuesWithReturnedValue() throws Exception {
    ServerResponse response = mock(ServerResponse.class);
    Object handler = new Object();
    given(response.writeTo(this.exchange, this.serverRequest)).willReturn("view");

    this.handler.handleReturnValue(this.exchange, handler, response);

    verify(this.delegate).handleReturnValue(this.exchange, handler, "view");
  }

  @Test
  void stopsWhenResponseIsFullyWritten() throws Exception {
    ServerResponse response = mock(ServerResponse.class);
    given(response.writeTo(this.exchange, this.serverRequest)).willReturn(ReturnValueHandler.NONE_RETURN_VALUE);

    this.handler.handleReturnValue(this.exchange, null, response);

    verify(this.delegate, never()).handleReturnValue(any(), any(), any());
  }

  @Test
  void delegatesEmitterToReturnValueChain() throws Exception {
    ServerResponse response = mock(ServerResponse.class);
    SseEmitter emitter = ResponseBodyEmitter.forServerSentEvents();
    given(response.writeTo(this.exchange, this.serverRequest)).willReturn(emitter);

    this.handler.handleReturnValue(this.exchange, null, response);

    verify(this.delegate).handleReturnValue(this.exchange, null, emitter);
  }

}
