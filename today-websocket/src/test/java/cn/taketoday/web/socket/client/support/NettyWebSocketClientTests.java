/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.socket.client.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.util.DataSize;
import io.netty.handler.codec.http.HttpDecoderConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/2 22:24
 */
class NettyWebSocketClientTests {

  final NettyWebSocketClient client = new NettyWebSocketClient();

  @Test
  void config() {
    assertThat(client).extracting("failOnMissingResponse").isEqualTo(false);
    assertThat(client).extracting("parseHttpAfterConnectRequest").isEqualTo(false);
    assertThat(client).extracting("maxContentLength").isEqualTo(DataSize.ofKilobytes(64).toBytesInt());
    assertThat(client).extracting("closeOnExpectationFailed").isEqualTo(false);
    assertThat(client).extracting("sessionDecorator").isNull();
    assertThat(client).extracting("httpDecoderConfig").isNotNull();

    client.setFailOnMissingResponse(true);
    client.setParseHttpAfterConnectRequest(true);
    client.setMaxContentLength(100);
    client.setCloseOnExpectationFailed(true);
    client.setSessionDecorator(s -> s);
    client.setHttpDecoderConfig(new HttpDecoderConfig());

    assertThat(client).extracting("failOnMissingResponse").isEqualTo(true);
    assertThat(client).extracting("parseHttpAfterConnectRequest").isEqualTo(true);
    assertThat(client).extracting("maxContentLength").isEqualTo(100);
    assertThat(client).extracting("closeOnExpectationFailed").isEqualTo(true);
    assertThat(client).extracting("sessionDecorator").isNotNull();
    assertThat(client).extracting("httpDecoderConfig").isNotNull();

    assertThatThrownBy(() ->
            client.setHttpDecoderConfig(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("httpDecoderConfig is required");
  }

}