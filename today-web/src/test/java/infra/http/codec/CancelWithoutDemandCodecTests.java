/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.codec;

import com.google.protobuf.Message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import infra.core.ResolvableType;
import infra.core.codec.CharSequenceEncoder;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.testfixture.io.buffer.LeakAwareDataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.ReactiveHttpOutputMessage;
import infra.http.client.MultipartBodyBuilder;
import infra.http.codec.json.JacksonJsonEncoder;
import infra.http.codec.multipart.MultipartHttpMessageWriter;
import infra.http.codec.protobuf.ProtobufDecoder;
import infra.http.codec.protobuf.ProtobufEncoder;
import infra.protobuf.Msg;
import infra.protobuf.SecondMsg;
import infra.util.MimeType;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Test scenarios for data buffer leaks.
 *
 * @author Rossen Stoyanchev
 */
class CancelWithoutDemandCodecTests {

  private final LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();

  @AfterEach
  void tearDown() {
    this.bufferFactory.checkForLeaks();
  }

  @Test
  public void cancelWithEncoderHttpMessageWriterAndSingleValue() {
    CharSequenceEncoder encoder = CharSequenceEncoder.allMimeTypes();
    HttpMessageWriter<CharSequence> writer = new EncoderHttpMessageWriter<>(encoder);
    CancellingOutputMessage outputMessage = new CancellingOutputMessage(this.bufferFactory);

    writer.write(Mono.just("foo"), ResolvableType.forType(String.class), MediaType.TEXT_PLAIN,
            outputMessage, Collections.emptyMap()).block(Duration.ofSeconds(5));
  }

  @Test
  public void cancelWithJackson() {
    JacksonJsonEncoder encoder = new JacksonJsonEncoder();

    Flux<DataBuffer> flux = encoder.encode(Flux.just(new Pojo("foofoo", "barbar"), new Pojo("bar", "baz")),
            this.bufferFactory, ResolvableType.forClass(Pojo.class),
            MediaType.APPLICATION_JSON, Collections.emptyMap());

    BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
    flux.subscribe(subscriber); // Assume sync execution (for example, encoding with Flux.just)
    subscriber.cancel();
  }

  @Test
  public void cancelWithProtobufEncoder() {
    ProtobufEncoder encoder = new ProtobufEncoder();
    Msg msg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

    Flux<DataBuffer> flux = encoder.encode(Mono.just(msg),
            this.bufferFactory, ResolvableType.forClass(Msg.class),
            MediaType.APPLICATION_PROTOBUF, Collections.emptyMap());

    BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
    flux.subscribe(subscriber); // Assume sync execution (for example, encoding with Flux.just)
    subscriber.cancel();
  }

  @Test
  public void cancelWithProtobufDecoder() {
    ProtobufDecoder decoder = new ProtobufDecoder();

    Mono<DataBuffer> input = Mono.fromCallable(() -> {
      Msg msg = Msg.newBuilder().setFoo("Foo").build();
      byte[] bytes = msg.toByteArray();
      DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
      buffer.write(bytes);
      return buffer;
    });

    Flux<Message> messages = decoder.decode(input, ResolvableType.forType(Msg.class),
            new MimeType("application", "x-protobuf"), Collections.emptyMap());
    ZeroDemandMessageSubscriber subscriber = new ZeroDemandMessageSubscriber();
    messages.subscribe(subscriber);
    subscriber.cancel();
  }

  @Test
  public void cancelWithMultipartContent() {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("part1", "value1");
    builder.part("part2", "value2");

    List<HttpMessageWriter<?>> writers = ClientCodecConfigurer.create().getWriters();
    MultipartHttpMessageWriter writer = new MultipartHttpMessageWriter(writers);
    CancellingOutputMessage outputMessage = new CancellingOutputMessage(this.bufferFactory);

    writer.write(Mono.just(builder.build()), null, MediaType.MULTIPART_FORM_DATA,
            outputMessage, Collections.emptyMap()).block(Duration.ofSeconds(5));
  }

  @Test
  public void cancelWithSse() {
    ServerSentEvent<?> event = ServerSentEvent.builder().data("bar").id("c42").event("foo").build();
    ServerSentEventHttpMessageWriter writer = new ServerSentEventHttpMessageWriter(new JacksonJsonEncoder());
    CancellingOutputMessage outputMessage = new CancellingOutputMessage(this.bufferFactory);

    writer.write(Mono.just(event), ResolvableType.forClass(ServerSentEvent.class), MediaType.TEXT_EVENT_STREAM,
            outputMessage, Collections.emptyMap()).block(Duration.ofSeconds(5));
  }

  private static class CancellingOutputMessage implements ReactiveHttpOutputMessage {

    private final DataBufferFactory bufferFactory;

    public CancellingOutputMessage(DataBufferFactory bufferFactory) {
      this.bufferFactory = bufferFactory;
    }

    @Override
    public DataBufferFactory bufferFactory() {
      return this.bufferFactory;
    }

    @Override
    public void beforeCommit(Supplier<? extends Mono<Void>> action) {
    }

    @Override
    public boolean isCommitted() {
      return false;
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
      Flux<? extends DataBuffer> flux = Flux.from(body);
      BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
      flux.subscribe(subscriber); // Assume sync execution (for example, encoding with Flux.just)
      subscriber.cancel();
      return Mono.empty();
    }

    @Override
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
      Flux<? extends DataBuffer> flux = Flux.from(body).concatMap(Flux::from);
      BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
      flux.subscribe(subscriber); // Assume sync execution (for example, encoding with Flux.just)
      subscriber.cancel();
      return Mono.empty();
    }

    @Override
    public Mono<Void> setComplete() {
      throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders getHeaders() {
      return HttpHeaders.forWritable();
    }
  }

  private static class ZeroDemandSubscriber extends BaseSubscriber<DataBuffer> {

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
      // Just subscribe without requesting
    }
  }

  private static class ZeroDemandMessageSubscriber extends BaseSubscriber<Message> {

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
      // Just subscribe without requesting
    }
  }

}
