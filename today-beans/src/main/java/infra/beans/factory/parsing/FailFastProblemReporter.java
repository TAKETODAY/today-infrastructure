/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory.parsing;

import infra.lang.Nullable;
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
