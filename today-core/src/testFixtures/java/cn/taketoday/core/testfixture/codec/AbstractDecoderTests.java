/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.testfixture.codec;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import io.netty5.buffer.Buffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Abstract base class for {@link Decoder} unit tests. Subclasses need to implement
 * {@link #canDecode()}, {@link #decode()} and {@link #decodeToMono()}, possibly using the wide
 * variety of helper methods like {@link #testDecodeAll} or {@link #testDecodeToMonoAll}.
 *
 * @author Arjen Poutsma
 */
public abstract class AbstractDecoderTests<D extends Decoder<?>> extends AbstractLeakCheckingTests {

  /**
   * The decoder to test.
   */
  protected D decoder;

  /**
   * Construct a new {@code AbstractDecoderTests} instance for the given decoder.
   *
   * @param decoder the decoder
   */
  protected AbstractDecoderTests(D decoder) {
    Assert.notNull(decoder, "Encoder must not be null");

    this.decoder = decoder;
  }

  /**
   * Subclasses should implement this method to test {@link Decoder#canDecode}.
   */
  @Test
  public abstract void canDecode() throws Exception;

  /**
   * Subclasses should implement this method to test {@link Decoder#decode}, possibly using
   * {@link #testDecodeAll} or other helper methods.
   */
  @Test
  public abstract void decode() throws Exception;

  /**
   * Subclasses should implement this method to test {@link Decoder#decodeToMono}, possibly using
   * {@link #testDecodeToMonoAll}.
   */
  @Test
  public abstract void decodeToMono() throws Exception;

  // Flux

  /**
   * Helper method that tests for a variety of {@link Flux} decoding scenarios. This method
   * invokes:
   * <ul>
   *     <li>{@link #testDecode(Publisher, ResolvableType, Consumer, MimeType, Map)}</li>
   *     <li>{@link #testDecodeError(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testDecodeCancel(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testDecodeEmpty(ResolvableType, MimeType, Map)}</li>
   * </ul>
   *
   * @param input the input to be provided to the decoder
   * @param outputClass the desired output class
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   * @param <T> the output type
   */
  protected <T> void testDecodeAll(Publisher<DataBuffer> input, Class<? extends T> outputClass,
          Consumer<StepVerifier.FirstStep<T>> stepConsumer) {

    testDecodeAll(input, ResolvableType.fromClass(outputClass), stepConsumer, null, null);
  }

  /**
   * Helper method that tests for a variety of {@link Flux} decoding scenarios. This method
   * invokes:
   * <ul>
   *     <li>{@link #testDecode(Publisher, ResolvableType, Consumer, MimeType, Map)}</li>
   *     <li>{@link #testDecodeError(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testDecodeCancel(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testDecodeEmpty(ResolvableType, MimeType, Map)}</li>
   * </ul>
   *
   * @param input the input to be provided to the decoder
   * @param outputType the desired output type
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   * @param <T> the output type
   */
  protected <T> void testDecodeAll(Publisher<DataBuffer> input, ResolvableType outputType,
          Consumer<StepVerifier.FirstStep<T>> stepConsumer,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    testDecode(input, outputType, stepConsumer, mimeType, hints);
    testDecodeError(input, outputType, mimeType, hints);
    testDecodeCancel(input, outputType, mimeType, hints);
    testDecodeEmpty(outputType, mimeType, hints);
  }

  /**
   * Test a standard {@link Decoder#decode decode} scenario. For example:
   * <pre class="code">
   * byte[] bytes1 = ...
   * byte[] bytes2 = ...
   *
   * Flux&lt;DataBuffer&gt; input = Flux.concat(
   *   dataBuffer(bytes1),
   *   dataBuffer(bytes2));
   *
   * testDecodeAll(input, byte[].class, step -&gt; step
   *   .consumeNextWith(expectBytes(bytes1))
   *   .consumeNextWith(expectBytes(bytes2))
   * 	 .verifyComplete());
   * </pre>
   *
   * @param input the input to be provided to the decoder
   * @param outputClass the desired output class
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   * @param <T> the output type
   */
  protected <T> void testDecode(Publisher<DataBuffer> input, Class<? extends T> outputClass,
          Consumer<StepVerifier.FirstStep<T>> stepConsumer) {

    testDecode(input, ResolvableType.fromClass(outputClass), stepConsumer, null, null);
  }

  /**
   * Test a standard {@link Decoder#decode decode} scenario. For example:
   * <pre class="code">
   * byte[] bytes1 = ...
   * byte[] bytes2 = ...
   *
   * Flux&lt;DataBuffer&gt; input = Flux.concat(
   *   dataBuffer(bytes1),
   *   dataBuffer(bytes2));
   *
   * testDecodeAll(input, byte[].class, step -&gt; step
   *   .consumeNextWith(expectBytes(bytes1))
   *   .consumeNextWith(expectBytes(bytes2))
   * 	 .verifyComplete());
   * </pre>
   *
   * @param input the input to be provided to the decoder
   * @param outputType the desired output type
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   * @param <T> the output type
   */
  @SuppressWarnings("unchecked")
  protected <T> void testDecode(Publisher<DataBuffer> input, ResolvableType outputType,
          Consumer<StepVerifier.FirstStep<T>> stepConsumer,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Flux<T> result = (Flux<T>) this.decoder.decode(input, outputType, mimeType, hints);
    StepVerifier.FirstStep<T> step = StepVerifier.create(result);
    stepConsumer.accept(step);
  }

  /**
   * Test a {@link Decoder#decode decode} scenario where the input stream contains an error.
   * This test method will feed the first element of the {@code input} stream to the decoder,
   * followed by an {@link InputException}.
   * The result is expected to contain one "normal" element, followed by the error.
   *
   * @param input the input to be provided to the decoder
   * @param outputType the desired output type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   * @see InputException
   */
  protected void testDecodeError(Publisher<DataBuffer> input, ResolvableType outputType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Flux<DataBuffer> flux = Mono.from(input).concatWith(Flux.error(new InputException()));
    Assertions.assertThatExceptionOfType(InputException.class).isThrownBy(() ->
            this.decoder.decode(flux, outputType, mimeType, hints)
                    .doOnNext(object -> {
                      if (object instanceof Buffer buffer) {
                        buffer.close();
                      }
                    })
                    .blockLast(Duration.ofSeconds(5)));
  }

  /**
   * Test a {@link Decoder#decode decode} scenario where the input stream is canceled.
   * This test method will feed the first element of the {@code input} stream to the decoder,
   * followed by a cancel signal.
   * The result is expected to contain one "normal" element.
   *
   * @param input the input to be provided to the decoder
   * @param outputType the desired output type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   */
  protected void testDecodeCancel(Publisher<DataBuffer> input, ResolvableType outputType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Flux<?> result = this.decoder.decode(input, outputType, mimeType, hints)
            .doOnNext(object -> {
              if (object instanceof Buffer buffer) {
                buffer.close();
              }
            });
    StepVerifier.create(result).expectNextCount(1).thenCancel().verify();
  }

  /**
   * Test a {@link Decoder#decode decode} scenario where the input stream is empty.
   * The output is expected to be empty as well.
   *
   * @param outputType the desired output type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   */
  protected void testDecodeEmpty(ResolvableType outputType, @Nullable MimeType mimeType,
          @Nullable Map<String, Object> hints) {

    Flux<DataBuffer> input = Flux.empty();
    Flux<?> result = this.decoder.decode(input, outputType, mimeType, hints);
    StepVerifier.create(result).verifyComplete();
  }

  // Mono

  /**
   * Helper method that tests for a variety of {@link Mono} decoding scenarios. This method
   * invokes:
   * <ul>
   *     <li>{@link #testDecodeToMono(Publisher, ResolvableType, Consumer, MimeType, Map)}</li>
   *     <li>{@link #testDecodeToMonoError(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testDecodeToMonoCancel(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testDecodeToMonoEmpty(ResolvableType, MimeType, Map)}</li>
   * </ul>
   *
   * @param input the input to be provided to the decoder
   * @param outputClass the desired output class
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   * @param <T> the output type
   */
  protected <T> void testDecodeToMonoAll(Publisher<DataBuffer> input,
          Class<? extends T> outputClass, Consumer<StepVerifier.FirstStep<T>> stepConsumer) {

    testDecodeToMonoAll(input, ResolvableType.fromClass(outputClass), stepConsumer, null, null);
  }

  /**
   * Helper method that tests for a variety of {@link Mono} decoding scenarios. This method
   * invokes:
   * <ul>
   *     <li>{@link #testDecodeToMono(Publisher, ResolvableType, Consumer, MimeType, Map)}</li>
   *     <li>{@link #testDecodeToMonoError(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testDecodeToMonoCancel(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testDecodeToMonoEmpty(ResolvableType, MimeType, Map)}</li>
   * </ul>
   *
   * @param input the input to be provided to the decoder
   * @param outputType the desired output type
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   * @param <T> the output type
   */
  protected <T> void testDecodeToMonoAll(Publisher<DataBuffer> input, ResolvableType outputType,
          Consumer<StepVerifier.FirstStep<T>> stepConsumer,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    testDecodeToMono(input, outputType, stepConsumer, mimeType, hints);
    testDecodeToMonoError(input, outputType, mimeType, hints);
    testDecodeToMonoCancel(input, outputType, mimeType, hints);
    testDecodeToMonoEmpty(outputType, mimeType, hints);
  }

  /**
   * Test a standard {@link Decoder#decodeToMono decode} scenario. For example:
   * <pre class="code">
   * byte[] bytes1 = ...
   * byte[] bytes2 = ...
   * byte[] allBytes = ... // bytes1 + bytes2
   *
   * Flux&lt;DataBuffer&gt; input = Flux.concat(
   *   dataBuffer(bytes1),
   *   dataBuffer(bytes2));
   *
   * testDecodeAll(input, byte[].class, step -&gt; step
   *   .consumeNextWith(expectBytes(allBytes))
   * 	 .verifyComplete());
   * </pre>
   *
   * @param input the input to be provided to the decoder
   * @param outputClass the desired output class
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   * @param <T> the output type
   */
  protected <T> void testDecodeToMono(Publisher<DataBuffer> input,
          Class<? extends T> outputClass, Consumer<StepVerifier.FirstStep<T>> stepConsumer) {

    testDecodeToMono(input, ResolvableType.fromClass(outputClass), stepConsumer, null, null);
  }

  /**
   * Test a standard {@link Decoder#decodeToMono decode} scenario. For example:
   * <pre class="code">
   * byte[] bytes1 = ...
   * byte[] bytes2 = ...
   * byte[] allBytes = ... // bytes1 + bytes2
   *
   * Flux&lt;DataBuffer&gt; input = Flux.concat(
   *   dataBuffer(bytes1),
   *   dataBuffer(bytes2));
   *
   * testDecodeAll(input, byte[].class, step -&gt; step
   *   .consumeNextWith(expectBytes(allBytes))
   * 	 .verifyComplete());
   * </pre>
   *
   * @param input the input to be provided to the decoder
   * @param outputType the desired output type
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   * @param <T> the output type
   */
  @SuppressWarnings("unchecked")
  protected <T> void testDecodeToMono(Publisher<DataBuffer> input, ResolvableType outputType,
          Consumer<StepVerifier.FirstStep<T>> stepConsumer,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Mono<T> result = (Mono<T>) this.decoder.decodeToMono(input, outputType, mimeType, hints);
    StepVerifier.FirstStep<T> step = StepVerifier.create(result);
    stepConsumer.accept(step);
  }

  /**
   * Test a {@link Decoder#decodeToMono decode} scenario where the input stream contains an error.
   * This test method will feed the first element of the {@code input} stream to the decoder,
   * followed by an {@link InputException}.
   * The result is expected to contain the error.
   *
   * @param input the input to be provided to the decoder
   * @param outputType the desired output type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   * @see InputException
   */
  protected void testDecodeToMonoError(Publisher<DataBuffer> input, ResolvableType outputType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    input = Mono.from(input).concatWith(Flux.error(new InputException()));
    Mono<?> result = this.decoder.decodeToMono(input, outputType, mimeType, hints);
    StepVerifier.create(result).expectError(InputException.class).verify();
  }

  /**
   * Test a {@link Decoder#decodeToMono decode} scenario where the input stream is canceled.
   * This test method will immediately cancel the output stream.
   *
   * @param input the input to be provided to the decoder
   * @param outputType the desired output type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   */
  protected void testDecodeToMonoCancel(Publisher<DataBuffer> input, ResolvableType outputType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Mono<?> result = this.decoder.decodeToMono(input, outputType, mimeType, hints);
    StepVerifier.create(result).thenCancel().verify();
  }

  /**
   * Test a {@link Decoder#decodeToMono decode} scenario where the input stream is empty.
   * The output is expected to be empty as well.
   *
   * @param outputType the desired output type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   */
  protected void testDecodeToMonoEmpty(ResolvableType outputType, @Nullable MimeType mimeType,
          @Nullable Map<String, Object> hints) {

    Mono<?> result = this.decoder.decodeToMono(Flux.empty(), outputType, mimeType, hints);
    StepVerifier.create(result).verifyComplete();
  }

  /**
   * Creates a deferred {@link DataBuffer} containing the given bytes.
   *
   * @param bytes the bytes that are to be stored in the buffer
   * @return the deferred buffer
   */
  protected Mono<DataBuffer> dataBuffer(byte[] bytes) {
    return Mono.fromCallable(() -> {
      DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(bytes.length);
      dataBuffer.write(bytes);
      return dataBuffer;
    });
  }

  /**
   * Exception used in {@link #testDecodeError} and {@link #testDecodeToMonoError}
   */
  @SuppressWarnings("serial")
  public static class InputException extends RuntimeException { }

}
