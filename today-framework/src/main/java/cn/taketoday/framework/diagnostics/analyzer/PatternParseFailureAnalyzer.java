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

import cn.taketoday.framework.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.web.util.pattern.PatternParseException;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code PatternParseException}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class PatternParseFailureAnalyzer extends AbstractFailureAnalyzer<PatternParseException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, PatternParseException cause) {
    return new FailureAnalysis("Invalid mapping pattern detected:\n" + cause.toDetailedString(),
            "Fix this pattern in your application or switch to the legacy parser implementation with "
                    + "'web.mvc.pathmatch.matching-strategy=ant_path_matcher'.",
            cause);
  }

}
