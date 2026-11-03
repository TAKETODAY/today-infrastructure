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

package infra.web.server.netty;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.web.DispatcherHandler;
import infra.web.server.HttpRequestExecutor;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpTrafficHandler} error handling.
 */
class HttpTrafficHandlerTests {

  @Test
  void exceptionAfterCommitClosesChannel() {
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .multipartParser(mock())
            .sendErrorHandler(mock())
            .build();
    HttpRequestExecutor executor = (context, command) -> {
      ((NettyHttpContext) context).writeHeaders();
      throw new IllegalStateException("boom");
    };
    HttpTrafficHandler trafficHandler = new HttpTrafficHandler(
            config, mock(ApplicationContext.class), mock(DispatcherHandler.class), executor);

    EmbeddedChannel channel = new EmbeddedChannel(trafficHandler);
    HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");

    channel.writeInbound(request);

    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  void exceptionBeforeCommitWritesErrorResponse() {
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .multipartParser(mock())
            .sendErrorHandler(mock())
            .build();
    HttpRequestExecutor executor = (context, command) -> {
      throw new IllegalStateException("boom");
    };
    HttpTrafficHandler trafficHandler = new HttpTrafficHandler(
            config, mock(ApplicationContext.class), mock(DispatcherHandler.class), executor);

    EmbeddedChannel channel = new EmbeddedChannel(trafficHandler);
    HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");

    channel.writeInbound(request);

    assertThat(channel.outboundMessages()).hasSize(1);
  }

}
