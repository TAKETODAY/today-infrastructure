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

package cn.taketoday.core.test.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SourceFileAssert}.
 *
 * @author Phillip Webb
 */
class SourceFileAssertTests {

  private static final String SAMPLE = """
          package com.example;

          import java.lang.Runnable;

          public class Sample implements Runnable {

          	void run() {
          		run("Hello World!");
          	}

          	void run(String message) {
          		System.out.println(message);
          	}

          	public static void main(String[] args) {
          		new Sample().run();
          	}
          }
          """;

  private final SourceFile sourceFile = SourceFile.of(SAMPLE);

  @Test
  void containsWhenContainsAll() {
    assertThat(this.sourceFile).contains("Sample", "main");
  }

  @Test
  void containsWhenMissingOneThrowsException() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(
            () -> assertThat(this.sourceFile).contains("Sample",
                    "missing")).withMessageContaining("to contain");
  }

  @Test
  void isEqualToWhenEqual() {
    assertThat(this.sourceFile).hasContent(SAMPLE);
  }

  @Test
  void isEqualToWhenNotEqualThrowsException() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(
            () -> assertThat(this.sourceFile).hasContent("no")).withMessageContaining(
            "expected", "but was");
  }

}
