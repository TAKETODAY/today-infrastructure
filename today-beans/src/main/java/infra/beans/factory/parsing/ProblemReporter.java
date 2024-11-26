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

package infra.beans.factory.parsing;

/**
 * SPI interface allowing tools and other external processes to handle errors
 * and warnings reported during bean definition parsing.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see Problem
 * @since 4.0
 */
public interface ProblemReporter {

  /**
   * Called when a fatal error is encountered during the parsing process.
   * <p>Implementations must treat the given problem as fatal,
   * i.e. they have to eventually raise an exception.
   *
   * @param problem the source of the error (never {@code null})
   */
  void fatal(Problem problem);

  /**
   * Called when an error is encountered during the parsing process.
   * <p>Implementations may choose to treat errors as fatal.
   *
   * @param problem the source of the error (never {@code null})
   */
  void error(Problem problem);

  /**
   * Called when a warning is raised during the parsing process.
   * <p>Warnings are <strong>never</strong> considered to be fatal.
   *
   * @param problem the source of the warning (never {@code null})
   */
  void warning(Problem problem);

}
