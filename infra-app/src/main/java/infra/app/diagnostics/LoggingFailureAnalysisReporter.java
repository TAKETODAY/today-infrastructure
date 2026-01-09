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
