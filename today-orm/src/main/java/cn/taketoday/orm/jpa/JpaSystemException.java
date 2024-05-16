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

package cn.taketoday.orm.jpa;

import cn.taketoday.dao.UncategorizedDataAccessException;

/**
 * JPA-specific subclass of UncategorizedDataAccessException,
 * for JPA system errors that do not match any concrete
 * {@code cn.taketoday.dao} exceptions.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
 * @since 4.0
 */
public class JpaSystemException extends UncategorizedDataAccessException {

  public JpaSystemException(RuntimeException ex) {
    super(ex.getMessage(), ex);
  }

}
