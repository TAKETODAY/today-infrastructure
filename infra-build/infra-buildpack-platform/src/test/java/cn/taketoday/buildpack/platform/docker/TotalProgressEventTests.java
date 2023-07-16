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

package cn.taketoday.buildpack.platform.docker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TotalProgressEvent}.
 *
 * @author Phillip Webb
 */
class TotalProgressEventTests {

  @Test
  void create() {
    assertThat(new TotalProgressEvent(0).getPercent()).isZero();
    assertThat(new TotalProgressEvent(10).getPercent()).isEqualTo(10);
    assertThat(new TotalProgressEvent(100).getPercent()).isEqualTo(100);
  }

  @Test
  void createWhenPercentLessThanZeroThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new TotalProgressEvent(-1))
            .withMessage("Percent must be in the range 0 to 100");
  }

  @Test
  void createWhenEventMoreThanOneHundredThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new TotalProgressEvent(101))
            .withMessage("Percent must be in the range 0 to 100");
  }

}
