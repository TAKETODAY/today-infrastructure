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

package infra.core.test.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import javax.tools.Diagnostic;

import infra.core.test.tools.CompilationException.Problem;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompilationException}.
 *
 * @author Phillip Webb
 */
class CompilationExceptionTests {

  @Test
  void exceptionMessageReportsSingleError() {
    CompilationException exception = new CompilationException(
            List.of(new Problem(Diagnostic.Kind.ERROR, "error message")),
            SourceFiles.none(), ResourceFiles.none());
    assertThat(exception.getMessage().lines()).containsExactly(
            "Unable to compile source", "", "Errors:", "- error message");
  }

  @Test
  void exceptionMessageReportsSingleWarning() {
    CompilationException exception = new CompilationException(
            List.of(new Problem(Diagnostic.Kind.MANDATORY_WARNING, "warning message")),
            SourceFiles.none(), ResourceFiles.none());
    assertThat(exception.getMessage().lines()).containsExactly(
            "Unable to compile source", "", "Warnings:", "- warning message");
  }

  @Test
  void exceptionMessageReportsProblems() {
    CompilationException exception = new CompilationException(List.of(
            new Problem(Diagnostic.Kind.MANDATORY_WARNING, "warning message"),
            new Problem(Diagnostic.Kind.ERROR, "error message"),
            new Problem(Diagnostic.Kind.WARNING, "warning message2"),
            new Problem(Diagnostic.Kind.ERROR, "error message2")), SourceFiles.none(), ResourceFiles.none());
    assertThat(exception.getMessage().lines()).containsExactly(
            "Unable to compile source", "", "Errors:", "- error message", "- error message2", "" ,
            "Warnings:", "- warning message","- warning message2");
  }

  @Test
  void exceptionMessageReportsSourceCode() {
    CompilationException exception = new CompilationException(
            List.of(new Problem(Diagnostic.Kind.ERROR, "error message")),
            SourceFiles.of(SourceFile.of("public class Hello {}")), ResourceFiles.none());
    assertThat(exception.getMessage().lines()).containsExactly(
            "Unable to compile source", "", "Errors:", "- error message", "",
            "---- source: Hello.java", "public class Hello {}");
  }

  @Test
  void exceptionMessageReportsResource() {
    CompilationException exception = new CompilationException(
            List.of(new Problem(Diagnostic.Kind.ERROR, "error message")),
            SourceFiles.none(), ResourceFiles.of(ResourceFile.of("application.properties", "test=value")));
    assertThat(exception.getMessage().lines()).containsExactly(
            "Unable to compile source", "", "Errors:", "- error message", "",
            "---- resource: application.properties", "test=value");
  }

}
