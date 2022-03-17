/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.support.incrementer;

import cn.taketoday.dao.DataAccessException;

/**
 * Interface that defines contract of incrementing any data store field's
 * maximum value. Works much like a sequence number generator.
 *
 * <p>Typical implementations may use standard SQL, native RDBMS sequences
 * or Stored Procedures to do the job.
 *
 * @author Dmitriy Kopylenko
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 */
public interface DataFieldMaxValueIncrementer {

  /**
   * Increment the data store field's max value as int.
   *
   * @return int next data store value such as <b>max + 1</b>
   * @throws cn.taketoday.dao.DataAccessException in case of errors
   */
  int nextIntValue() throws DataAccessException;

  /**
   * Increment the data store field's max value as long.
   *
   * @return int next data store value such as <b>max + 1</b>
   * @throws cn.taketoday.dao.DataAccessException in case of errors
   */
  long nextLongValue() throws DataAccessException;

  /**
   * Increment the data store field's max value as String.
   *
   * @return next data store value such as <b>max + 1</b>
   * @throws cn.taketoday.dao.DataAccessException in case of errors
   */
  String nextStringValue() throws DataAccessException;

}
