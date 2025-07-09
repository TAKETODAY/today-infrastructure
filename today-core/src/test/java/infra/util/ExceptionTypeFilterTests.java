/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class ExceptionTypeFilterTests {

  ExceptionTypeFilter filter;

  @Test
  void emptyFilter() {
    filter = new ExceptionTypeFilter(null, null);

    assertMatches(new Throwable());
    assertMatches(new Error());
    assertMatches(new Exception());
    assertMatches(new RuntimeException());
  }

  @Test
  void includes() {
    filter = new ExceptionTypeFilter(List.of(FileNotFoundException.class, IllegalArgumentException.class), null);

    assertMatches(new FileNotFoundException());
    assertMatches(new IllegalArgumentException());
    assertMatches(new NumberFormatException());

    assertDoesNotMatch(new Throwable());
    assertDoesNotMatch(new FileSystemException("test"));
  }

  @Test
  void includesSubtypeMatching() {
    filter = new ExceptionTypeFilter(List.of(RuntimeException.class), null);

    assertMatches(new RuntimeException());
    assertMatches(new IllegalStateException());

    assertDoesNotMatch(new Exception());
  }

  @Test
  void excludes() {
    filter = new ExceptionTypeFilter(null, List.of(FileNotFoundException.class, IllegalArgumentException.class));

    assertDoesNotMatch(new FileNotFoundException());
    assertDoesNotMatch(new IllegalArgumentException());

    assertMatches(new Throwable());
    assertMatches(new AssertionError());
    assertMatches(new FileSystemException("test"));
  }

  @Test
  void excludesSubtypeMatching() {
    filter = new ExceptionTypeFilter(null, List.of(IllegalArgumentException.class));

    assertDoesNotMatch(new IllegalArgumentException());
    assertDoesNotMatch(new NumberFormatException());

    assertMatches(new Throwable());
  }

  @Test
  void includesAndExcludes() {
    filter = new ExceptionTypeFilter(List.of(IOException.class), List.of(FileNotFoundException.class));

    assertMatches(new IOException());
    assertMatches(new FileSystemException("test"));

    assertDoesNotMatch(new FileNotFoundException());
    assertDoesNotMatch(new Throwable());
  }

  private void assertMatches(Throwable candidate) {
    assertThat(this.filter.match(candidate))
            .as("filter '" + this.filter + "' should match " + candidate.getClass().getSimpleName())
            .isTrue();
  }

  private void assertDoesNotMatch(Throwable candidate) {
    assertThat(this.filter.match(candidate))
            .as("filter '" + this.filter + "' should not match " + candidate.getClass().getSimpleName())
            .isFalse();
  }

}
