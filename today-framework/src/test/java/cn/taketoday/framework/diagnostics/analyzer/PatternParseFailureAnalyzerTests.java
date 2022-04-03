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

package cn.taketoday.framework.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.web.util.pattern.PathPatternParser;
import cn.taketoday.web.util.pattern.PatternParseException;

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
    assertThat(failureAnalysis.getDescription()).contains("Invalid mapping pattern detected: /spring/**/framework");
    assertThat(failureAnalysis.getAction())
            .contains("Fix this pattern in your application or switch to the legacy parser"
                    + " implementation with 'spring.mvc.pathmatch.matching-strategy=ant_path_matcher'.");
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
