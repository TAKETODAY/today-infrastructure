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

import cn.taketoday.orm.ObjectRetrievalFailureException;
import jakarta.persistence.EntityNotFoundException;

/**
 * JPA-specific subclass of ObjectRetrievalFailureException.
 * Converts JPA's EntityNotFoundException.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
 * @since 4.0
 */
public class JpaObjectRetrievalFailureException extends ObjectRetrievalFailureException {

  public JpaObjectRetrievalFailureException(EntityNotFoundException ex) {
    super(ex.getMessage(), ex);
  }

}
