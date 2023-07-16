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
 * Tests for {@link ContainerReference}.
 *
 * @author Phillip Webb
 */
class ContainerReferenceTests {

  @Test
  void ofCreatesInstance() {
    ContainerReference reference = ContainerReference
            .of("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
    assertThat(reference).hasToString("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
  }

  @Test
  void ofWhenNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ContainerReference.of(null))
            .withMessage("Value must not be empty");
  }

  @Test
  void ofWhenEmptyThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ContainerReference.of(""))
            .withMessage("Value must not be empty");
  }

  @Test
  void hashCodeAndEquals() {
    ContainerReference r1 = ContainerReference
            .of("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
    ContainerReference r2 = ContainerReference
            .of("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
    ContainerReference r3 = ContainerReference
            .of("02691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
    assertThat(r1).hasSameHashCodeAs(r2);
    assertThat(r1).isEqualTo(r1).isEqualTo(r2).isNotEqualTo(r3);
  }

}
