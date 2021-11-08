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

package cn.taketoday.http.codec.support;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.core.codec.ByteArrayDecoder;
import cn.taketoday.core.codec.ByteArrayEncoder;
import cn.taketoday.core.codec.ByteBufferDecoder;
import cn.taketoday.core.codec.ByteBufferEncoder;
import cn.taketoday.core.codec.CharSequenceEncoder;
import cn.taketoday.core.codec.DataBufferDecoder;
import cn.taketoday.core.codec.DataBufferEncoder;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.Encoder;
import cn.taketoday.core.codec.NettyByteBufDecoder;
import cn.taketoday.core.codec.NettyByteBufEncoder;
import cn.taketoday.core.codec.ResourceDecoder;
import cn.taketoday.core.codec.StringDecoder;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.codec.CodecConfigurer;
import cn.taketoday.http.codec.DecoderHttpMessageReader;
import cn.taketoday.http.codec.EncoderHttpMessageWriter;
import cn.taketoday.http.codec.FormHttpMessageReader;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.http.codec.ResourceHttpMessageReader;
import cn.taketoday.http.codec.ResourceHttpMessageWriter;
import cn.taketoday.http.codec.ServerCodecConfigurer;
import cn.taketoday.http.codec.ServerSentEventHttpMessageWriter;
import cn.taketoday.http.codec.json.Jackson2JsonDecoder;
import cn.taketoday.http.codec.json.Jackson2JsonEncoder;
import cn.taketoday.http.codec.json.Jackson2SmileDecoder;
import cn.taketoday.http.codec.json.Jackson2SmileEncoder;
import cn.taketoday.http.codec.multipart.DefaultPartHttpMessageReader;
import cn.taketoday.http.codec.multipart.MultipartHttpMessageReader;
import cn.taketoday.http.codec.multipart.PartHttpMessageWriter;
import cn.taketoday.http.codec.protobuf.ProtobufDecoder;
import cn.taketoday.http.codec.protobuf.ProtobufHttpMessageWriter;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import static cn.taketoday.core.ResolvableType.fromClass;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ServerCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 */
public class ServerCodecConfigurerTests {

  private final ServerCodecConfigurer configurer = new DefaultServerCodecConfigurer();

  private final AtomicInteger index = new AtomicInteger();

  @Test
  public void defaultReaders() {
    List<HttpMessageReader<?>> readers = this.configurer.getReaders();
    assertThat(readers.size()).isEqualTo(15);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteArrayDecoder.class);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteBufferDecoder.class);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(DataBufferDecoder.class);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(NettyByteBufDecoder.class);
    assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageReader.class);
    assertStringDecoder(getNextDecoder(readers), true);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(ProtobufDecoder.class);
    assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(FormHttpMessageReader.class);
    assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(DefaultPartHttpMessageReader.class);
    assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(MultipartHttpMessageReader.class);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2JsonDecoder.class);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2SmileDecoder.class);
    assertStringDecoder(getNextDecoder(readers), false);
  }

  @Test
  public void defaultWriters() {
    List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
    assertThat(writers.size()).isEqualTo(14);
    assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteArrayEncoder.class);
    assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteBufferEncoder.class);
    assertThat(getNextEncoder(writers).getClass()).isEqualTo(DataBufferEncoder.class);
    assertThat(getNextEncoder(writers).getClass()).isEqualTo(NettyByteBufEncoder.class);
    assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageWriter.class);
    assertStringEncoder(getNextEncoder(writers), true);
    assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ProtobufHttpMessageWriter.class);
    assertThat(writers.get(this.index.getAndIncrement()).getClass()).isEqualTo(PartHttpMessageWriter.class);
    assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2JsonEncoder.class);
    assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2SmileEncoder.class);
    assertSseWriter(writers);
    assertStringEncoder(getNextEncoder(writers), false);
  }

  @Test
  public void jackson2EncoderOverride() {
    Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();
    Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
    this.configurer.defaultCodecs().jackson2JsonDecoder(decoder);
    this.configurer.defaultCodecs().jackson2JsonEncoder(encoder);

    List<HttpMessageReader<?>> readers = this.configurer.getReaders();
    Jackson2JsonDecoder actualDecoder = findCodec(readers, Jackson2JsonDecoder.class);
    assertThat(actualDecoder).isSameAs(decoder);

    List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
    Jackson2JsonEncoder actualEncoder = findCodec(writers, Jackson2JsonEncoder.class);
    assertThat(actualEncoder).isSameAs(encoder);
    assertThat(findCodec(writers, ServerSentEventHttpMessageWriter.class).getEncoder()).isSameAs(encoder);
  }

  @Test
  public void maxInMemorySize() {
    int size = 99;
    this.configurer.defaultCodecs().maxInMemorySize(size);

    List<HttpMessageReader<?>> readers = this.configurer.getReaders();
    assertThat(((ByteArrayDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((ByteBufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((DataBufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((NettyByteBufDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((ResourceDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((StringDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((ProtobufDecoder) getNextDecoder(readers)).getMaxMessageSize()).isEqualTo(size);
    assertThat(((FormHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((DefaultPartHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);

    MultipartHttpMessageReader multipartReader = (MultipartHttpMessageReader) nextReader(readers);
    DefaultPartHttpMessageReader reader = (DefaultPartHttpMessageReader) multipartReader.getPartReader();
    assertThat((reader).getMaxInMemorySize()).isEqualTo(size);

    assertThat(((Jackson2JsonDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((Jackson2SmileDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((StringDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
  }

  @Test
  public void maxInMemorySizeWithCustomCodecs() {

    int size = 99;
    this.configurer.defaultCodecs().maxInMemorySize(size);
    this.configurer.registerDefaults(false);

    CodecConfigurer.CustomCodecs customCodecs = this.configurer.customCodecs();
    customCodecs.register(new ByteArrayDecoder());
    customCodecs.registerWithDefaultConfig(new ByteArrayDecoder());
    customCodecs.register(new Jackson2JsonDecoder());
    customCodecs.registerWithDefaultConfig(new Jackson2JsonDecoder());

    this.configurer.defaultCodecs().enableLoggingRequestDetails(true);

    List<HttpMessageReader<?>> readers = this.configurer.getReaders();
    assertThat(((ByteArrayDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(256 * 1024);
    assertThat(((ByteArrayDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((Jackson2JsonDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(256 * 1024);
    assertThat(((Jackson2JsonDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
  }

  @Test
  public void enableRequestLoggingDetails() {
    this.configurer.defaultCodecs().enableLoggingRequestDetails(true);

    List<HttpMessageReader<?>> readers = this.configurer.getReaders();
    assertThat(findCodec(readers, FormHttpMessageReader.class).isEnableLoggingRequestDetails()).isTrue();

    MultipartHttpMessageReader multipartReader = findCodec(readers, MultipartHttpMessageReader.class);
    assertThat(multipartReader.isEnableLoggingRequestDetails()).isTrue();

    DefaultPartHttpMessageReader reader = (DefaultPartHttpMessageReader) multipartReader.getPartReader();
    assertThat(reader.isEnableLoggingRequestDetails()).isTrue();
  }

  @Test
  public void enableRequestLoggingDetailsWithCustomCodecs() {

    this.configurer.registerDefaults(false);
    this.configurer.defaultCodecs().enableLoggingRequestDetails(true);

    CodecConfigurer.CustomCodecs customCodecs = this.configurer.customCodecs();
    customCodecs.register(new FormHttpMessageReader());
    customCodecs.registerWithDefaultConfig(new FormHttpMessageReader());

    List<HttpMessageReader<?>> readers = this.configurer.getReaders();
    assertThat(((FormHttpMessageReader) readers.get(0)).isEnableLoggingRequestDetails()).isFalse();
    assertThat(((FormHttpMessageReader) readers.get(1)).isEnableLoggingRequestDetails()).isTrue();
  }

  @Test
  public void cloneConfigurer() {
    ServerCodecConfigurer clone = this.configurer.clone();

    MultipartHttpMessageReader reader = new MultipartHttpMessageReader(new DefaultPartHttpMessageReader());
    Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
    clone.defaultCodecs().multipartReader(reader);
    clone.defaultCodecs().serverSentEventEncoder(encoder);

    // Clone has the customizations

    HttpMessageReader<?> actualReader =
            findCodec(clone.getReaders(), MultipartHttpMessageReader.class);

    ServerSentEventHttpMessageWriter actualWriter =
            findCodec(clone.getWriters(), ServerSentEventHttpMessageWriter.class);

    assertThat(actualReader).isSameAs(reader);
    assertThat(actualWriter.getEncoder()).isSameAs(encoder);

    // Original does not have the customizations

    actualReader = findCodec(this.configurer.getReaders(), MultipartHttpMessageReader.class);
    actualWriter = findCodec(this.configurer.getWriters(), ServerSentEventHttpMessageWriter.class);

    assertThat(actualReader).isNotSameAs(reader);
    assertThat(actualWriter.getEncoder()).isNotSameAs(encoder);
  }

  private Decoder<?> getNextDecoder(List<HttpMessageReader<?>> readers) {
    HttpMessageReader<?> reader = nextReader(readers);
    assertThat(reader).isInstanceOf(DecoderHttpMessageReader.class);
    return ((DecoderHttpMessageReader<?>) reader).getDecoder();
  }

  private HttpMessageReader<?> nextReader(List<HttpMessageReader<?>> readers) {
    return readers.get(this.index.getAndIncrement());
  }

  private Encoder<?> getNextEncoder(List<HttpMessageWriter<?>> writers) {
    HttpMessageWriter<?> writer = writers.get(this.index.getAndIncrement());
    assertThat(writer.getClass()).isEqualTo(EncoderHttpMessageWriter.class);
    return ((EncoderHttpMessageWriter<?>) writer).getEncoder();
  }

  @SuppressWarnings("unchecked")
  private <T> T findCodec(List<?> codecs, Class<T> type) {
    return (T) codecs.stream()
            .map(c -> {
              if (c instanceof EncoderHttpMessageWriter) {
                return ((EncoderHttpMessageWriter<?>) c).getEncoder();
              }
              else if (c instanceof DecoderHttpMessageReader) {
                return ((DecoderHttpMessageReader<?>) c).getDecoder();
              }
              else {
                return c;
              }
            })
            .filter(type::isInstance).findFirst().get();
  }

  @SuppressWarnings("unchecked")
  private void assertStringDecoder(Decoder<?> decoder, boolean textOnly) {
    assertThat(decoder.getClass()).isEqualTo(StringDecoder.class);
    assertThat(decoder.canDecode(fromClass(String.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
    Object expected = !textOnly;
    assertThat(decoder.canDecode(fromClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);

    byte[] bytes = "line1\nline2".getBytes(StandardCharsets.UTF_8);
    Flux<String> flux = (Flux<String>) decoder.decode(
            Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes)),
            fromClass(String.class), MimeTypeUtils.TEXT_PLAIN, Collections.emptyMap());

    assertThat(flux.collectList().block(Duration.ZERO)).isEqualTo(Arrays.asList("line1", "line2"));
  }

  private void assertStringEncoder(Encoder<?> encoder, boolean textOnly) {
    assertThat(encoder.getClass()).isEqualTo(CharSequenceEncoder.class);
    assertThat(encoder.canEncode(fromClass(String.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
    Object expected = !textOnly;
    assertThat(encoder.canEncode(fromClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);
  }

  private void assertSseWriter(List<HttpMessageWriter<?>> writers) {
    HttpMessageWriter<?> writer = writers.get(this.index.getAndIncrement());
    assertThat(writer.getClass()).isEqualTo(ServerSentEventHttpMessageWriter.class);
    Encoder<?> encoder = ((ServerSentEventHttpMessageWriter) writer).getEncoder();
    assertThat(encoder).isNotNull();
    assertThat(encoder.getClass()).isEqualTo(Jackson2JsonEncoder.class);
  }

}
