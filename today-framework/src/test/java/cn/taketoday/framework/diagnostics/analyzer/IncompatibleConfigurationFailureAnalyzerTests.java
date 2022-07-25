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

import cn.taketoday.context.properties.IncompatibleConfigurationException;
import cn.taketoday.framework.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IncompatibleConfigurationFailureAnalyzer}
 *
 * @author Brian Clozel
 */
class IncompatibleConfigurationFailureAnalyzerTests {

  @Test
  void incompatibleConfigurationListsKeys() {
    FailureAnalysis failureAnalysis = performAnalysis("spring.first.key", "spring.second.key");
    assertThat(failureAnalysis.getDescription()).contains(
            "The following configuration properties have incompatible values: [spring.first.key, spring.second.key]");
    assertThat(failureAnalysis.getAction())
            .contains("Review the docs for spring.first.key, spring.second.key and change the configured values.");
  }

  private FailureAnalysis performAnalysis(String... keys) {
    IncompatibleConfigurationException failure = new IncompatibleConfigurationException(keys);
    return new IncompatibleConfigurationFailureAnalyzer().analyze(failure);
  }

}
