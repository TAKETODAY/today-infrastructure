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

import cn.taketoday.framework.AotInitializerNotFoundException;
import cn.taketoday.framework.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.framework.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link AotInitializerNotFoundException}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class AotInitializerNotFoundFailureAnalyzer extends AbstractFailureAnalyzer<AotInitializerNotFoundException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, AotInitializerNotFoundException cause) {
    return new FailureAnalysis(cause.getMessage(), "Consider the following:\n"
            + "\tDid you build the application with enabled AOT processing?\n"
            + "\tIs the main class %s correct?\n".formatted(cause.getMainClass().getName())
            + "\tIf you want to run the application in regular mode, remove the system property 'infra.aot.enabled'",
            cause);
  }

}
