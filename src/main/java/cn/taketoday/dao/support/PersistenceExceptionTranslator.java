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

package cn.taketoday.dao.support;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.lang.Nullable;

/**
 * Interface implemented by Framework integrations with data access technologies
 * that throw runtime exceptions, such as JPA and Hibernate.
 *
 * <p>This allows consistent usage of combined exception translation functionality,
 * without forcing a single translator to understand every single possible type
 * of exception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
@FunctionalInterface
public interface PersistenceExceptionTranslator {

  /**
   * Translate the given runtime exception thrown by a persistence framework to a
   * corresponding exception from  generic
   * {@link DataAccessException} hierarchy, if possible.
   * <p>Do not translate exceptions that are not understood by this translator:
   * for example, if coming from another persistence framework, or resulting
   * from user code or otherwise unrelated to persistence.
   * <p>Of particular importance is the correct translation to
   * DataIntegrityViolationException, for example on constraint violation.
   * Implementations may use Framework JDBC's sophisticated exception translation
   * to provide further information in the event of SQLException as a root cause.
   *
   * @param ex a RuntimeException to translate
   * @return the corresponding DataAccessException (or {@code null} if the
   * exception could not be translated, as in this case it may result from
   * user code rather than from an actual persistence problem)
   * @see cn.taketoday.dao.DataIntegrityViolationException
   * @see cn.taketoday.jdbc.support.SQLExceptionTranslator
   */
  @Nullable
  DataAccessException translateExceptionIfPossible(RuntimeException ex);

}
