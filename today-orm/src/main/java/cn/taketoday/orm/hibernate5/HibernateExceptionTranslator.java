/*
 * Copyright 2017 - 2023 the original author or authors.
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
import org.hibernate.JDBCException;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.jdbc.support.SQLExceptionTranslator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.jpa.EntityManagerFactoryUtils;
import jakarta.persistence.PersistenceException;

/**
 * {@link PersistenceExceptionTranslator} capable of translating {@link HibernateException}
 * instances to Framework's {@link DataAccessException} hierarchy. As of Hibernate 5.2,
 * it also converts standard JPA {@link PersistenceException} instances.
 *
 * <p>Extended by {@link LocalSessionFactoryBean}, so there is no need to declare this
 * translator in addition to a {@code LocalSessionFactoryBean}.
 *
 * <p>When configuring the container with {@code @Configuration} classes, a {@code @Bean}
 * of this type must be registered manually.
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * @see SessionFactoryUtils#convertHibernateAccessException(HibernateException)
 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible(RuntimeException)
 * @since 4.0
 */
public class HibernateExceptionTranslator implements PersistenceExceptionTranslator {

  @Nullable
  private SQLExceptionTranslator jdbcExceptionTranslator;

  /**
   * Set the JDBC exception translator for Hibernate exception translation purposes.
   * <p>Applied to any detected {@link java.sql.SQLException} root cause of a Hibernate
   * {@link JDBCException}, overriding Hibernate's own {@code SQLException} translation
   * (which is based on a Hibernate Dialect for a specific target database).
   *
   * @see java.sql.SQLException
   * @see JDBCException
   * @see cn.taketoday.jdbc.support.SQLErrorCodeSQLExceptionTranslator
   * @see cn.taketoday.jdbc.support.SQLStateSQLExceptionTranslator
   * @since 4.0
   */
  public void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator) {
    this.jdbcExceptionTranslator = jdbcExceptionTranslator;
  }

  @Override
  @Nullable
  public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
    if (ex instanceof HibernateException) {
      return convertHibernateAccessException((HibernateException) ex);
    }
    if (ex instanceof PersistenceException) {
      if (ex.getCause() instanceof HibernateException) {
        return convertHibernateAccessException((HibernateException) ex.getCause());
      }
      return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
    }
    return null;
  }

  /**
   * Convert the given HibernateException to an appropriate exception from the
   * {@code cn.taketoday.dao} hierarchy.
   * <p>Will automatically apply a specified SQLExceptionTranslator to a
   * Hibernate JDBCException, otherwise rely on Hibernate's default translation.
   *
   * @param ex the HibernateException that occurred
   * @return a corresponding DataAccessException
   * @see SessionFactoryUtils#convertHibernateAccessException
   */
  protected DataAccessException convertHibernateAccessException(HibernateException ex) {
    if (jdbcExceptionTranslator != null && ex instanceof JDBCException jdbcEx) {
      DataAccessException dae = jdbcExceptionTranslator.translate(
              "Hibernate operation: " + jdbcEx.getMessage(), jdbcEx.getSQL(), jdbcEx.getSQLException());
      if (dae != null) {
        return dae;
      }
    }
    return SessionFactoryUtils.convertHibernateAccessException(ex);
  }

}
