/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.loader.net.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UrlEncoder}.
 *
 * @author Phillip Webb
 */
class UrlEncoderTests {

  @Test
  void encodeWhenBasicString() {
    assertThat(UrlEncoder.encode("a/b/C.class")).isEqualTo("a/b/C.class");
  }

  @Test
  void encodeWhenHasSingleByteEncodedCharacters() {
    assertThat(UrlEncoder.encode("a b/c#d%e?f.txt")).isEqualTo("a%20b/c%23d%25e%3ff.txt");
  }

  @Test
  void encodeWhenHasDoubleByteEncodedCharacters() {
    assertThat(UrlEncoder.encode("\u00e1/b/C.class")).isEqualTo("%c3%a1/b/C.class");
  }

  @Test
  void encodeWhenHasTrippleByteEncodedCharacters() {
    assertThat(UrlEncoder.encode("\u6841/b/C.class")).isEqualTo("%e6%a1%81/b/C.class");
  }

  @Test
  void encodeWhenHasMixtureOfEncodedAndUnencodedDoubleByteCharacters() {
    assertThat(UrlEncoder.encode("\u00e1/b/\u00c7.class")).isEqualTo("%c3%a1/b/%c3%87.class");
  }

  @Test
  void encodeWhenNeedsToExpand() {
    StringBuilder path = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      path.append("\u00e1");
    }
    String encoded = UrlEncoder.encode(path.toString());
    assertThat(encoded).hasSize(600);
  }

}
