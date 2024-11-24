/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.app.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import infra.app.diagnostics.FailureAnalysis;
import infra.web.util.pattern.PathPatternParser;
import infra.web.util.pattern.PatternParseException;
import infra.app.diagnostics.analyzer.PatternParseFailureAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PatternParseFailureAnalyzer}
 *
 * @author Brian Clozel
 */
class PatternParseFailureAnalyzerTests {

  private PathPatternParser parser = new PathPatternParser();

  @Test
  void patternParseFailureQuotesPattern() {
    FailureAnalysis failureAnalysis = performAnalysis("/spring/**/framework");
    assertThat(failureAnalysis.getDescription()).contains("Invalid mapping pattern detected:\n/spring/**/framework\n");
    assertThat(failureAnalysis.getAction())
            .contains("Fix this pattern in your application or switch to the legacy parser"
                    + " implementation with 'web.mvc.pathmatch.matching-strategy=ant_path_matcher'.");
  }

  private FailureAnalysis performAnalysis(String pattern) {
    PatternParseException failure = createFailure(pattern);
    assertThat(failure).isNotNull();
    return new PatternParseFailureAnalyzer().analyze(failure);
  }

  PatternParseException createFailure(String pattern) {
    try {
      this.parser.parse(pattern);
      return null;
    }
    catch (PatternParseException ex) {
      return ex;
    }
  }

}
