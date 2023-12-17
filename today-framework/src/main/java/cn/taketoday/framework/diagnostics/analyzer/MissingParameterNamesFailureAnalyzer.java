/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.framework.diagnostics.FailureAnalyzer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link FailureAnalyzer} for exceptions caused by missing parameter names. This analyzer
 * is ordered last, if other analyzers wish to also report parameter actions they can use
 * the {@link #analyzeForMissingParameters(Throwable)} static method.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class MissingParameterNamesFailureAnalyzer implements FailureAnalyzer {

  private static final String USE_PARAMETERS_MESSAGE = "Ensure that the compiler uses the '-parameters' flag";

  static final String POSSIBILITY = "This may be due to missing parameter name information";

  static String ACTION = """
          Ensure that your compiler is configured to use the '-parameters' flag.
          You may need to update both your build tool settings as well as your IDE.""";

  @Override
  public FailureAnalysis analyze(Throwable failure) {
    return analyzeForMissingParameters(failure);
  }

  /**
   * Analyze the given failure for missing parameter name exceptions.
   *
   * @param failure the failure to analyze
   * @return a failure analysis or {@code null}
   */
  @Nullable
  static FailureAnalysis analyzeForMissingParameters(Throwable failure) {
    return analyzeForMissingParameters(failure, failure, new HashSet<>());
  }

  @Nullable
  private static FailureAnalysis analyzeForMissingParameters(
          Throwable rootFailure, @Nullable Throwable cause, Set<Throwable> seen) {

    if (cause != null && seen.add(cause)) {
      if (isInfraParametersException(cause)) {
        return getAnalysis(rootFailure, cause);
      }
      FailureAnalysis analysis = analyzeForMissingParameters(rootFailure, cause.getCause(), seen);
      if (analysis != null) {
        return analysis;
      }
      for (Throwable suppressed : cause.getSuppressed()) {
        analysis = analyzeForMissingParameters(rootFailure, suppressed, seen);
        if (analysis != null) {
          return analysis;
        }
      }
    }
    return null;
  }

  private static boolean isInfraParametersException(Throwable failure) {
    String message = failure.getMessage();
    return message != null && message.contains(USE_PARAMETERS_MESSAGE) && isInfraException(failure);
  }

  private static boolean isInfraException(Throwable failure) {
    StackTraceElement[] elements = failure.getStackTrace();
    return elements.length > 0 && isInfraClass(elements[0].getClassName());
  }

  private static boolean isInfraClass(@Nullable String className) {
    return className != null && className.startsWith("cn.taketoday.");
  }

  private static FailureAnalysis getAnalysis(Throwable rootFailure, Throwable cause) {
    StringBuilder description = new StringBuilder(String.format("%s:%n", cause.getMessage()));
    if (rootFailure != cause) {
      description.append(String.format("%n    Resulting Failure: %s", getExceptionTypeAndMessage(rootFailure)));
    }
    return new FailureAnalysis(description.toString(), ACTION, rootFailure);
  }

  private static String getExceptionTypeAndMessage(Throwable ex) {
    String message = ex.getMessage();
    return ex.getClass().getName() + (StringUtils.hasText(message) ? ": " + message : "");
  }

  static void appendPossibility(StringBuilder description) {
    if (!description.toString().endsWith(System.lineSeparator())) {
      description.append("%n".formatted());
    }
    description.append("%n%s".formatted(POSSIBILITY));
  }

}
