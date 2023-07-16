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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TotalProgressBar}.
 *
 * @author Phillip Webb
 */
class TotalProgressBarTests {

  @Test
  void withPrefixAndBookends() {
    TestPrintStream out = new TestPrintStream();
    TotalProgressBar bar = new TotalProgressBar("prefix:", '#', true, out);
    assertThat(out).hasToString("prefix: [ ");
    bar.accept(new TotalProgressEvent(10));
    assertThat(out).hasToString("prefix: [ #####");
    bar.accept(new TotalProgressEvent(50));
    assertThat(out).hasToString("prefix: [ #########################");
    bar.accept(new TotalProgressEvent(100));
    assertThat(out).hasToString(String.format("prefix: [ ################################################## ]%n"));
  }

  @Test
  void withoutPrefix() {
    TestPrintStream out = new TestPrintStream();
    TotalProgressBar bar = new TotalProgressBar(null, '#', true, out);
    assertThat(out).hasToString("[ ");
    bar.accept(new TotalProgressEvent(10));
    assertThat(out).hasToString("[ #####");
    bar.accept(new TotalProgressEvent(50));
    assertThat(out).hasToString("[ #########################");
    bar.accept(new TotalProgressEvent(100));
    assertThat(out).hasToString(String.format("[ ################################################## ]%n"));
  }

  @Test
  void withoutBookends() {
    TestPrintStream out = new TestPrintStream();
    TotalProgressBar bar = new TotalProgressBar("", '.', false, out);
    assertThat(out).hasToString("");
    bar.accept(new TotalProgressEvent(10));
    assertThat(out).hasToString(".....");
    bar.accept(new TotalProgressEvent(50));
    assertThat(out).hasToString(".........................");
    bar.accept(new TotalProgressEvent(100));
    assertThat(out).hasToString(String.format("..................................................%n"));
  }

  static class TestPrintStream extends PrintStream {

    TestPrintStream() {
      super(new ByteArrayOutputStream());
    }

    @Override
    public String toString() {
      return this.out.toString();
    }

  }

}
