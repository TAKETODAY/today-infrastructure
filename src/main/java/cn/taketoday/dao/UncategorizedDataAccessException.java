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

package cn.taketoday.dao;

import cn.taketoday.lang.Nullable;

/**
 * Normal superclass when we can't distinguish anything more specific
 * than "something went wrong with the underlying resource": for example,
 * an SQLException from JDBC we can't pinpoint more precisely.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public abstract class UncategorizedDataAccessException extends NonTransientDataAccessException {

  /**
   * Constructor for UncategorizedDataAccessException.
   *
   * @param msg the detail message
   * @param cause the exception thrown by underlying data access API
   */
  public UncategorizedDataAccessException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
