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

package cn.taketoday.orm.hibernate5;

import org.hibernate.QueryException;

import cn.taketoday.dao.InvalidDataAccessResourceUsageException;

/**
 * Hibernate-specific subclass of InvalidDataAccessResourceUsageException,
 * thrown on invalid HQL query syntax.
 *
 * @author Juergen Hoeller
 * @see SessionFactoryUtils#convertHibernateAccessException
 * @since 4.0
 */
@SuppressWarnings("serial")
public class HibernateQueryException extends InvalidDataAccessResourceUsageException {

  public HibernateQueryException(QueryException ex) {
    super(ex.getMessage(), ex);
  }

  /**
   * Return the HQL query string that was invalid.
   */
  public String getQueryString() {
    return ((QueryException) getCause()).getQueryString();
  }

}
