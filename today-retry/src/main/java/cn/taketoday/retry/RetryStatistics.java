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

package cn.taketoday.retry;

/**
 * Interface for statistics reporting of retry attempts. Counts the number of retry
 * attempts, successes, errors (including retries), and aborts.
 *
 * @author Dave Syer
 */
public interface RetryStatistics {

  /**
   * @return the number of completed successful retry attempts.
   */
  int getCompleteCount();

  /**
   * Get the number of times a retry block has been entered, irrespective of how many
   * times the operation was retried.
   *
   * @return the number of retry blocks started.
   */
  int getStartedCount();

  /**
   * Get the number of errors detected, whether or not they resulted in a retry.
   *
   * @return the number of errors detected.
   */
  int getErrorCount();

  /**
   * Get the number of times a block failed to complete successfully, even after retry.
   *
   * @return the number of retry attempts that failed overall.
   */
  int getAbortCount();

  /**
   * Get the number of times a recovery callback was applied.
   *
   * @return the number of recovered attempts.
   */
  int getRecoveryCount();

  /**
   * Get an identifier for the retry block for reporting purposes.
   *
   * @return an identifier for the block.
   */
  String getName();

}
