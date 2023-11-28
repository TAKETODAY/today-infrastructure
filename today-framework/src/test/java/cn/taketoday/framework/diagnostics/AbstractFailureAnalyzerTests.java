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

package cn.taketoday.framework.diagnostics;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractFailureAnalyzer}.
 *
 * @author Kim Jung Bin
 * @author Stephane Nicoll
 */
class AbstractFailureAnalyzerTests {

  private final TestFailureAnalyzer failureAnalyzer = new TestFailureAnalyzer();

  @Test
  void findCauseWithNullException() {
    assertThat(this.failureAnalyzer.findCause(null, Throwable.class)).isNull();
  }

  @Test
  void findCauseWithDirectExactMatch() {
    TestException ex = new TestException();
    assertThat(this.failureAnalyzer.findCause(ex, TestException.class)).isEqualTo(ex);
  }

  @Test
  void findCauseWithDirectSubClass() {
    SpecificTestException ex = new SpecificTestException();
    assertThat(this.failureAnalyzer.findCause(ex, TestException.class)).isEqualTo(ex);
  }

  @Test
  void findCauseWitNestedAndExactMatch() {
    TestException ex = new TestException();
    assertThat(this.failureAnalyzer.findCause(new IllegalArgumentException(new IllegalStateException(ex)),
            TestException.class)).isEqualTo(ex);
  }

  @Test
  void findCauseWitNestedAndSubClass() {
    SpecificTestException ex = new SpecificTestException();
    assertThat(this.failureAnalyzer.findCause(new IOException(new IllegalStateException(ex)), TestException.class))
            .isEqualTo(ex);
  }

  @Test
  void findCauseWithUnrelatedException() {
    IOException ex = new IOException();
    assertThat(this.failureAnalyzer.findCause(ex, TestException.class)).isNull();
  }

  @Test
  void findCauseWithMoreSpecificException() {
    TestException ex = new TestException();
    assertThat(this.failureAnalyzer.findCause(ex, SpecificTestException.class)).isNull();
  }

  static class TestFailureAnalyzer extends AbstractFailureAnalyzer<Throwable> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
      return null;
    }

  }

  @SuppressWarnings("serial")
  static class TestException extends Exception {

  }

  @SuppressWarnings("serial")
  static class SpecificTestException extends TestException {

  }

}
