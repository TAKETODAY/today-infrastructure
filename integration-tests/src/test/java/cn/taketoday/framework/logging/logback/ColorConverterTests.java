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

package cn.taketoday.framework.logging.logback;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import cn.taketoday.core.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ColorConverter}.
 *
 * @author Phillip Webb
 */
class ColorConverterTests {

  private final ColorConverter converter = new ColorConverter();

  private final LoggingEvent event = new LoggingEvent();

  private final String in = "in";

  @BeforeAll
  static void setupAnsi() {
    AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
  }

  @AfterAll
  static void resetAnsi() {
    AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
  }

  @Test
  void black() {
    this.converter.setOptionList(Collections.singletonList("black"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[30min\033[0;39m");
  }

  @Test
  void white() {
    this.converter.setOptionList(Collections.singletonList("white"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[37min\033[0;39m");
  }

  @Test
  void faint() {
    this.converter.setOptionList(Collections.singletonList("faint"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[2min\033[0;39m");
  }

  @Test
  void red() {
    this.converter.setOptionList(Collections.singletonList("red"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[31min\033[0;39m");
  }

  @Test
  void green() {
    this.converter.setOptionList(Collections.singletonList("green"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[32min\033[0;39m");
  }

  @Test
  void yellow() {
    this.converter.setOptionList(Collections.singletonList("yellow"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[33min\033[0;39m");
  }

  @Test
  void blue() {
    this.converter.setOptionList(Collections.singletonList("blue"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[34min\033[0;39m");
  }

  @Test
  void magenta() {
    this.converter.setOptionList(Collections.singletonList("magenta"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[35min\033[0;39m");
  }

  @Test
  void cyan() {
    this.converter.setOptionList(Collections.singletonList("cyan"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[36min\033[0;39m");
  }

  @Test
  void brightBlack() {
    this.converter.setOptionList(Collections.singletonList("bright_black"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[90min\033[0;39m");
  }

  @Test
  void brightWhite() {
    this.converter.setOptionList(Collections.singletonList("bright_white"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[97min\033[0;39m");
  }

  @Test
  void brightRed() {
    this.converter.setOptionList(Collections.singletonList("bright_red"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[91min\033[0;39m");
  }

  @Test
  void brightGreen() {
    this.converter.setOptionList(Collections.singletonList("bright_green"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[92min\033[0;39m");
  }

  @Test
  void brightYellow() {
    this.converter.setOptionList(Collections.singletonList("bright_yellow"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[93min\033[0;39m");
  }

  @Test
  void brightBlue() {
    this.converter.setOptionList(Collections.singletonList("bright_blue"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[94min\033[0;39m");
  }

  @Test
  void brightMagenta() {
    this.converter.setOptionList(Collections.singletonList("bright_magenta"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[95min\033[0;39m");
  }

  @Test
  void brightCyan() {
    this.converter.setOptionList(Collections.singletonList("bright_cyan"));
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[96min\033[0;39m");
  }

  @Test
  void highlightError() {
    this.event.setLevel(Level.ERROR);
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[31min\033[0;39m");
  }

  @Test
  void highlightWarn() {
    this.event.setLevel(Level.WARN);
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[33min\033[0;39m");
  }

  @Test
  void highlightDebug() {
    this.event.setLevel(Level.DEBUG);
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[32min\033[0;39m");
  }

  @Test
  void highlightTrace() {
    this.event.setLevel(Level.TRACE);
    String out = this.converter.transform(this.event, this.in);
    assertThat(out).isEqualTo("\033[32min\033[0;39m");
  }

}
