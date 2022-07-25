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

import com.google.protobuf.Message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.CharSequenceEncoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.http.client.MultipartBodyBuilder;
import cn.taketoday.http.codec.json.Jackson2JsonEncoder;
import cn.taketoday.http.codec.json.LeakAwareDataBufferFactory;
import cn.taketoday.http.codec.multipart.MultipartHttpMessageWriter;
import cn.taketoday.http.codec.protobuf.ProtobufDecoder;
import cn.taketoday.http.codec.protobuf.ProtobufEncoder;
import cn.taketoday.protobuf.Msg;
import cn.taketoday.protobuf.SecondMsg;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Test scenarios for data buffer leaks.
 *
 * @author Rossen Stoyanchev
 */
public class CancelWithoutDemandCodecTests {

  private final LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();

  @AfterEach
  public void tearDown() throws Exception {
    this.bufferFactory.checkForLeaks();
  }

  @Test // gh-22107
  public void cancelWithEncoderHttpMessageWriterAndSingleValue() {
    CharSequenceEncoder encoder = CharSequenceEncoder.allMimeTypes();
    HttpMessageWriter<CharSequence> writer = new EncoderHttpMessageWriter<>(encoder);
    CancellingOutputMessage outputMessage = new CancellingOutputMessage(this.bufferFactory);

    writer.write(Mono.just("foo"), ResolvableType.fromType(String.class), MediaType.TEXT_PLAIN,
            outputMessage, Collections.emptyMap()).block(Duration.ofSeconds(5));
  }

  @Test // gh-22107
  public void cancelWithJackson() {
    Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();

    Flux<DataBuffer> flux = encoder.encode(Flux.just(new Pojo("foofoo", "barbar"), new Pojo("bar", "baz")),
            this.bufferFactory, ResolvableType.fromClass(Pojo.class),
            MediaType.APPLICATION_JSON, Collections.emptyMap());

    BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
    flux.subscribe(subscriber); // Assume sync execution (e.g. encoding with Flux.just)..
    subscriber.cancel();
  }

  @Test // gh-22543
  public void cancelWithProtobufEncoder() {
    ProtobufEncoder encoder = new ProtobufEncoder();
    Msg msg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

    Flux<DataBuffer> flux = encoder.encode(Mono.just(msg),
            this.bufferFactory, ResolvableType.fromClass(Msg.class),
            new MimeType("application", "x-protobuf"), Collections.emptyMap());

    BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
    flux.subscribe(subscriber); // Assume sync execution (e.g. encoding with Flux.just)..
    subscriber.cancel();
  }

  @Test // gh-22731
  public void cancelWithProtobufDecoder() throws InterruptedException {
    ProtobufDecoder decoder = new ProtobufDecoder();

    Mono<DataBuffer> input = Mono.fromCallable(() -> {
      Msg msg = Msg.newBuilder().setFoo("Foo").build();
      byte[] bytes = msg.toByteArray();
      DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
      buffer.write(bytes);
      return buffer;
    });

    Flux<Message> messages = decoder.decode(input, ResolvableType.fromType(Msg.class),
            new MimeType("application", "x-protobuf"), Collections.emptyMap());
    ZeroDemandMessageSubscriber subscriber = new ZeroDemandMessageSubscriber();
    messages.subscribe(subscriber);
    subscriber.cancel();
  }

  @Test // gh-22107
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

  @Test // gh-22107
  public void cancelWithSse() {
    ServerSentEvent<?> event = ServerSentEvent.builder().data("bar").id("c42").event("foo").build();
    ServerSentEventHttpMessageWriter writer = new ServerSentEventHttpMessageWriter(new Jackson2JsonEncoder());
    CancellingOutputMessage outputMessage = new CancellingOutputMessage(this.bufferFactory);

    writer.write(Mono.just(event), ResolvableType.fromClass(ServerSentEvent.class), MediaType.TEXT_EVENT_STREAM,
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
      flux.subscribe(subscriber); // Assume sync execution (e.g. encoding with Flux.just)..
      subscriber.cancel();
      return Mono.empty();
    }

    @Override
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
      Flux<? extends DataBuffer> flux = Flux.from(body).concatMap(Flux::from);
      BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
      flux.subscribe(subscriber); // Assume sync execution (e.g. encoding with Flux.just)..
      subscriber.cancel();
      return Mono.empty();
    }

    @Override
    public Mono<Void> setComplete() {
      throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders getHeaders() {
      return HttpHeaders.create();
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
