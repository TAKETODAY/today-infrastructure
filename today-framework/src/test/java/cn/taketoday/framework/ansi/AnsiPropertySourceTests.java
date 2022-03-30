/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.ansi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.framework.ansi.AnsiOutput.Enabled;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnsiPropertySource}.
 *
 * @author Phillip Webb
 * @author Toshiaki Maki
 */
class AnsiPropertySourceTests {

  private final AnsiPropertySource source = new AnsiPropertySource("ansi", false);

  @AfterEach
  void reset() {
    AnsiOutput.setEnabled(Enabled.DETECT);
  }

  @Test
  void getAnsiStyle() {
    assertThat(this.source.getProperty("AnsiStyle.BOLD")).isEqualTo(AnsiStyle.BOLD);
  }

  @Test
  void getAnsiColor() {
    assertThat(this.source.getProperty("AnsiColor.RED")).isEqualTo(AnsiColor.RED);
    assertThat(this.source.getProperty("AnsiColor.100")).isEqualTo(Ansi8BitColor.foreground(100));
  }

  @Test
  void getAnsiBackground() {
    assertThat(this.source.getProperty("AnsiBackground.GREEN")).isEqualTo(AnsiBackground.GREEN);
    assertThat(this.source.getProperty("AnsiBackground.100")).isEqualTo(Ansi8BitColor.background(100));
  }

  @Test
  void getAnsi() {
    assertThat(this.source.getProperty("Ansi.BOLD")).isEqualTo(AnsiStyle.BOLD);
    assertThat(this.source.getProperty("Ansi.RED")).isEqualTo(AnsiColor.RED);
    assertThat(this.source.getProperty("Ansi.BG_RED")).isEqualTo(AnsiBackground.RED);
  }

  @Test
  void getMissing() {
    assertThat(this.source.getProperty("AnsiStyle.NOPE")).isNull();
  }

  @Test
  void encodeEnabled() {
    AnsiOutput.setEnabled(Enabled.ALWAYS);
    AnsiPropertySource source = new AnsiPropertySource("ansi", true);
    assertThat(source.getProperty("Ansi.RED")).isEqualTo("\033[31m");
    assertThat(source.getProperty("AnsiColor.100")).isEqualTo("\033[38;5;100m");
    assertThat(source.getProperty("AnsiBackground.100")).isEqualTo("\033[48;5;100m");
  }

  @Test
  void encodeDisabled() {
    AnsiOutput.setEnabled(Enabled.NEVER);
    AnsiPropertySource source = new AnsiPropertySource("ansi", true);
    assertThat(source.getProperty("Ansi.RED")).isEqualTo("");
    assertThat(source.getProperty("AnsiColor.100")).isEqualTo("");
    assertThat(source.getProperty("AnsiBackground.100")).isEqualTo("");
  }

}
