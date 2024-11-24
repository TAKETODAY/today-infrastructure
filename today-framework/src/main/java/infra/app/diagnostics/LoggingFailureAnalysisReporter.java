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

package infra.app.diagnostics;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.StringUtils;

/**
 * {@link FailureAnalysisReporter} that logs the failure analysis.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class LoggingFailureAnalysisReporter implements FailureAnalysisReporter {

  private static final Logger logger = LoggerFactory.getLogger(LoggingFailureAnalysisReporter.class);

  @Override
  public void report(FailureAnalysis failureAnalysis) {
    logger.debug("Application failed to start due to an exception", failureAnalysis.getCause());
    if (logger.isErrorEnabled()) {
      logger.error(buildMessage(failureAnalysis));
    }
  }

  private String buildMessage(FailureAnalysis failureAnalysis) {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format("%n%n"));
    builder.append(String.format("***************************%n"));
    builder.append(String.format("APPLICATION FAILED TO START%n"));
    builder.append(String.format("***************************%n%n"));
    builder.append(String.format("Description:%n%n"));
    builder.append(String.format("%s%n", failureAnalysis.getDescription()));
    if (StringUtils.hasText(failureAnalysis.getAction())) {
      builder.append(String.format("%nAction:%n%n"));
      builder.append(String.format("%s%n", failureAnalysis.getAction()));
    }
    return builder.toString();
  }

}
