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

import org.hibernate.HibernateException;

import cn.taketoday.dao.UncategorizedDataAccessException;
import cn.taketoday.lang.Nullable;

/**
 * Hibernate-specific subclass of UncategorizedDataAccessException,
 * for Hibernate system errors that do not match any concrete
 * {@code cn.taketoday.dao} exceptions.
 *
 * @author Juergen Hoeller
 * @see SessionFactoryUtils#convertHibernateAccessException
 * @since 4.0
 */
@SuppressWarnings("serial")
public class HibernateSystemException extends UncategorizedDataAccessException {

  /**
   * Create a new HibernateSystemException,
   * wrapping an arbitrary HibernateException.
   *
   * @param cause the HibernateException thrown
   */
  public HibernateSystemException(@Nullable HibernateException cause) {
    super(cause != null ? cause.getMessage() : null, cause);
  }

}
