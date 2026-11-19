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

package infra.web.socket.client.support;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import static org.assertj.core.api.Assertions.assertThat;

class NettyClientHttpResponseTests {

  @Test
  void exposesStatusHeadersBodyAndCharset() throws Exception {
    DefaultFullHttpResponse nativeResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.BAD_REQUEST, Unpooled.copiedBuffer("bad", StandardCharsets.UTF_8));
    nativeResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    NettyClientHttpResponse response = new NettyClientHttpResponse(nativeResponse);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getStatusText()).isEqualTo("Bad Request");
    assertThat(response.getHeaders().getFirst(HttpHeaderNames.CONTENT_TYPE.toString())).contains("text/plain");
    assertThat(response.getCharset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(response.bodyByteArray()).containsExactly('b', 'a', 'd');
    assertThat(response.getBody().readAllBytes()).containsExactly('b', 'a', 'd');
    response.close();
  }

  @Test
  void returnsNullCharsetWhenContentTypeDoesNotSpecifyOne() {
    DefaultFullHttpResponse nativeResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
    NettyClientHttpResponse response = new NettyClientHttpResponse(nativeResponse);
    assertThat(response.getCharset()).isNull();
    response.close();
  }

}
