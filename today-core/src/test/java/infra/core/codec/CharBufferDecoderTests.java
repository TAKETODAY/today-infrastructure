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

package infra.core.codec;

import org.junit.jupiter.api.Test;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferLimitException;
import infra.core.testfixture.codec.AbstractDecoderTests;
import infra.util.MimeType;
import infra.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/22 20:04
 */
class CharBufferDecoderTests extends AbstractDecoderTests<CharBufferDecoder> {

  private static final ResolvableType TYPE = ResolvableType.forClass(CharBuffer.class);

  CharBufferDecoderTests() {
    super(CharBufferDecoder.allMimeTypes());
  }

  @Override
  @Test
  public void canDecode() {
    assertThat(this.decoder.canDecode(TYPE, MimeType.TEXT_PLAIN)).isTrue();
    assertThat(this.decoder.canDecode(TYPE, MimeType.TEXT_HTML)).isTrue();
    assertThat(this.decoder.canDecode(TYPE, MimeType.APPLICATION_JSON)).isTrue();
    assertThat(this.decoder.canDecode(TYPE, MimeTypeUtils.parseMimeType("text/plain;charset=utf-8"))).isTrue();
    assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class), MimeType.TEXT_PLAIN)).isFalse();
    assertThat(this.decoder.canDecode(ResolvableType.forClass(Object.class), MimeType.APPLICATION_JSON)).isFalse();
  }

  @Override
  @Test
  public void decode() {
    CharBuffer u = charBuffer("ü");
    CharBuffer e = charBuffer("é");
    CharBuffer o = charBuffer("ø");
    String s = String.format("%s\n%s\n%s", u, e, o);
    Flux<DataBuffer> input = toDataBuffers(s, 1, UTF_8);

    testDecodeAll(input, TYPE, step -> step.expectNext(u, e, o).verifyComplete(), null, null);
  }

  @Test
  void decodeMultibyteCharacterUtf16() {
    CharBuffer u = charBuffer("ü");
    CharBuffer e = charBuffer("é");
    CharBuffer o = charBuffer("ø");
    String s = String.format("%s\n%s\n%s", u, e, o);
    Flux<DataBuffer> source = toDataBuffers(s, 2, UTF_16BE);
    MimeType mimeType = MimeTypeUtils.parseMimeType("text/plain;charset=utf-16be");

    testDecode(source, TYPE, step -> step.expectNext(u, e, o).verifyComplete(), mimeType, null);
  }

  private Flux<DataBuffer> toDataBuffers(String s, int length, Charset charset) {
    byte[] bytes = s.getBytes(charset);
    List<byte[]> chunks = new ArrayList<>();
    for (int i = 0; i < bytes.length; i += length) {
      chunks.add(Arrays.copyOfRange(bytes, i, i + length));
    }
    return Flux.fromIterable(chunks)
            .map(chunk -> {
              DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(length);
              dataBuffer.write(chunk, 0, chunk.length);
              return dataBuffer;
            });
  }

  @Test
  void decodeNewLine() {
    Flux<DataBuffer> input = Flux.just(
            stringBuffer("\r\nabc\n"),
            stringBuffer("def"),
            stringBuffer("ghi\r\n\n"),
            stringBuffer("jkl"),
            stringBuffer("mno\npqr\n"),
            stringBuffer("stu"),
            stringBuffer("vw"),
            stringBuffer("xyz")
    );

    testDecode(input, CharBuffer.class, step -> step
            .expectNext(charBuffer("")).as("1st")
            .expectNext(charBuffer("abc"))
            .expectNext(charBuffer("defghi"))
            .expectNext(charBuffer("")).as("2nd")
            .expectNext(charBuffer("jklmno"))
            .expectNext(charBuffer("pqr"))
            .expectNext(charBuffer("stuvwxyz"))
            .expectComplete()
            .verify());
  }

  @Test
  void decodeNewlinesAcrossBuffers() {
    Flux<DataBuffer> input = Flux.just(
            stringBuffer("\r"),
            stringBuffer("\n"),
            stringBuffer("xyz")
    );

    testDecode(input, CharBuffer.class, step -> step
            .expectNext(charBuffer(""))
            .expectNext(charBuffer("xyz"))
            .expectComplete()
            .verify());
  }

  @Test
  void maxInMemoryLimit() {
    Flux<DataBuffer> input = Flux.just(
            stringBuffer("abc\n"), stringBuffer("defg\n"),
            stringBuffer("hi"), stringBuffer("jkl"), stringBuffer("mnop"));

    this.decoder.setMaxInMemorySize(5);

    testDecode(input, CharBuffer.class, step -> step
            .expectNext(charBuffer("abc"))
            .expectNext(charBuffer("defg"))
            .verifyError(DataBufferLimitException.class));
  }

  @Test
  void maxInMemoryLimitDoesNotApplyToParsedItemsThatDontRequireBuffering() {
    Flux<DataBuffer> input = Flux.just(
            stringBuffer("TOO MUCH DATA\nanother line\n\nand another\n"));

    this.decoder.setMaxInMemorySize(5);

    testDecode(input, CharBuffer.class, step -> step
            .expectNext(charBuffer("TOO MUCH DATA"))
            .expectNext(charBuffer("another line"))
            .expectNext(charBuffer(""))
            .expectNext(charBuffer("and another"))
            .expectComplete()
            .verify());
  }

  @Test
    // gh-24339
  void maxInMemoryLimitReleaseUnprocessedLinesWhenUnlimited() {
    Flux<DataBuffer> input = Flux.just(stringBuffer("Line 1\nLine 2\nLine 3\n"));

    this.decoder.setMaxInMemorySize(-1);
    testDecodeCancel(input, ResolvableType.forClass(String.class), null, Collections.emptyMap());
  }

  @Test
  void decodeNewLineIncludeDelimiters() {
    this.decoder = CharBufferDecoder.allMimeTypes(CharBufferDecoder.DEFAULT_DELIMITERS, false);

    Flux<DataBuffer> input = Flux.just(
            stringBuffer("\r\nabc\n"),
            stringBuffer("def"),
            stringBuffer("ghi\r\n\n"),
            stringBuffer("jkl"),
            stringBuffer("mno\npqr\n"),
            stringBuffer("stu"),
            stringBuffer("vw"),
            stringBuffer("xyz")
    );

    testDecode(input, CharBuffer.class, step -> step
            .expectNext(charBuffer("\r\n"))
            .expectNext(charBuffer("abc\n"))
            .expectNext(charBuffer("defghi\r\n"))
            .expectNext(charBuffer("\n"))
            .expectNext(charBuffer("jklmno\n"))
            .expectNext(charBuffer("pqr\n"))
            .expectNext(charBuffer("stuvwxyz"))
            .expectComplete()
            .verify());
  }

  @Test
  void decodeEmptyFlux() {
    Flux<DataBuffer> input = Flux.empty();

    testDecode(input, String.class, step -> step
            .expectComplete()
            .verify());
  }

  @Test
  void decodeEmptyDataBuffer() {
    Flux<DataBuffer> input = Flux.just(stringBuffer(""));
    Flux<CharBuffer> output = this.decoder.decode(input,
            TYPE, null, Collections.emptyMap());

    StepVerifier.create(output)
            .expectNext(charBuffer(""))
            .expectComplete().verify();
  }

  @Override
  @Test
  public void decodeToMono() {
    Flux<DataBuffer> input = Flux.just(
            stringBuffer("foo"),
            stringBuffer("bar"),
            stringBuffer("baz"));

    testDecodeToMonoAll(input, CharBuffer.class, step -> step
            .expectNext(charBuffer("foobarbaz"))
            .expectComplete()
            .verify());
  }

  @Test
  void decodeToMonoWithEmptyFlux() {
    Flux<DataBuffer> input = Flux.empty();

    testDecodeToMono(input, String.class, step -> step
            .expectComplete()
            .verify());
  }

  private DataBuffer stringBuffer(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
    buffer.write(bytes);
    return buffer;
  }

  private CharBuffer charBuffer(String value) {
    return CharBuffer
            .allocate(value.length())
            .put(value)
            .flip();
  }

}
