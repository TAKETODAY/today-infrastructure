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

import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Encoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Abstract base class for {@link Encoder} unit tests. Subclasses need to implement
 * {@link #canEncode()} and {@link #encode()}, possibly using the wide variety of
 * helper methods like {@link #testEncodeAll}.
 *
 * @author Arjen Poutsma
 */
public abstract class AbstractEncoderTests<E extends Encoder<?>> extends AbstractLeakCheckingTests {

  /**
   * The encoder to test.
   */
  protected final E encoder;

  /**
   * Construct a new {@code AbstractEncoderTestCase} for the given parameters.
   *
   * @param encoder the encoder
   */
  protected AbstractEncoderTests(E encoder) {
    Assert.notNull(encoder, "Encoder must not be null");
    this.encoder = encoder;
  }

  /**
   * Subclasses should implement this method to test {@link Encoder#canEncode}.
   */
  @Test
  public abstract void canEncode() throws Exception;

  /**
   * Subclasses should implement this method to test {@link Encoder#encode}, possibly using
   * {@link #testEncodeAll} or other helper methods.
   */
  @Test
  public abstract void encode() throws Exception;

  /**
   * Helper method that tests for a variety of encoding scenarios. This method
   * invokes:
   * <ul>
   *     <li>{@link #testEncode(Publisher, ResolvableType, MimeType, Map, Consumer)}</li>
   *     <li>{@link #testEncodeError(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testEncodeCancel(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testEncodeEmpty(ResolvableType, MimeType, Map)}</li>
   * </ul>
   *
   * @param input the input to be provided to the encoder
   * @param inputClass the input class
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   * @param <T> the output type
   */
  protected <T> void testEncodeAll(Publisher<? extends T> input, Class<? extends T> inputClass,
          Consumer<StepVerifier.FirstStep<DataBuffer>> stepConsumer) {

    testEncodeAll(input, ResolvableType.fromClass(inputClass), null, null, stepConsumer);
  }

  /**
   * Helper method that tests for a variety of decoding scenarios. This method
   * invokes:
   * <ul>
   *     <li>{@link #testEncode(Publisher, ResolvableType, MimeType, Map, Consumer)}</li>
   *     <li>{@link #testEncodeError(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testEncodeCancel(Publisher, ResolvableType, MimeType, Map)}</li>
   *     <li>{@link #testEncodeEmpty(ResolvableType, MimeType, Map)}</li>
   * </ul>
   *
   * @param <T> the output type
   * @param input the input to be provided to the encoder
   * @param inputType the input type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   */
  protected <T> void testEncodeAll(Publisher<? extends T> input, ResolvableType inputType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints,
          Consumer<StepVerifier.FirstStep<DataBuffer>> stepConsumer) {

    testEncode(input, inputType, mimeType, hints, stepConsumer);
    testEncodeError(input, inputType, mimeType, hints);
    testEncodeCancel(input, inputType, mimeType, hints);
    testEncodeEmpty(inputType, mimeType, hints);
  }

  /**
   * Test a standard {@link Encoder#encode encode} scenario.
   *
   * @param input the input to be provided to the encoder
   * @param inputClass the input class
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   * @param <T> the output type
   */
  protected <T> void testEncode(Publisher<? extends T> input, Class<? extends T> inputClass,
          Consumer<StepVerifier.FirstStep<DataBuffer>> stepConsumer) {

    testEncode(input, ResolvableType.fromClass(inputClass), null, null, stepConsumer);
  }

  /**
   * Test a standard {@link Encoder#encode encode} scenario.
   *
   * @param <T> the output type
   * @param input the input to be provided to the encoder
   * @param inputType the input type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   * @param stepConsumer a consumer to {@linkplain StepVerifier verify} the output
   */
  protected <T> void testEncode(Publisher<? extends T> input, ResolvableType inputType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints,
          Consumer<StepVerifier.FirstStep<DataBuffer>> stepConsumer) {

    Flux<DataBuffer> result = encoder().encode(input, this.bufferFactory, inputType, mimeType, hints);
    StepVerifier.FirstStep<DataBuffer> step = StepVerifier.create(result);
    stepConsumer.accept(step);
  }

  /**
   * Test a {@link Encoder#encode encode} scenario where the input stream contains an error.
   * This test method will feed the first element of the {@code input} stream to the encoder,
   * followed by an {@link InputException}.
   * The result is expected to contain one "normal" element, followed by the error.
   *
   * @param input the input to be provided to the encoder
   * @param inputType the input type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   * @see InputException
   */
  protected void testEncodeError(Publisher<?> input, ResolvableType inputType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    input = Flux.concat(
            Flux.from(input).take(1),
            Flux.error(new InputException()));

    Flux<DataBuffer> result = encoder().encode(input, this.bufferFactory, inputType,
            mimeType, hints);

    StepVerifier.create(result)
            .consumeNextWith(DataBufferUtils::release)
            .expectError(InputException.class)
            .verify();
  }

  /**
   * Test a {@link Encoder#encode encode} scenario where the input stream is canceled.
   * This test method will feed the first element of the {@code input} stream to the decoder,
   * followed by a cancel signal.
   * The result is expected to contain one "normal" element.
   *
   * @param input the input to be provided to the encoder
   * @param inputType the input type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   */
  protected void testEncodeCancel(Publisher<?> input, ResolvableType inputType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Flux<DataBuffer> result = encoder().encode(input, this.bufferFactory, inputType, mimeType,
            hints);

    StepVerifier.create(result)
            .consumeNextWith(DataBufferUtils::release)
            .thenCancel()
            .verify();
  }

  /**
   * Test a {@link Encoder#encode encode} scenario where the input stream is empty.
   * The output is expected to be empty as well.
   *
   * @param inputType the input type
   * @param mimeType the mime type to use for decoding. May be {@code null}.
   * @param hints the hints used for decoding. May be {@code null}.
   */
  protected void testEncodeEmpty(ResolvableType inputType, @Nullable MimeType mimeType,
          @Nullable Map<String, Object> hints) {

    Flux<?> input = Flux.empty();
    Flux<DataBuffer> result = encoder().encode(input, this.bufferFactory, inputType,
            mimeType, hints);

    StepVerifier.create(result)
            .verifyComplete();
  }

  /**
   * Create a result consumer that expects the given bytes.
   *
   * @param expected the expected bytes
   * @return a consumer that expects the given data buffer to be equal to {@code expected}
   */
  protected final Consumer<DataBuffer> expectBytes(byte[] expected) {
    return dataBuffer -> {
      byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
      dataBuffer.read(resultBytes);
      DataBufferUtils.release(dataBuffer);
      Assertions.assertThat(resultBytes).isEqualTo(expected);
    };
  }

  /**
   * Create a result consumer that expects the given string, using the UTF-8 encoding.
   *
   * @param expected the expected string
   * @return a consumer that expects the given data buffer to be equal to {@code expected}
   */
  protected Consumer<DataBuffer> expectString(String expected) {
    return dataBuffer -> {
      String actual = dataBuffer.toString(UTF_8);
      DataBufferUtils.release(dataBuffer);
      Assertions.assertThat(actual).isEqualTo(expected);
    };
  }

  @SuppressWarnings("unchecked")
  private <T> Encoder<T> encoder() {
    return (Encoder<T>) this.encoder;
  }

  /**
   * Exception used in {@link #testEncodeError}.
   */
  @SuppressWarnings("serial")
  public static class InputException extends RuntimeException {

  }

}
