/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link VolumeName}.
 *
 * @author Phillip Webb
 */
class VolumeNameTests {

  @Test
  void randomWhenPrefixIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.random(null))
            .withMessage("Prefix is required");
  }

  @Test
  void randomGeneratesRandomString() {
    VolumeName v1 = VolumeName.random("abc-");
    VolumeName v2 = VolumeName.random("abc-");
    assertThat(v1.toString()).startsWith("abc-").hasSize(14);
    assertThat(v2.toString()).startsWith("abc-").hasSize(14);
    assertThat(v1).isNotEqualTo(v2);
    assertThat(v1.toString()).isNotEqualTo(v2.toString());
  }

  @Test
  void randomStringWithLengthGeneratesRandomString() {
    VolumeName v1 = VolumeName.random("abc-", 20);
    VolumeName v2 = VolumeName.random("abc-", 20);
    assertThat(v1.toString()).startsWith("abc-").hasSize(24);
    assertThat(v2.toString()).startsWith("abc-").hasSize(24);
    assertThat(v1).isNotEqualTo(v2);
    assertThat(v1.toString()).isNotEqualTo(v2.toString());
  }

  @Test
  void basedOnWhenSourceIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.basedOn(null, "prefix", "suffix", 6))
            .withMessage("Source is required");
  }

  @Test
  void basedOnWhenNameExtractorIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.basedOn("test", null, "prefix", "suffix", 6))
            .withMessage("NameExtractor is required");
  }

  @Test
  void basedOnWhenPrefixIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.basedOn("test", null, "suffix", 6))
            .withMessage("Prefix is required");
  }

  @Test
  void basedOnWhenSuffixIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.basedOn("test", "prefix", null, 6))
            .withMessage("Suffix is required");
  }

  @Test
  void basedOnGeneratesHashBasedName() {
    VolumeName name = VolumeName.basedOn("index.docker.io/library/myapp:latest", "pack-cache-", ".build", 6);
    assertThat(name).hasToString("pack-cache-40a311b545d7.build");
  }

  @Test
  void basedOnWhenSizeIsTooBigThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.basedOn("name", "prefix", "suffix", 33))
            .withMessage("DigestLength must be less than or equal to 32");
  }

  @Test
  void ofWhenValueIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.of(null))
            .withMessage("Value is required");
  }

  @Test
  void ofGeneratesValue() {
    VolumeName name = VolumeName.of("test");
    assertThat(name).hasToString("test");
  }

  @Test
  void equalsAndHashCode() {
    VolumeName n1 = VolumeName.of("test1");
    VolumeName n2 = VolumeName.of("test1");
    VolumeName n3 = VolumeName.of("test2");
    assertThat(n1).hasSameHashCodeAs(n2);
    assertThat(n1).isEqualTo(n1).isEqualTo(n2).isNotEqualTo(n3);
  }

}
