/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.dao.support;

import infra.beans.factory.BeanInitializationException;
import infra.beans.factory.InitializingBean;

/**
 * Generic base class for DAOs, defining template methods for DAO initialization.
 *
 * <p>Extended by  specific DAO support classes, such as:
 * JdbcDaoSupport, JdoDaoSupport, etc.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see infra.jdbc.core.support.JdbcDataAccessObjectSupport
 * @since 4.0
 */
public abstract class DataAccessObjectSupport implements InitializingBean {

  @Override
  public final void afterPropertiesSet() throws IllegalArgumentException, BeanInitializationException {
    // Let abstract subclasses check their configuration.
    checkDaoConfig();

    // Let concrete implementations initialize themselves.
    try {
      initDao();
    }
    catch (Exception ex) {
      throw new BeanInitializationException("Initialization of DAO failed", ex);
    }
  }

  /**
   * Abstract subclasses must override this to check their configuration.
   * <p>Implementors should be marked as {@code final} if concrete subclasses
   * are not supposed to override this template method themselves.
   *
   * @throws IllegalArgumentException in case of illegal configuration
   */
  protected abstract void checkDaoConfig() throws IllegalArgumentException;

  /**
   * Concrete subclasses can override this for custom initialization behavior.
   * Gets called after population of this instance's bean properties.
   *
   * @throws Exception if DAO initialization fails
   * (will be rethrown as a BeanInitializationException)
   * @see BeanInitializationException
   */
  protected void initDao() throws Exception {
  }

}
