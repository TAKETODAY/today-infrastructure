/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.server.support;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.lang.Constant;
import infra.web.RequestContext;
import infra.web.multipart.MultipartParser;
import infra.web.multipart.upload.DefaultMultipartParser;
import infra.web.server.error.SendErrorHandler;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.handler.codec.http.HttpHeadersFactory;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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
            .multipartParser(new DefaultMultipartParser())
            .build().writerCharset).isSameAs(Constant.DEFAULT_CHARSET);

    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .writerCharset(StandardCharsets.US_ASCII)
            .sendErrorHandler(new SendErrorHandler0())
            .multipartParser(new DefaultMultipartParser())
            .build();
    assertThat(config.writerCharset).isEqualTo(StandardCharsets.US_ASCII);
    assertThat(config.postRequestDecoderCharset).isSameAs(Constant.DEFAULT_CHARSET);

    assertThat(NettyRequestConfig.forBuilder(false)
            .postRequestDecoderCharset(StandardCharsets.US_ASCII)
            .sendErrorHandler(new SendErrorHandler0())
            .multipartParser(new DefaultMultipartParser())
            .build().postRequestDecoderCharset).isEqualTo(StandardCharsets.US_ASCII);
  }

  @Test
  void shouldBuildWithAllRequiredFields() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .build();

    // then
    assertThat(config).isNotNull();
    assertThat(config.sendErrorHandler).isEqualTo(sendErrorHandler);
    assertThat(config.multipartParser).isEqualTo(multipartParser);
    assertThat(config.responseBodyInitialCapacity).isEqualTo(128);
  }

  @Test
  void shouldSetCookieEncoderAndDecoder() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .cookieEncoder(ServerCookieEncoder.LAX)
            .cookieDecoder(ServerCookieDecoder.LAX)
            .build();

    // then
    assertThat(config.cookieEncoder).isEqualTo(ServerCookieEncoder.LAX);
    assertThat(config.cookieDecoder).isEqualTo(ServerCookieDecoder.LAX);
  }

  @Test
  void shouldSetResponseBodyInitialCapacity() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .responseBodyInitialCapacity(256)
            .build();

    // then
    assertThat(config.responseBodyInitialCapacity).isEqualTo(256);
  }

  @Test
  void shouldSetResponseBodyFactory() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);
    Function<RequestContext, ByteBuf> factory = mock(Function.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .responseBodyFactory(factory)
            .build();

    // then
    assertThat(config.responseBodyFactory).isEqualTo(factory);
  }

  @Test
  void shouldSetTrailerHeadersConsumer() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);
    Consumer consumer = mock(Consumer.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .trailerHeadersConsumer(consumer)
            .build();

    // then
    assertThat(config.trailerHeadersConsumer).isEqualTo(consumer);
  }

  @Test
  void shouldSetHeadersFactory() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);
    HttpHeadersFactory headersFactory = mock(HttpHeadersFactory.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .headersFactory(headersFactory)
            .build();

    // then
    assertThat(config.httpHeadersFactory).isEqualTo(headersFactory);
  }

  @Test
  void shouldSetSecureFlag() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(true)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .build();

    // then
    assertThat(config.secure).isTrue();
  }

  @Test
  void shouldUseDefaultValuesWhenNotSet() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .build();

    // then
    assertThat(config.cookieEncoder).isEqualTo(ServerCookieEncoder.STRICT);
    assertThat(config.cookieDecoder).isEqualTo(ServerCookieDecoder.STRICT);
    assertThat(config.responseBodyInitialCapacity).isEqualTo(128);
    assertThat(config.httpHeadersFactory).isNotNull();
    assertThat(config.postRequestDecoderCharset).isEqualTo(Constant.DEFAULT_CHARSET);
    assertThat(config.writerCharset).isEqualTo(Constant.DEFAULT_CHARSET);
    assertThat(config.secure).isFalse();
  }

  @Test
  void shouldThrowExceptionWhenSendErrorHandlerIsNull() {
    // given
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when & then
    assertThatThrownBy(() -> NettyRequestConfig.forBuilder(false)
            .multipartParser(multipartParser)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SendErrorHandler is required");
  }

  @Test
  void shouldThrowExceptionWhenHttpDataFactoryIsNull() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();

    // when & then
    assertThatThrownBy(() -> NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("HttpDataFactory is required");
  }

  @Test
  void shouldThrowExceptionWhenResponseBodyInitialCapacityIsNotPositive() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when & then
    assertThatThrownBy(() -> NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .responseBodyInitialCapacity(0)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("responseBodyInitialCapacity is required");
  }

  @Test
  void shouldUseProvidedCharsetsWhenSet() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .postRequestDecoderCharset(StandardCharsets.UTF_16)
            .writerCharset(StandardCharsets.ISO_8859_1)
            .build();

    // then
    assertThat(config.postRequestDecoderCharset).isEqualTo(StandardCharsets.UTF_16);
    assertThat(config.writerCharset).isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  void shouldUseDefaultCharsetsWhenNullProvided() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .postRequestDecoderCharset(null)
            .writerCharset(null)
            .build();

    // then
    assertThat(config.postRequestDecoderCharset).isEqualTo(Constant.DEFAULT_CHARSET);
    assertThat(config.writerCharset).isEqualTo(Constant.DEFAULT_CHARSET);
  }

  @Test
  void shouldUseDefaultCookieEncoderAndDecoderWhenNullProvided() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .cookieEncoder(null)
            .cookieDecoder(null)
            .build();

    // then
    assertThat(config.cookieEncoder).isEqualTo(ServerCookieEncoder.STRICT);
    assertThat(config.cookieDecoder).isEqualTo(ServerCookieDecoder.STRICT);
  }

  @Test
  void shouldUseDefaultHeadersFactoryWhenNullProvided() {
    // given
    SendErrorHandler sendErrorHandler = new NettyRequestConfigTests.SendErrorHandler0();
    MultipartParser multipartParser = mock(MultipartParser.class);

    // when
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler)
            .multipartParser(multipartParser)
            .headersFactory(null)
            .build();

    // then
    assertThat(config.httpHeadersFactory).isNotNull();
    assertThat(config.httpHeadersFactory).isEqualTo(DefaultHttpHeadersFactory.headersFactory());
  }

  static class SendErrorHandler0 implements SendErrorHandler {

    @Override
    public void handleError(RequestContext request, @Nullable String message) throws IOException {

    }

  }

}