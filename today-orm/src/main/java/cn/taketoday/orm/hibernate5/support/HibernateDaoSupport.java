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

package cn.taketoday.orm.hibernate5.support;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.support.DataAccessObjectSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.hibernate5.HibernateTemplate;

/**
 * Convenient super class for Hibernate-based data access objects.
 *
 * <p>Requires a {@link SessionFactory} to be set, providing a
 * {@link HibernateTemplate} based on it to
 * subclasses through the {@link #getHibernateTemplate()} method.
 * Can alternatively be initialized directly with a HibernateTemplate,
 * in order to reuse the latter's settings such as the SessionFactory,
 * exception translator, flush mode, etc.
 *
 * <p>This class will create its own HibernateTemplate instance if a SessionFactory
 * is passed in. The "allowCreate" flag on that HibernateTemplate will be "true"
 * by default. A custom HibernateTemplate instance can be used through overriding
 * {@link #createHibernateTemplate}.
 *
 * <p><b>NOTE: Hibernate access code can also be coded in plain Hibernate style.
 * Hence, for newly started projects, consider adopting the standard Hibernate
 * style of coding data access objects instead, based on
 * {@link SessionFactory#getCurrentSession()}.
 * This HibernateTemplate primarily exists as a migration helper for Hibernate 3
 * based data access code, to benefit from bug fixes in Hibernate 5.x.</b>
 *
 * @author Juergen Hoeller
 * @see #setSessionFactory
 * @see #getHibernateTemplate
 * @see HibernateTemplate
 * @since 4.0
 */
public abstract class HibernateDaoSupport extends DataAccessObjectSupport {

  @Nullable
  private HibernateTemplate hibernateTemplate;

  /**
   * Set the Hibernate SessionFactory to be used by this DAO.
   * Will automatically create a HibernateTemplate for the given SessionFactory.
   *
   * @see #createHibernateTemplate
   * @see #setHibernateTemplate
   */
  public final void setSessionFactory(SessionFactory sessionFactory) {
    if (this.hibernateTemplate == null || sessionFactory != this.hibernateTemplate.getSessionFactory()) {
      this.hibernateTemplate = createHibernateTemplate(sessionFactory);
    }
  }

  /**
   * Create a HibernateTemplate for the given SessionFactory.
   * Only invoked if populating the DAO with a SessionFactory reference!
   * <p>Can be overridden in subclasses to provide a HibernateTemplate instance
   * with different configuration, or a custom HibernateTemplate subclass.
   *
   * @param sessionFactory the Hibernate SessionFactory to create a HibernateTemplate for
   * @return the new HibernateTemplate instance
   * @see #setSessionFactory
   */
  protected HibernateTemplate createHibernateTemplate(SessionFactory sessionFactory) {
    return new HibernateTemplate(sessionFactory);
  }

  /**
   * Return the Hibernate SessionFactory used by this DAO.
   */
  @Nullable
  public final SessionFactory getSessionFactory() {
    return (this.hibernateTemplate != null ? this.hibernateTemplate.getSessionFactory() : null);
  }

  /**
   * Set the HibernateTemplate for this DAO explicitly,
   * as an alternative to specifying a SessionFactory.
   *
   * @see #setSessionFactory
   */
  public final void setHibernateTemplate(@Nullable HibernateTemplate hibernateTemplate) {
    this.hibernateTemplate = hibernateTemplate;
  }

  /**
   * Return the HibernateTemplate for this DAO,
   * pre-initialized with the SessionFactory or set explicitly.
   * <p><b>Note: The returned HibernateTemplate is a shared instance.</b>
   * You may introspect its configuration, but not modify the configuration
   * (other than from within an {@link #initDao} implementation).
   * Consider creating a custom HibernateTemplate instance via
   * {@code new HibernateTemplate(getSessionFactory())}, in which case
   * you're allowed to customize the settings on the resulting instance.
   */
  @Nullable
  public final HibernateTemplate getHibernateTemplate() {
    return this.hibernateTemplate;
  }

  @Override
  protected final void checkDaoConfig() {
    if (this.hibernateTemplate == null) {
      throw new IllegalArgumentException("'sessionFactory' or 'hibernateTemplate' is required");
    }
  }

  /**
   * Conveniently obtain the current Hibernate Session.
   *
   * @return the Hibernate Session
   * @throws DataAccessResourceFailureException if the Session couldn't be created
   * @see SessionFactory#getCurrentSession()
   */
  protected final Session currentSession() throws DataAccessResourceFailureException {
    SessionFactory sessionFactory = getSessionFactory();
    Assert.state(sessionFactory != null, "No SessionFactory set");
    return sessionFactory.getCurrentSession();
  }

}
