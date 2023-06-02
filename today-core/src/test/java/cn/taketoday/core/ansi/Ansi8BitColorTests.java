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

package cn.taketoday.core.ansi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Ansi8BitColor}.
 *
 * @author Toshiaki Maki
 * @author Phillip Webb
 */
class Ansi8BitColorTests {

  @Test
  void toStringWhenForegroundAddsCorrectPrefix() {
    assertThat(Ansi8BitColor.foreground(208).toString()).isEqualTo("38;5;208");
  }

  @Test
  void toStringWhenBackgroundAddsCorrectPrefix() {
    assertThat(Ansi8BitColor.background(208).toString()).isEqualTo("48;5;208");
  }

  @Test
  void foregroundWhenOutsideBoundsThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Ansi8BitColor.foreground(-1))
            .withMessage("Code must be between 0 and 255");
    assertThatIllegalArgumentException().isThrownBy(() -> Ansi8BitColor.foreground(256))
            .withMessage("Code must be between 0 and 255");
  }

  @Test
  void backgroundWhenOutsideBoundsThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Ansi8BitColor.background(-1))
            .withMessage("Code must be between 0 and 255");
    assertThatIllegalArgumentException().isThrownBy(() -> Ansi8BitColor.background(256))
            .withMessage("Code must be between 0 and 255");
  }

  @Test
  void equalsAndHashCode() {
    Ansi8BitColor one = Ansi8BitColor.foreground(123);
    Ansi8BitColor two = Ansi8BitColor.foreground(123);
    Ansi8BitColor three = Ansi8BitColor.background(123);
    assertThat(one.hashCode()).isEqualTo(two.hashCode());
    assertThat(one).isEqualTo(one).isEqualTo(two).isNotEqualTo(three).isNotEqualTo(null).isNotEqualTo("foo");
  }

}
