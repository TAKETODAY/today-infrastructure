/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.codec;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.codec.CharSequenceEncoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.server.reactive.MockServerHttpResponse;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static cn.taketoday.core.ResolvableType.fromClass;
import static cn.taketoday.http.MediaType.TEXT_HTML;
import static cn.taketoday.http.MediaType.TEXT_PLAIN;
import static cn.taketoday.http.MediaType.TEXT_XML;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link EncoderHttpMessageWriter}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class EncoderHttpMessageWriterTests {

  private static final Map<String, Object> NO_HINTS = Collections.emptyMap();

  private static final MediaType TEXT_PLAIN_UTF_8 = new MediaType("text", "plain", UTF_8);

  @Mock
  private HttpMessageEncoder<String> encoder;

  private final ArgumentCaptor<MediaType> mediaTypeCaptor = ArgumentCaptor.forClass(MediaType.class);

  private final MockServerHttpResponse response = new MockServerHttpResponse();

  @Test
  void getWritableMediaTypes() {
    configureEncoder(MimeTypeUtils.TEXT_HTML, MimeTypeUtils.TEXT_XML);
    HttpMessageWriter<?> writer = new EncoderHttpMessageWriter<>(this.encoder);
    assertThat(writer.getWritableMediaTypes()).isEqualTo(Arrays.asList(TEXT_HTML, TEXT_XML));
  }

  @Test
  void canWrite() {
    configureEncoder(MimeTypeUtils.TEXT_HTML);
    HttpMessageWriter<?> writer = new EncoderHttpMessageWriter<>(this.encoder);
    given(this.encoder.canEncode(fromClass(String.class), TEXT_HTML)).willReturn(true);

    assertThat(writer.canWrite(fromClass(String.class), TEXT_HTML)).isTrue();
    assertThat(writer.canWrite(fromClass(String.class), TEXT_XML)).isFalse();
  }

  @Test
  void useNegotiatedMediaType() {
    configureEncoder(TEXT_PLAIN);
    HttpMessageWriter<String> writer = new EncoderHttpMessageWriter<>(this.encoder);
    writer.write(Flux.empty(), fromClass(String.class), TEXT_PLAIN, this.response, NO_HINTS);

    assertThat(response.getHeaders().getContentType()).isEqualTo(TEXT_PLAIN);
    assertThat(this.mediaTypeCaptor.getValue()).isEqualTo(TEXT_PLAIN);
  }

  @Test
  void useDefaultMediaType() {
    testDefaultMediaType(null);
    testDefaultMediaType(new MediaType("text", "*"));
    testDefaultMediaType(new MediaType("*", "*"));
    testDefaultMediaType(MediaType.APPLICATION_OCTET_STREAM);
  }

  private void testDefaultMediaType(MediaType negotiatedMediaType) {
    MimeType defaultContentType = MimeTypeUtils.TEXT_XML;
    configureEncoder(defaultContentType);
    HttpMessageWriter<String> writer = new EncoderHttpMessageWriter<>(this.encoder);
    writer.write(Flux.empty(), fromClass(String.class), negotiatedMediaType, this.response, NO_HINTS);

    assertThat(this.response.getHeaders().getContentType()).isEqualTo(defaultContentType);
    assertThat(this.mediaTypeCaptor.getValue()).isEqualTo(defaultContentType);
  }

  @Test
  void useDefaultMediaTypeCharset() {
    configureEncoder(TEXT_PLAIN_UTF_8, TEXT_HTML);
    HttpMessageWriter<String> writer = new EncoderHttpMessageWriter<>(this.encoder);
    writer.write(Flux.empty(), fromClass(String.class), TEXT_HTML, response, NO_HINTS);

    assertThat(this.response.getHeaders().getContentType()).isEqualTo(new MediaType("text", "html", UTF_8));
    assertThat(this.mediaTypeCaptor.getValue()).isEqualTo(new MediaType("text", "html", UTF_8));
  }

  @Test
  void useNegotiatedMediaTypeCharset() {
    MediaType negotiatedMediaType = new MediaType("text", "html", ISO_8859_1);
    configureEncoder(TEXT_PLAIN_UTF_8, TEXT_HTML);
    HttpMessageWriter<String> writer = new EncoderHttpMessageWriter<>(this.encoder);
    writer.write(Flux.empty(), fromClass(String.class), negotiatedMediaType, this.response, NO_HINTS);

    assertThat(this.response.getHeaders().getContentType()).isEqualTo(negotiatedMediaType);
    assertThat(this.mediaTypeCaptor.getValue()).isEqualTo(negotiatedMediaType);
  }

  @Test
  void useHttpOutputMessageMediaType() {
    MediaType outputMessageMediaType = MediaType.TEXT_HTML;
    this.response.getHeaders().setContentType(outputMessageMediaType);

    configureEncoder(TEXT_PLAIN_UTF_8, TEXT_HTML);
    HttpMessageWriter<String> writer = new EncoderHttpMessageWriter<>(this.encoder);
    writer.write(Flux.empty(), fromClass(String.class), TEXT_PLAIN, this.response, NO_HINTS);

    assertThat(this.response.getHeaders().getContentType()).isEqualTo(outputMessageMediaType);
    assertThat(this.mediaTypeCaptor.getValue()).isEqualTo(outputMessageMediaType);
  }

  @Test
  void setContentLengthForMonoBody() {
    DefaultDataBufferFactory factory = DefaultDataBufferFactory.sharedInstance;
    DataBuffer buffer = factory.wrap("body".getBytes(StandardCharsets.UTF_8));
    configureEncoder(Flux.just(buffer), MimeTypeUtils.TEXT_PLAIN);
    HttpMessageWriter<String> writer = new EncoderHttpMessageWriter<>(this.encoder);
    writer.write(Mono.just("body"), fromClass(String.class), TEXT_PLAIN, this.response, NO_HINTS).block();

    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(4);
  }

  @Test
    // gh-22952
  void monoBodyDoesNotCancelEncodedFlux() {
    Mono<String> inputStream = Mono.just("body")
            .doOnCancel(() -> {
              throw new AssertionError("Cancel signal not expected");
            });
    new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes())
            .write(inputStream, fromClass(String.class), TEXT_PLAIN, this.response, NO_HINTS)
            .block();
  }

  @Test
    // SPR-17220
  void emptyBodyWritten() {
    configureEncoder(MimeTypeUtils.TEXT_PLAIN);
    HttpMessageWriter<String> writer = new EncoderHttpMessageWriter<>(this.encoder);
    writer.write(Mono.empty(), fromClass(String.class), TEXT_PLAIN, this.response, NO_HINTS).block();
    StepVerifier.create(this.response.getBody()).verifyComplete();
    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(0);
  }

  @Test
    // gh-22936
  void isStreamingMediaType() throws InvocationTargetException, IllegalAccessException {
    configureEncoder(TEXT_HTML);
    MediaType streamingMediaType = new MediaType(TEXT_PLAIN, Collections.singletonMap("streaming", "true"));
    given(this.encoder.getStreamingMediaTypes()).willReturn(Arrays.asList(streamingMediaType));

    HttpMessageWriter<String> writer = new EncoderHttpMessageWriter<>(this.encoder);
    Method method = ReflectionUtils.findMethod(writer.getClass(), "isStreamingMediaType", MediaType.class);
    ReflectionUtils.makeAccessible(method);

    assertThat((boolean) (Boolean) method.invoke(writer, streamingMediaType)).isTrue();
    assertThat((boolean) (Boolean) method.invoke(writer, new MediaType(TEXT_PLAIN, Collections.singletonMap("streaming", "false")))).isFalse();
    assertThat((boolean) (Boolean) method.invoke(writer, TEXT_HTML)).isFalse();
  }

  private void configureEncoder(MimeType... mimeTypes) {
    configureEncoder(Flux.empty(), mimeTypes);
  }

  private void configureEncoder(Flux<DataBuffer> encodedStream, MimeType... mimeTypes) {
    List<MimeType> typeList = Arrays.asList(mimeTypes);
    given(this.encoder.getEncodableMimeTypes()).willReturn(typeList);
    given(this.encoder.encode(any(), any(), any(), this.mediaTypeCaptor.capture(), any()))
            .willReturn(encodedStream);
  }

}
