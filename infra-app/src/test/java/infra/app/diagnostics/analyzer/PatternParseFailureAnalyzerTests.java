/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import infra.app.diagnostics.FailureAnalysis;
import infra.web.util.pattern.PathPatternParser;
import infra.web.util.pattern.PatternParseException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PatternParseFailureAnalyzer}
 *
 * @author Brian Clozel
 */
class PatternParseFailureAnalyzerTests {

  private final PathPatternParser parser = new PathPatternParser();

  @Test
  void patternParseFailureQuotesPattern() {
    FailureAnalysis failureAnalysis = performAnalysis("/infra/**/app");
    assertThat(failureAnalysis.getDescription()).contains("Invalid mapping pattern detected:\n/infra/**/app\n");
    assertThat(failureAnalysis.getAction())
            .contains("Fix this pattern in your application.");
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
