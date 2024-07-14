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

package cn.taketoday.framework.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import cn.taketoday.framework.AotInitializerNotFoundException;
import cn.taketoday.framework.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/14 09:44
 */
class AotInitializerNotFoundFailureAnalyzerTests {

  @Test
  void shouldAnalyze() {
    FailureAnalysis analysis = analyze();
    assertThat(analysis.getDescription()).isEqualTo(
            "Startup with AOT mode enabled failed: AOT initializer class cn.taketoday.framework.diagnostics.analyzer.AotInitializerNotFoundFailureAnalyzerTests__ApplicationContextInitializer could not be found");
    assertThat(analysis.getAction()).isEqualTo(
            """
                Consider the following:
                \tDid you build the application with enabled AOT processing?
                \tIs the main class cn.taketoday.framework.diagnostics.analyzer.AotInitializerNotFoundFailureAnalyzerTests correct?
                \tIf you want to run the application in regular mode, remove the system property 'infra.aot.enabled'""");
  }

  private FailureAnalysis analyze() {
    return new AotInitializerNotFoundFailureAnalyzer()
            .analyze(new AotInitializerNotFoundException(AotInitializerNotFoundFailureAnalyzerTests.class,
                    AotInitializerNotFoundFailureAnalyzerTests.class + "__ApplicationContextInitializer"));
  }

}