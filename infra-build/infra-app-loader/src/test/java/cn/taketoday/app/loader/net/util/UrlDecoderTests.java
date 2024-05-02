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

package cn.taketoday.app.loader.net.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UrlDecoder}.
 *
 * @author Phillip Webb
 */
class UrlDecoderTests {

  @Test
  void decodeWhenBasicString() {
    assertThat(UrlDecoder.decode("a/b/C.class")).isEqualTo("a/b/C.class");
  }

  @Test
  void decodeWhenHasSingleByteEncodedCharacters() {
    assertThat(UrlDecoder.decode("%61/%62/%43.class")).isEqualTo("a/b/C.class");
  }

  @Test
  void decodeWhenHasDoubleByteEncodedCharacters() {
    assertThat(UrlDecoder.decode("%c3%a1/b/C.class")).isEqualTo("\u00e1/b/C.class");
  }

  @Test
  void decodeWhenHasMixtureOfEncodedAndUnencodedDoubleByteCharacters() {
    assertThat(UrlDecoder.decode("%c3%a1/b/\u00c7.class")).isEqualTo("\u00e1/b/\u00c7.class");
  }

}
