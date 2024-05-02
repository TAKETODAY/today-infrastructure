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

package cn.taketoday.app.loader.net.protocol.jar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Canonicalizer}.
 *
 * @author Phillip Webb
 */
class CanonicalizerTests {

  @Test
  void canonicalizeAfterOnlyChangesAfterPos() {
    String prefix = "/foo/.././bar/.!/foo/.././bar/.";
    String canonicalized = Canonicalizer.canonicalizeAfter(prefix, prefix.indexOf("!/"));
    assertThat(canonicalized).isEqualTo("/foo/.././bar/.!/bar/");
  }

  @Test
  void canonicalizeWhenHasEmbeddedSlashDotDotSlash() {
    assertThat(Canonicalizer.canonicalize("/foo/../bar/bif/bam/../../baz")).isEqualTo("/bar/baz");
  }

  @Test
  void canonicalizeWhenHasEmbeddedSlashDotSlash() {
    assertThat(Canonicalizer.canonicalize("/foo/./bar/bif/bam/././baz")).isEqualTo("/foo/bar/bif/bam/baz");
  }

  @Test
  void canonicalizeWhenHasTrailingSlashDotDot() {
    assertThat(Canonicalizer.canonicalize("/foo/bar/baz/../..")).isEqualTo("/foo/");
  }

  @Test
  void canonicalizeWhenHasTrailingSlashDot() {
    assertThat(Canonicalizer.canonicalize("/foo/bar/baz/./.")).isEqualTo("/foo/bar/baz/");
  }

}
