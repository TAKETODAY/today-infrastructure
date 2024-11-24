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

package infra.http.codec.support;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import infra.core.codec.ByteArrayDecoder;
import infra.core.codec.ByteArrayEncoder;
import infra.core.codec.ByteBufferDecoder;
import infra.core.codec.ByteBufferEncoder;
import infra.core.codec.CharSequenceEncoder;
import infra.core.codec.DataBufferDecoder;
import infra.core.codec.DataBufferEncoder;
import infra.core.codec.Decoder;
import infra.core.codec.Encoder;
import infra.core.codec.Netty5BufferDecoder;
import infra.core.codec.Netty5BufferEncoder;
import infra.core.codec.NettyByteBufDecoder;
import infra.core.codec.NettyByteBufEncoder;
import infra.core.codec.ResourceDecoder;
import infra.core.codec.StringDecoder;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.MediaType;
import infra.http.codec.ClientCodecConfigurer;
import infra.http.codec.DecoderHttpMessageReader;
import infra.http.codec.EncoderHttpMessageWriter;
import infra.http.codec.FormHttpMessageReader;
import infra.http.codec.FormHttpMessageWriter;
import infra.http.codec.HttpMessageReader;
import infra.http.codec.HttpMessageWriter;
import infra.http.codec.ResourceHttpMessageReader;
import infra.http.codec.ResourceHttpMessageWriter;
import infra.http.codec.ServerSentEventHttpMessageReader;
import infra.http.codec.json.Jackson2CodecSupport;
import infra.http.codec.json.Jackson2JsonDecoder;
import infra.http.codec.json.Jackson2JsonEncoder;
import infra.http.codec.json.Jackson2SmileDecoder;
import infra.http.codec.json.Jackson2SmileEncoder;
import infra.http.codec.multipart.DefaultPartHttpMessageReader;
import infra.http.codec.multipart.MultipartHttpMessageReader;
import infra.http.codec.multipart.MultipartHttpMessageWriter;
import infra.http.codec.multipart.PartEventHttpMessageReader;
import infra.http.codec.multipart.PartEventHttpMessageWriter;
import infra.http.codec.multipart.PartHttpMessageWriter;
import infra.http.codec.protobuf.ProtobufDecoder;
import infra.http.codec.protobuf.ProtobufHttpMessageWriter;
import infra.util.MimeType;
import reactor.core.publisher.Flux;

import static infra.core.ResolvableType.forClass;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ClientCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 */
public class ClientCodecConfigurerTests {

  private final ClientCodecConfigurer configurer = new DefaultClientCodecConfigurer();

  private final AtomicInteger index = new AtomicInteger();

  @Test
  public void defaultReaders() {
    List<HttpMessageReader<?>> readers = this.configurer.getReaders();
    assertThat(readers.size()).isEqualTo(16);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteArrayDecoder.class);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteBufferDecoder.class);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(DataBufferDecoder.class);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(NettyByteBufDecoder.class);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(Netty5BufferDecoder.class);

    assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageReader.class);
    assertStringDecoder(getNextDecoder(readers), true);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(ProtobufDecoder.class);
    // SPR-16804
    assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(FormHttpMessageReader.class);
    assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(DefaultPartHttpMessageReader.class);
    assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(MultipartHttpMessageReader.class);
    assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(PartEventHttpMessageReader.class);

    assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2JsonDecoder.class);
    assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2SmileDecoder.class);
    assertSseReader(readers);
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
    assertThat(getNextEncoder(writers).getClass()).isEqualTo(Netty5BufferEncoder.class);

    assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageWriter.class);
    assertStringEncoder(getNextEncoder(writers), true);
    assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ProtobufHttpMessageWriter.class);
    assertThat(writers.get(this.index.getAndIncrement()).getClass()).isEqualTo(MultipartHttpMessageWriter.class);
    assertThat(writers.get(this.index.getAndIncrement()).getClass()).isEqualTo(PartEventHttpMessageWriter.class);
    assertThat(writers.get(this.index.getAndIncrement()).getClass()).isEqualTo(PartHttpMessageWriter.class);

    assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2JsonEncoder.class);
    assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2SmileEncoder.class);
    assertStringEncoder(getNextEncoder(writers), false);
  }

  @Test
  public void jackson2CodecCustomization() {
    Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();
    Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
    this.configurer.defaultCodecs().jackson2JsonDecoder(decoder);
    this.configurer.defaultCodecs().jackson2JsonEncoder(encoder);

    List<HttpMessageReader<?>> readers = this.configurer.getReaders();
    Jackson2JsonDecoder actualDecoder = findCodec(readers, Jackson2JsonDecoder.class);
    assertThat(actualDecoder).isSameAs(decoder);
    assertThat(findCodec(readers, ServerSentEventHttpMessageReader.class).getDecoder()).isSameAs(decoder);

    List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
    Jackson2JsonEncoder actualEncoder = findCodec(writers, Jackson2JsonEncoder.class);
    assertThat(actualEncoder).isSameAs(encoder);

    MultipartHttpMessageWriter multipartWriter = findCodec(writers, MultipartHttpMessageWriter.class);
    actualEncoder = findCodec(multipartWriter.getPartWriters(), Jackson2JsonEncoder.class);
    assertThat(actualEncoder).isSameAs(encoder);
  }

  @Test
  public void objectMapperCustomization() {
    ObjectMapper objectMapper = new ObjectMapper();
    this.configurer.defaultCodecs().configureDefaultCodec(codec -> {
      if (codec instanceof Jackson2CodecSupport) {
        ((Jackson2CodecSupport) codec).setObjectMapper(objectMapper);
      }
    });

    List<HttpMessageReader<?>> readers = this.configurer.getReaders();
    Jackson2JsonDecoder actualDecoder = findCodec(readers, Jackson2JsonDecoder.class);
    assertThat(actualDecoder.getObjectMapper()).isSameAs(objectMapper);

    List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
    Jackson2JsonEncoder actualEncoder = findCodec(writers, Jackson2JsonEncoder.class);
    assertThat(actualEncoder.getObjectMapper()).isSameAs(objectMapper);

    MultipartHttpMessageWriter multipartWriter = findCodec(writers, MultipartHttpMessageWriter.class);
    actualEncoder = findCodec(multipartWriter.getPartWriters(), Jackson2JsonEncoder.class);
    assertThat(actualEncoder.getObjectMapper()).isSameAs(objectMapper);
  }

  @Test
  public void maxInMemorySize() {
    int size = 99;
    this.configurer.defaultCodecs().maxInMemorySize(size);
    List<HttpMessageReader<?>> readers = this.configurer.getReaders();
    assertThat(readers.size()).isEqualTo(16);
    assertThat(((ByteArrayDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((ByteBufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((DataBufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((NettyByteBufDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((Netty5BufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);

    assertThat(((ResourceDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((StringDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((ProtobufDecoder) getNextDecoder(readers)).getMaxMessageSize()).isEqualTo(size);
    assertThat(((FormHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);

    assertThat(((DefaultPartHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);
    nextReader(readers);
    assertThat(((PartEventHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);

    assertThat(((Jackson2JsonDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
    assertThat(((Jackson2SmileDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);

    ServerSentEventHttpMessageReader reader = (ServerSentEventHttpMessageReader) nextReader(readers);
    assertThat(reader.getMaxInMemorySize()).isEqualTo(size);
    assertThat(((Jackson2JsonDecoder) reader.getDecoder()).getMaxInMemorySize()).isEqualTo(size);

    assertThat(((StringDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
  }

  @Test
  public void enableLoggingRequestDetails() {
    this.configurer.defaultCodecs().enableLoggingRequestDetails(true);

    List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
    MultipartHttpMessageWriter multipartWriter = findCodec(writers, MultipartHttpMessageWriter.class);
    assertThat(multipartWriter.isEnableLoggingRequestDetails()).isTrue();

    FormHttpMessageWriter formWriter = (FormHttpMessageWriter) multipartWriter.getFormWriter();
    assertThat(formWriter).isNotNull();
    assertThat(formWriter.isEnableLoggingRequestDetails()).isTrue();
  }

  @Test
  public void clonedConfigurer() {
    ClientCodecConfigurer clone = this.configurer.clone();

    Jackson2JsonDecoder jackson2Decoder = new Jackson2JsonDecoder();
    clone.defaultCodecs().serverSentEventDecoder(jackson2Decoder);
    clone.defaultCodecs().multipartCodecs().encoder(new Jackson2SmileEncoder());
    clone.defaultCodecs().multipartCodecs().writer(new ResourceHttpMessageWriter());

    // Clone has the customizations

    Decoder<?> sseDecoder = findCodec(clone.getReaders(), ServerSentEventHttpMessageReader.class).getDecoder();
    List<HttpMessageWriter<?>> writers = findCodec(clone.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

    assertThat(sseDecoder).isSameAs(jackson2Decoder);
    assertThat(writers).hasSize(2);

    // Original does not have the customizations

    sseDecoder = findCodec(this.configurer.getReaders(), ServerSentEventHttpMessageReader.class).getDecoder();
    writers = findCodec(this.configurer.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

    assertThat(sseDecoder).isNotSameAs(jackson2Decoder);
    assertThat(writers).hasSize(14);
  }

  @Test // gh-24194
  public void cloneShouldNotDropMultipartCodecs() {

    ClientCodecConfigurer clone = this.configurer.clone();
    List<HttpMessageWriter<?>> writers =
            findCodec(clone.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

    assertThat(writers).hasSize(14);
  }

  @Test
  public void cloneShouldNotBeImpactedByChangesToOriginal() {

    ClientCodecConfigurer clone = this.configurer.clone();

    this.configurer.registerDefaults(false);
    this.configurer.customCodecs().register(new Jackson2JsonEncoder());

    List<HttpMessageWriter<?>> writers =
            findCodec(clone.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

    assertThat(writers).hasSize(14);
  }

  private Decoder<?> getNextDecoder(List<HttpMessageReader<?>> readers) {
    HttpMessageReader<?> reader = readers.get(this.index.getAndIncrement());
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
    assertThat(decoder.canDecode(forClass(String.class), MimeType.TEXT_PLAIN)).isTrue();
    Object expected = !textOnly;
    assertThat(decoder.canDecode(forClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);

    byte[] bytes = "line1\nline2".getBytes(StandardCharsets.UTF_8);
    Flux<String> decoded = (Flux<String>) decoder.decode(
            Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes)),
            forClass(String.class), MimeType.TEXT_PLAIN, Collections.emptyMap());

    assertThat(decoded.collectList().block(Duration.ZERO)).isEqualTo(Arrays.asList("line1", "line2"));
  }

  private void assertStringEncoder(Encoder<?> encoder, boolean textOnly) {
    assertThat(encoder.getClass()).isEqualTo(CharSequenceEncoder.class);
    assertThat(encoder.canEncode(forClass(String.class), MimeType.TEXT_PLAIN)).isTrue();
    Object expected = !textOnly;
    assertThat(encoder.canEncode(forClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);
  }

  private void assertSseReader(List<HttpMessageReader<?>> readers) {
    HttpMessageReader<?> reader = readers.get(this.index.getAndIncrement());
    assertThat(reader.getClass()).isEqualTo(ServerSentEventHttpMessageReader.class);
    Decoder<?> decoder = ((ServerSentEventHttpMessageReader) reader).getDecoder();
    assertThat(decoder).isNotNull();
    assertThat(decoder.getClass()).isEqualTo(Jackson2JsonDecoder.class);
  }

}
