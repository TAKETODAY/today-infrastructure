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

package cn.taketoday.orm.mybatis.support;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import cn.taketoday.dao.support.DataAccessObjectSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;

/**
 * Convenient super class for MyBatis SqlSession data access objects. It gives you access to the template which can then
 * be used to execute SQL methods.
 * <p>
 * This class needs a SqlSessionTemplate or a SqlSessionFactory. If both are set the SqlSessionFactory will be ignored.
 * <p>
 *
 * @author Putthiphong Boonphong
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setSqlSessionFactory
 * @see #setSqlSessionTemplate
 * @see SqlSessionTemplate
 */
public abstract class SqlSessionDaoSupport extends DataAccessObjectSupport {

  private SqlSessionTemplate sqlSessionTemplate;

  /**
   * Set MyBatis SqlSessionFactory to be used by this DAO. Will automatically create SqlSessionTemplate for the given
   * SqlSessionFactory.
   *
   * @param sqlSessionFactory a factory of SqlSession
   */
  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    if (this.sqlSessionTemplate == null || sqlSessionFactory != this.sqlSessionTemplate.getSqlSessionFactory()) {
      this.sqlSessionTemplate = createSqlSessionTemplate(sqlSessionFactory);
    }
  }

  /**
   * Create a SqlSessionTemplate for the given SqlSessionFactory. Only invoked if populating the DAO with a
   * SqlSessionFactory reference!
   * <p>
   * Can be overridden in subclasses to provide a SqlSessionTemplate instance with different configuration, or a custom
   * SqlSessionTemplate subclass.
   *
   * @param sqlSessionFactory the MyBatis SqlSessionFactory to create a SqlSessionTemplate for
   * @return the new SqlSessionTemplate instance
   * @see #setSqlSessionFactory
   */
  @SuppressWarnings("WeakerAccess")
  protected SqlSessionTemplate createSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionTemplate(sqlSessionFactory);
  }

  /**
   * Return the MyBatis SqlSessionFactory used by this DAO.
   *
   * @return a factory of SqlSession
   */
  public final SqlSessionFactory getSqlSessionFactory() {
    return (this.sqlSessionTemplate != null ? this.sqlSessionTemplate.getSqlSessionFactory() : null);
  }

  /**
   * Set the SqlSessionTemplate for this DAO explicitly, as an alternative to specifying a SqlSessionFactory.
   *
   * @param sqlSessionTemplate a template of SqlSession
   * @see #setSqlSessionFactory
   */
  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  /**
   * Users should use this method to get a SqlSession to call its statement methods This is SqlSession is managed by
   * Framework. Users should not commit/rollback/close it because it will be automatically done.
   *
   * @return managed thread safe SqlSession
   */
  public SqlSession getSqlSession() {
    return this.sqlSessionTemplate;
  }

  /**
   * Return the SqlSessionTemplate for this DAO, pre-initialized with the SessionFactory or set explicitly.
   * <p>
   * <b>Note: The returned SqlSessionTemplate is a shared instance.</b> You may introspect its configuration, but not
   * modify the configuration (other than from within an {@link #initDao} implementation). Consider creating a custom
   * SqlSessionTemplate instance via {@code new SqlSessionTemplate(getSqlSessionFactory())}, in which case you're
   * allowed to customize the settings on the resulting instance.
   *
   * @return a template of SqlSession
   */
  public SqlSessionTemplate getSqlSessionTemplate() {
    return this.sqlSessionTemplate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void checkDaoConfig() {
    Assert.notNull(this.sqlSessionTemplate, "Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required");
  }

}
