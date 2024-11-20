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

package cn.taketoday.web.server.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.server.error.SendErrorHandler;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/11/12 17:07
 */
class NettyRequestConfigTests {

  @Test
  void charset() {
    assertThatThrownBy(() -> NettyRequestConfig.forBuilder(false)
            .build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SendErrorHandler is required");

    assertThatThrownBy(() -> NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(new SendErrorHandler0())
            .build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("HttpDataFactory is required");

    assertThat(NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(new SendErrorHandler0())
            .httpDataFactory(new DefaultHttpDataFactory())
            .build().writerCharset).isSameAs(Constant.DEFAULT_CHARSET);

    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .writerCharset(StandardCharsets.US_ASCII)
            .sendErrorHandler(new SendErrorHandler0())
            .httpDataFactory(new DefaultHttpDataFactory())
            .build();
    assertThat(config.writerCharset).isEqualTo(StandardCharsets.US_ASCII);
    assertThat(config.postRequestDecoderCharset).isSameAs(Constant.DEFAULT_CHARSET);

    assertThat(NettyRequestConfig.forBuilder(false)
            .postRequestDecoderCharset(StandardCharsets.US_ASCII)
            .sendErrorHandler(new SendErrorHandler0())
            .httpDataFactory(new DefaultHttpDataFactory())
            .build().postRequestDecoderCharset).isEqualTo(StandardCharsets.US_ASCII);
  }

  static class SendErrorHandler0 implements SendErrorHandler {

    @Override
    public void handleError(RequestContext request, @Nullable String message) throws IOException {

    }

  }

}