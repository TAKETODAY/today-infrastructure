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

package cn.taketoday.core.codec;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.stream.Stream;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class CharSequenceEncoderTests extends AbstractEncoderTests<CharSequenceEncoder> {

  private final String foo = "foo";

  private final String bar = "bar";

  CharSequenceEncoderTests() {
    super(CharSequenceEncoder.textPlainOnly());
  }

  @Override
  @Test
  public void canEncode() throws Exception {
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(String.class),
            MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(StringBuilder.class),
            MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(StringBuffer.class),
            MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(Integer.class),
            MimeTypeUtils.TEXT_PLAIN)).isFalse();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(String.class),
            MimeTypeUtils.APPLICATION_JSON)).isFalse();

    // SPR-15464
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
  }

  @Override
  @Test
  public void encode() {
    Flux<CharSequence> input = Flux.just(this.foo, this.bar);

    testEncodeAll(input, CharSequence.class, step -> step
            .consumeNextWith(expectString(this.foo))
            .consumeNextWith(expectString(this.bar))
            .verifyComplete());
  }

  @Test
  void calculateCapacity() {
    String sequence = "Hello World!";
    Stream.of(UTF_8, UTF_16, ISO_8859_1, US_ASCII, Charset.forName("BIG5"))
            .forEach(charset -> {
              int capacity = this.encoder.calculateCapacity(sequence, charset);
              int length = sequence.length();
              assertThat(capacity >= length).as(String.format("%s has capacity %d; length %d", charset, capacity, length)).isTrue();
            });
  }

}
