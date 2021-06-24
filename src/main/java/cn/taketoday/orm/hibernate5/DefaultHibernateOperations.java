/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.orm.hibernate5;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.GenericTypeResolver;
import cn.taketoday.jdbc.PersistenceException;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author TODAY <br>
 * 2018-09-15 15:31
 */
public class DefaultHibernateOperations<T> implements HibernateOperations<T>, InitializingBean {
  private static final Logger log = LoggerFactory.getLogger(DefaultHibernateOperations.class);

  @Autowired
  private SessionFactory sessionFactory;

  private Integer fetchSize = null;
  private Integer maxResults = null;
  private boolean cacheable = true;

  private String queryCacheRegion;

  private Class<T> beanClass;
  /** Bean class simple name */
  private String beanClassName;

  @SuppressWarnings("unchecked")
  public DefaultHibernateOperations() {
    final Class<?> type = GenericTypeResolver.resolveTypeArgument(getClass(), DefaultHibernateOperations.class);
    setBeanClass((Class<T>) type);
  }

  public DefaultHibernateOperations(Class<T> beanClass) {
    setBeanClass(beanClass);
  }

  @Override
  public void afterPropertiesSet() {
    if (getSessionFactory() == null) {
      throw new ConfigurationException("SessionFactory is required");
    }
  }

  public <Q> Query<Q> setParameter(Query<Q> query, Object[] params) {
    if (params != null) {
      for (int i = 0; i < params.length; i++) {
        query.setParameter(i, params[i]);
      }
    }
    return query;
  }

  @Override
  public final <R> R execute(HibernateCallback<R> session) throws PersistenceException {
    return doExecute(session);
  }

  protected <H> H doExecute(HibernateCallback<H> action) throws PersistenceException {
    final Session session = obtainSession(sessionFactory);
    Transaction transaction = null;
    try {
      transaction = session.beginTransaction();
      return action.doInHibernate(session);
    }
    catch (HibernateException ex) {
      log.error("Could not execute sql", ex);
      try {
        if (transaction != null) {
          transaction.rollback();
          transaction = null;
        }
      }
      catch (Throwable ex2) {
        log.error("Could not rollback Session after failed transaction begin", ex2);
      }
      finally {
        session.close();
      }
      throw ex;
    }
    finally {
      if (transaction != null) {
        log.debug("Commit the transaction -> [{}].", transaction);
        transaction.commit();
      }
    }
  }

  protected Session obtainSession(final SessionFactory sessionFactory) {
    Session session = sessionFactory.getCurrentSession();
    if (session == null) {
      log.debug("Open new session");
      session = sessionFactory.openSession();
      session.setHibernateFlushMode(FlushMode.MANUAL);
    }
    return session;
  }

  @Override
  public T get(Serializable id) {
    return get(id, null);
  }

  @Override
  public T get(Serializable id, LockMode lockMode) {
    return execute(s -> lockMode != null ? s.get(beanClass, id, new LockOptions(lockMode)) : s.get(beanClass, id));
  }

  @Override
  public T load(Serializable id) {
    return load(id, null);
  }

  @Override
  public T load(Serializable id, LockMode lockMode) {
    return execute(s -> lockMode != null ? s.load(beanClass, id, new LockOptions(lockMode)) : s.load(beanClass, id));
  }

  @Override
  @SuppressWarnings({ "unchecked", "deprecation" })
  public List<T> findAll() {
    return execute(session -> {
      return prepareCriteria(session.createCriteria(beanClass)//
                                     .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY))//
              .list();
    });
  }

  @Override
  public void load(Object entity, Serializable id) {
    execute(session -> {
      session.load(entity, id);
      return null;
    });
  }

  @Override
  public boolean contains(Object entity) {
    return execute(session -> session.contains(entity));
  }

  @Override
  public Filter enableFilter(String filterName) throws IllegalStateException {
    final Session session = obtainSession(sessionFactory);
    Filter filter = session.getEnabledFilter(filterName);
    if (filter == null) {
      filter = session.enableFilter(filterName);
    }
    return filter;
  }

  @Override
  public Serializable save(Object entity) {
    return execute(session -> session.save(entity));
  }

  @Override
  public void saveOrUpdate(Object entity) {
    execute(session -> {
      session.saveOrUpdate(entity);
      return null;
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public T merge(T entity) {
    return execute(session -> (T) session.merge(entity));
  }

  @Override
  @SuppressWarnings("unchecked")
  public T merge(String entityName, T entity) {
    return execute(session -> (T) session.merge(entityName, entity));
  }

  @Override
  public void delete(Object entity) {
    delete(entity, null);
  }

  @Override
  public void delete(Object entity, LockMode lockMode) {
    execute(session -> {
      if (lockMode != null) {
        session.buildLockRequest(new LockOptions(lockMode)).lock(entity);
      }
      session.delete(entity);
      return null;
    });
  }

  @Override
  public void delete(String entityName, Object entity) {
    delete(entityName, entity, null);
  }

  @Override
  public void delete(String entityName, Object entity, LockMode lockMode) {
    execute(session -> {
      if (lockMode != null) {
        session.buildLockRequest(new LockOptions(lockMode)).lock(entityName, entity);
      }
      session.delete(entityName, entity);
      return null;
    });
  }

  @Override
  public void deleteAll(Collection<?> entities) {
    execute(session -> {
      for (final Object entity : entities) {
        session.delete(entity);
      }
      return null;
    });
  }

  @Override
  public void flush() {
    execute(session -> {
      session.flush();
      return null;
    });
  }

  @Override
  public void clear() {
    execute(session -> {
      session.clear();
      return null;
    });
  }

  @Override
  public void closeIterator(Iterator<?> it) {
    try {
      Hibernate.close(it);
    }
    catch (HibernateException ignored) {}
  }

  protected <Q> Query<Q> prepareQuery(Query<Q> query) {
    query.setCacheable(cacheable);
    if (queryCacheRegion != null) {
      query.setCacheRegion(queryCacheRegion);
    }
    if (fetchSize != null) {
      query.setFetchSize(fetchSize);
    }
    if (maxResults != null) {
      return query.setMaxResults(maxResults);
    }
    return query;
  }

  protected Criteria prepareCriteria(Criteria criteria) {
    criteria.setCacheable(cacheable);

    if (queryCacheRegion != null) {
      criteria.setCacheRegion(queryCacheRegion);
    }
    if (fetchSize != null) {
      criteria.setFetchSize(fetchSize);
    }
    if (maxResults != null) {
      criteria.setMaxResults(maxResults);
    }
    return criteria;
  }

  @Override
  public Long getTotalRecord() {
    return execute(session -> session.createQuery("select count(*) from " + beanClassName, Long.class)
            .uniqueResult()//
    );
  }

  @Override
  public void update(T entity) {
    execute(session -> {
      session.update(entity);
      return null;
    });
  }

  @Override
  public Integer updateOne(String columnName, String primaryKey, Object columnValue, Object keyValue) {
    return execute(session -> session.createQuery(
            new StringBuilder()//
                    .append("update ")
                    .append(beanClassName)
                    .append(" set ")
                    .append(columnName)
                    .append("=:columnName where ")
                    .append(primaryKey)
                    .append("=:keyValue")
                    .toString())
            .setParameter("columnName", columnValue)
            .setParameter("keyValue", keyValue)
            .executeUpdate()//
    );
  }

  @Override
  public Long getTotalRecord(Object[] params, String condition) {
    return execute(session -> setParameter(session.createQuery(
            new StringBuilder("select count(*) from ")//
                    .append(beanClassName)//
                    .append(" where ")//
                    .append(condition)//
                    .toString(),
            Long.class), params).setCacheable(true).uniqueResult()//
    );
  }

  @Override
  public Integer update(String columnNames, Object[] params, String primaryKey) {

    return execute(session -> {
      Query<?> query = session.createQuery(
              new StringBuilder("update ")
                      .append(beanClassName)
                      .append(" set ")
                      .append(columnNames)
                      .append(" where ")
                      .append(primaryKey)
                      .append("=:primaryKey")
                      .toString()
      );

      for (int i = 0; i < params.length - 1; i++) {
        query.setParameter(i, params[i]);
      }

      return query.setParameter("primaryKey", params[params.length - 1])//
              .executeUpdate();
    });
  }

  @Override
  public Integer insertBySql(Object[] params, String sql) {
    return execute(session -> setParameter(session.createNativeQuery(sql, Integer.class), params).executeUpdate());
  }

  @Override
  public Integer insertBySql(String sql) {
    return execute(session -> session.createNativeQuery(sql).executeUpdate());
  }

  @Override
  public Integer deleteById(Serializable id) {
    return execute(session -> session.createQuery("DELETE FROM " + beanClassName + " WHERE id=:id")//
            .setParameter("id", id).executeUpdate()//
    );
  }

  @Override
  public void saveAll(List<T> t) {
    execute(session -> {
      session.save(t);
      return null;
    });
  }

  @Override
  public List<T> find(int pageNow, int pageSize) {
    return execute(session -> session.createQuery("from " + beanClassName, beanClass)//
            .setFirstResult((pageNow - 1) * pageSize)//
            .setMaxResults(pageSize)//
            .list()//
    );
  }

  @Override
  public List<T> find(int pageNow, int pageSize, Object[] params, String condition) {
    return execute(session -> setParameter(session.createQuery(where(condition), beanClass), params)
            .setFirstResult((pageNow - 1) * pageSize)
            .setMaxResults(pageSize)
            .list()
    );
  }

  @Override
  public List<T> findCondition(Object[] params, String condition) {
    return execute(session -> setParameter(session.createQuery(where(condition), beanClass), params).list());
  }

  @Override
  public List<T> find(Object[] params, String queryString) {
    return execute(session -> setParameter(prepareQuery(session.createQuery(queryString, beanClass)), params).list());
  }

  @Override
  public <X> X query(String queryString, Object[] params, Class<X> type) {
    return execute(session -> setParameter(prepareQuery(session.createQuery(queryString, type)), params).uniqueResult());
  }

  @Override
  public List<String> query(Object[] params, String queryString) {
    return execute(session -> setParameter(prepareQuery(session.createQuery(queryString, String.class)), params).list());
  }

  @Override
  public T uniqueResult(Object[] params, String queryString) {
    return execute(session -> setParameter(prepareQuery(session.createQuery(queryString, beanClass)), params).uniqueResult());
  }

  @Override
  public T uniqueCondition(Object[] params, String condition) {
    return execute(session -> setParameter(prepareQuery(session.createQuery(where(condition), beanClass)), params)
            .uniqueResult());
  }

  @Override
  public Integer updateOne(Object[] params, String sql) {
    return execute(session -> setParameter(session.createNativeQuery(sql, Integer.class), params)
            .executeUpdate()
    );
  }

  @Override
  public List<T> orderBy(int pageNow, int pageSize, Object[] params, String by) {
    return execute(session -> setParameter(session.createQuery(orderBy(by), beanClass), params)
            .setFirstResult((pageNow - 1) * pageSize)
            .setMaxResults(pageSize).list()
    );
  }

  protected final String where(String condition) {
    return new StringBuilder("from ")
            .append(beanClassName)
            .append(" where ")
            .append(condition)
            .toString();
  }

  protected final String orderBy(String by) {
    return new StringBuilder("from ")
            .append(beanClassName)
            .append(" order by ")
            .append(by)
            .toString();
  }

  // -------------------------------

  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public final Integer getFetchSize() {
    return fetchSize;
  }

  public void setFetchSize(Integer fetchSize) {
    this.fetchSize = fetchSize;
  }

  public Integer getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(Integer maxResults) {
    this.maxResults = maxResults;
  }

  public boolean isCacheable() {
    return cacheable;
  }

  public void setCacheable(boolean cacheable) {
    this.cacheable = cacheable;
  }

  public String getQueryCacheRegion() {
    return queryCacheRegion;
  }

  public void setQueryCacheRegion(String queryCacheRegion) {
    this.queryCacheRegion = queryCacheRegion;
  }

  public Class<T> getBeanClass() {
    return beanClass;
  }

  public void setBeanClass(Class<T> beanClass) {
    this.beanClass = beanClass;
    this.beanClassName = beanClass.getSimpleName();
  }

  public String getBeanClassName() {
    return beanClassName;
  }
}
