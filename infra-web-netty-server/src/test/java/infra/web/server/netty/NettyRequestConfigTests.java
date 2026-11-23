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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Function;

import infra.util.DataSize;
import infra.web.HttpContext;
import infra.web.multipart.MultipartParser;
import infra.web.server.error.SendErrorHandler;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/11/12 17:07
 */
class NettyRequestConfigTests {

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
    Function<HttpContext, ByteBuf> factory = mock(Function.class);

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
    assertThat(config.secure).isFalse();
  }

  @Test
  void shouldConvertRequestBodyBufferSizesToBytes() {
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(new SendErrorHandler0())
            .multipartParser(mock(MultipartParser.class))
            .requestBodyHighWaterMark(DataSize.ofKilobytes(128))
            .requestBodyLowWaterMark(DataSize.ofBytes(0))
            .maxBufferedRequestBodyBytes(DataSize.ofMegabytes(1))
            .maxBufferedRequestBodyChunks(64)
            .build();

    assertThat(config.requestBodyBufferLimits)
            .isEqualTo(new NettyRequestConfig.BufferLimits(131_072, 0, 1_048_576, 64));
  }

  @Test
  void shouldValidateRequestBodyWatermarks() {
    MultipartParser multipartParser = mock(MultipartParser.class);
    SendErrorHandler sendErrorHandler = new SendErrorHandler0();

    assertThatThrownBy(() -> NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler).multipartParser(multipartParser)
            .requestBodyLowWaterMark(DataSize.ofBytes(256))
            .requestBodyHighWaterMark(DataSize.ofBytes(256)).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("requestBodyLowWaterMark must be less than requestBodyHighWaterMark");

    assertThatThrownBy(() -> NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(sendErrorHandler).multipartParser(multipartParser)
            .requestBodyLowWaterMark(DataSize.ofBytes(64))
            .requestBodyHighWaterMark(DataSize.ofBytes(513))
            .maxBufferedRequestBodyBytes(DataSize.ofBytes(512)).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("requestBodyHighWaterMark must not be greater than maxBufferedRequestBodyBytes");
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
            .hasMessage("MultipartParser is required");
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

  static class SendErrorHandler0 implements SendErrorHandler {

    @Override
    public void handleError(HttpContext http, @Nullable String message) throws IOException {

    }

  }

}
