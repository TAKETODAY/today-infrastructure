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

package infra.app.loader.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/14 09:53
 */
class UriPathEncoderTests {

  @Test
  void encodePath() {
    assertThat(UriPathEncoder.encode("/foo/bar")).isEqualTo("/foo/bar");
    assertThat(UriPathEncoder.encode("/foo bar")).isEqualTo("/foo%20bar");
    assertThat(UriPathEncoder.encode("/Z\u00fcrich")).isEqualTo("/Z%C3%BCrich");
  }

  @Test
  void encodePathWhenNoEncodingIsRequiredReturnsSameInstance() {
    String path = "/foo/bar";
    assertThat(UriPathEncoder.encode(path)).isSameAs(path);
  }

}