/*
 * Copyright 2002-present the original author or authors.
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

package infra.beans.factory.parsing;

import org.jspecify.annotations.Nullable;

import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Simple {@link ProblemReporter} implementation that exhibits fail-fast
 * behavior when errors are encountered.
 *
 * <p>The first error encountered results in a {@link BeanDefinitionParsingException}
 * being thrown.
 *
 * <p>Warnings are written to
 * {@link #setLogger(Logger) the log} for this class.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class FailFastProblemReporter implements ProblemReporter {

  private Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Set the {@link Logger logger} that is to be used to report warnings.
   * <p>If set to {@code null} then a default {@link Logger logger} set to
   * the name of the instance class will be used.
   *
   * @param logger the {@link Logger logger} that is to be used to report warnings
   */
  public void setLogger(@Nullable Logger logger) {
    this.logger = (logger != null ? logger : LoggerFactory.getLogger(getClass()));
  }

  /**
   * Throws a {@link BeanDefinitionParsingException} detailing the error
   * that has occurred.
   *
   * @param problem the source of the error
   */
  @Override
  public void fatal(Problem problem) {
    throw new BeanDefinitionParsingException(problem);
  }

  /**
   * Throws a {@link BeanDefinitionParsingException} detailing the error
   * that has occurred.
   *
   * @param problem the source of the error
   */
  @Override
  public void error(Problem problem) {
    throw new BeanDefinitionParsingException(problem);
  }

  /**
   * Writes the supplied {@link Problem} to the {@link Logger} at {@code WARN} level.
   *
   * @param problem the source of the warning
   */
  @Override
  public void warning(Problem problem) {
    logger.warn(problem, problem.getRootCause());
  }

}
