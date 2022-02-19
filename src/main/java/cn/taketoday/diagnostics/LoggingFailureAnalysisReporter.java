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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.diagnostics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.taketoday.util.StringUtils;

/**
 * {@link FailureAnalysisReporter} that logs the failure analysis.
 *
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public final class LoggingFailureAnalysisReporter implements FailureAnalysisReporter {

  private static final Logger logger = LoggerFactory.getLogger(LoggingFailureAnalysisReporter.class);

  @Override
  public void report(FailureAnalysis failureAnalysis) {
    if (logger.isDebugEnabled()) {
      logger.debug("Application failed to start due to an exception", failureAnalysis.getCause());
    }
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
