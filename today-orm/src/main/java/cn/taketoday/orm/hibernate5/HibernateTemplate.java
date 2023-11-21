/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Example;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.support.ResourceHolderSupport;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

/**
 * Helper class that simplifies Hibernate data access code. Automatically
 * converts HibernateExceptions into DataAccessExceptions, following the
 * {@code cn.taketoday.dao} exception hierarchy.
 *
 * <p>The central method is {@code execute}, supporting Hibernate access code
 * implementing the {@link HibernateCallback} interface. It provides Hibernate Session
 * handling such that neither the HibernateCallback implementation nor the calling
 * code needs to explicitly care about retrieving/closing Hibernate Sessions,
 * or handling Session lifecycle exceptions. For typical single step actions,
 * there are various convenience methods (find, load, saveOrUpdate, delete).
 *
 * <p>Can be used within a service implementation via direct instantiation
 * with a SessionFactory reference, or get prepared in an application context
 * and given to services as bean reference. Note: The SessionFactory should
 * always be configured as bean in the application context, in the first case
 * given to the service directly, in the second case to the prepared template.
 *
 * <p><b>NOTE: Hibernate access code can also be coded against the native Hibernate
 * {@link Session}. Hence, for newly started projects, consider adopting the standard
 * Hibernate style of coding against {@link SessionFactory#getCurrentSession()}.
 * Alternatively, use {@link #execute(HibernateCallback)} with Java 8 lambda code blocks
 * against the callback-provided {@code Session} which results in elegant code as well,
 * decoupled from the Hibernate Session lifecycle. The remaining operations on this
 * HibernateTemplate are deprecated in the meantime and primarily exist as a migration
 * helper for older Hibernate 3.x/4.x data access code in existing applications.</b>
 *
 * @author Juergen Hoeller
 * @see #setSessionFactory
 * @see HibernateCallback
 * @see Session
 * @see LocalSessionFactoryBean
 * @see HibernateTransactionManager
 * @since 4.0
 */
public class HibernateTemplate implements HibernateOperations, InitializingBean {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private SessionFactory sessionFactory;

  @Nullable
  private String[] filterNames;

  private boolean exposeNativeSession = false;

  private boolean checkWriteOperations = true;

  private boolean cacheQueries = false;

  @Nullable
  private String queryCacheRegion;

  private int fetchSize = 0;

  private int maxResults = 0;

  /**
   * Create a new HibernateTemplate instance.
   */
  public HibernateTemplate() { }

  /**
   * Create a new HibernateTemplate instance.
   *
   * @param sessionFactory the SessionFactory to create Sessions with
   */
  public HibernateTemplate(SessionFactory sessionFactory) {
    setSessionFactory(sessionFactory);
    afterPropertiesSet();
  }

  /**
   * Set the Hibernate SessionFactory that should be used to create
   * Hibernate Sessions.
   */
  public void setSessionFactory(@Nullable SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /**
   * Return the Hibernate SessionFactory that should be used to create
   * Hibernate Sessions.
   */
  @Nullable
  public SessionFactory getSessionFactory() {
    return this.sessionFactory;
  }

  /**
   * Obtain the SessionFactory for actual use.
   *
   * @return the SessionFactory (never {@code null})
   * @throws IllegalStateException in case of no SessionFactory set
   * @since 4.0
   */
  protected final SessionFactory obtainSessionFactory() {
    SessionFactory sessionFactory = getSessionFactory();
    Assert.state(sessionFactory != null, "No SessionFactory set");
    return sessionFactory;
  }

  /**
   * Set one or more names of Hibernate filters to be activated for all
   * Sessions that this accessor works with.
   * <p>Each of those filters will be enabled at the beginning of each
   * operation and correspondingly disabled at the end of the operation.
   * This will work for newly opened Sessions as well as for existing
   * Sessions (for example, within a transaction).
   *
   * @see #enableFilters(Session)
   * @see Session#enableFilter(String)
   */
  public void setFilterNames(@Nullable String... filterNames) {
    this.filterNames = filterNames;
  }

  /**
   * Return the names of Hibernate filters to be activated, if any.
   */
  @Nullable
  public String[] getFilterNames() {
    return this.filterNames;
  }

  /**
   * Set whether to expose the native Hibernate Session to
   * HibernateCallback code.
   * <p>Default is "false": a Session proxy will be returned, suppressing
   * {@code close} calls and automatically applying query cache
   * settings and transaction timeouts.
   *
   * @see HibernateCallback
   * @see Session
   * @see #setCacheQueries
   * @see #setQueryCacheRegion
   * @see #prepareQuery
   * @see #prepareCriteria
   */
  public void setExposeNativeSession(boolean exposeNativeSession) {
    this.exposeNativeSession = exposeNativeSession;
  }

  /**
   * Return whether to expose the native Hibernate Session to
   * HibernateCallback code, or rather a Session proxy.
   */
  public boolean isExposeNativeSession() {
    return this.exposeNativeSession;
  }

  /**
   * Set whether to check that the Hibernate Session is not in read-only mode
   * in case of write operations (save/update/delete).
   * <p>Default is "true", for fail-fast behavior when attempting write operations
   * within a read-only transaction. Turn this off to allow save/update/delete
   * on a Session with flush mode MANUAL.
   *
   * @see #checkWriteOperationAllowed
   * @see cn.taketoday.transaction.TransactionDefinition#isReadOnly
   */
  public void setCheckWriteOperations(boolean checkWriteOperations) {
    this.checkWriteOperations = checkWriteOperations;
  }

  /**
   * Return whether to check that the Hibernate Session is not in read-only
   * mode in case of write operations (save/update/delete).
   */
  public boolean isCheckWriteOperations() {
    return this.checkWriteOperations;
  }

  /**
   * Set whether to cache all queries executed by this template.
   * <p>If this is "true", all Query and Criteria objects created by
   * this template will be marked as cacheable (including all
   * queries through find methods).
   * <p>To specify the query region to be used for queries cached
   * by this template, set the "queryCacheRegion" property.
   *
   * @see #setQueryCacheRegion
   * @see Query#setCacheable
   * @see Criteria#setCacheable
   */
  public void setCacheQueries(boolean cacheQueries) {
    this.cacheQueries = cacheQueries;
  }

  /**
   * Return whether to cache all queries executed by this template.
   */
  public boolean isCacheQueries() {
    return this.cacheQueries;
  }

  /**
   * Set the name of the cache region for queries executed by this template.
   * <p>If this is specified, it will be applied to all Query and Criteria objects
   * created by this template (including all queries through find methods).
   * <p>The cache region will not take effect unless queries created by this
   * template are configured to be cached via the "cacheQueries" property.
   *
   * @see #setCacheQueries
   * @see Query#setCacheRegion
   * @see Criteria#setCacheRegion
   */
  public void setQueryCacheRegion(@Nullable String queryCacheRegion) {
    this.queryCacheRegion = queryCacheRegion;
  }

  /**
   * Return the name of the cache region for queries executed by this template.
   */
  @Nullable
  public String getQueryCacheRegion() {
    return this.queryCacheRegion;
  }

  /**
   * Set the fetch size for this HibernateTemplate. This is important for processing
   * large result sets: Setting this higher than the default value will increase
   * processing speed at the cost of memory consumption; setting this lower can
   * avoid transferring row data that will never be read by the application.
   * <p>Default is 0, indicating to use the JDBC driver's default.
   */
  public void setFetchSize(int fetchSize) {
    this.fetchSize = fetchSize;
  }

  /**
   * Return the fetch size specified for this HibernateTemplate.
   */
  public int getFetchSize() {
    return this.fetchSize;
  }

  /**
   * Set the maximum number of rows for this HibernateTemplate. This is important
   * for processing subsets of large result sets, avoiding to read and hold
   * the entire result set in the database or in the JDBC driver if we're
   * never interested in the entire result in the first place (for example,
   * when performing searches that might return a large number of matches).
   * <p>Default is 0, indicating to use the JDBC driver's default.
   */
  public void setMaxResults(int maxResults) {
    this.maxResults = maxResults;
  }

  /**
   * Return the maximum number of rows specified for this HibernateTemplate.
   */
  public int getMaxResults() {
    return this.maxResults;
  }

  @Override
  public void afterPropertiesSet() {
    if (getSessionFactory() == null) {
      throw new IllegalArgumentException("Property 'sessionFactory' is required");
    }
  }

  @Override
  @Nullable
  public <T> T execute(HibernateCallback<T> action) throws DataAccessException {
    return doExecute(action, false);
  }

  /**
   * Execute the action specified by the given action object within a
   * native {@link Session}.
   * <p>This execute variant overrides the template-wide
   * {@link #isExposeNativeSession() "exposeNativeSession"} setting.
   *
   * @param action callback object that specifies the Hibernate action
   * @return a result object returned by the action, or {@code null}
   * @throws DataAccessException in case of Hibernate errors
   */
  @Nullable
  public <T> T executeWithNativeSession(HibernateCallback<T> action) {
    return doExecute(action, true);
  }

  /**
   * Execute the action specified by the given action object within a Session.
   *
   * @param action callback object that specifies the Hibernate action
   * @param enforceNativeSession whether to enforce exposure of the native
   * Hibernate Session to callback code
   * @return a result object returned by the action, or {@code null}
   * @throws DataAccessException in case of Hibernate errors
   */
  @Nullable
  protected <T> T doExecute(HibernateCallback<T> action, boolean enforceNativeSession) throws DataAccessException {
    Assert.notNull(action, "Callback object is required");

    Session session = null;
    boolean isNew = false;
    try {
      session = obtainSessionFactory().getCurrentSession();
    }
    catch (HibernateException ex) {
      logger.debug("Could not retrieve pre-bound Hibernate session", ex);
    }
    if (session == null) {
      session = obtainSessionFactory().openSession();
      session.setHibernateFlushMode(FlushMode.MANUAL);
      isNew = true;
    }

    try {
      enableFilters(session);
      Session sessionToExpose = enforceNativeSession || isExposeNativeSession()
                                ? session : createSessionProxy(session);
      return action.doInHibernate(sessionToExpose);
    }
    catch (HibernateException ex) {
      throw SessionFactoryUtils.convertHibernateAccessException(ex);
    }
    catch (PersistenceException ex) {
      if (ex.getCause() instanceof HibernateException) {
        throw SessionFactoryUtils.convertHibernateAccessException((HibernateException) ex.getCause());
      }
      throw ex;
    }
    catch (RuntimeException ex) {
      // Callback code threw application exception...
      throw ex;
    }
    finally {
      if (isNew) {
        SessionFactoryUtils.closeSession(session);
      }
      else {
        disableFilters(session);
      }
    }
  }

  /**
   * Create a close-suppressing proxy for the given Hibernate Session.
   * The proxy also prepares returned Query and Criteria objects.
   *
   * @param session the Hibernate Session to create a proxy for
   * @return the Session proxy
   * @see Session#close()
   * @see #prepareQuery
   * @see #prepareCriteria
   */
  protected Session createSessionProxy(Session session) {
    if (session instanceof SessionImplementor) {
      return new CloseSuppressingSessionProxy((SessionImplementor) session);
    }
    return (Session) Proxy.newProxyInstance(
            session.getClass().getClassLoader(), new Class<?>[] { Session.class },
            new CloseSuppressingInvocationHandler(session));
  }

  /**
   * Enable the specified filters on the given Session.
   *
   * @param session the current Hibernate Session
   * @see #setFilterNames
   * @see Session#enableFilter(String)
   */
  protected void enableFilters(Session session) {
    String[] filterNames = getFilterNames();
    if (filterNames != null) {
      for (String filterName : filterNames) {
        session.enableFilter(filterName);
      }
    }
  }

  /**
   * Disable the specified filters on the given Session.
   *
   * @param session the current Hibernate Session
   * @see #setFilterNames
   * @see Session#disableFilter(String)
   */
  protected void disableFilters(Session session) {
    String[] filterNames = getFilterNames();
    if (filterNames != null) {
      for (String filterName : filterNames) {
        session.disableFilter(filterName);
      }
    }
  }

  //-------------------------------------------------------------------------
  // Convenience methods for loading individual objects
  //-------------------------------------------------------------------------

  @Override
  @Nullable
  public <T> T get(Class<T> entityClass, Serializable id) throws DataAccessException {
    return get(entityClass, id, null);
  }

  @Override
  @Nullable
  public <T> T get(Class<T> entityClass, Serializable id, @Nullable LockMode lockMode) throws DataAccessException {
    return executeWithNativeSession(session -> {
      if (lockMode != null) {
        return session.get(entityClass, id, new LockOptions(lockMode));
      }
      else {
        return session.get(entityClass, id);
      }
    });
  }

  @Override
  @Nullable
  public Object get(String entityName, Serializable id) throws DataAccessException {
    return get(entityName, id, null);
  }

  @Override
  @Nullable
  public Object get(String entityName, Serializable id, @Nullable LockMode lockMode) throws DataAccessException {
    return executeWithNativeSession(session -> {
      if (lockMode != null) {
        return session.get(entityName, id, new LockOptions(lockMode));
      }
      else {
        return session.get(entityName, id);
      }
    });
  }

  @Override
  public <T> T load(Class<T> entityClass, Serializable id) throws DataAccessException {
    return load(entityClass, id, null);
  }

  @Override
  public <T> T load(Class<T> entityClass, Serializable id, @Nullable LockMode lockMode)
          throws DataAccessException {

    return nonNull(executeWithNativeSession(session -> {
      if (lockMode != null) {
        return session.load(entityClass, id, new LockOptions(lockMode));
      }
      else {
        return session.load(entityClass, id);
      }
    }));
  }

  @Override
  public Object load(String entityName, Serializable id) throws DataAccessException {
    return load(entityName, id, null);
  }

  @Override
  public Object load(String entityName, Serializable id, @Nullable LockMode lockMode) throws DataAccessException {
    return nonNull(executeWithNativeSession(session -> {
      if (lockMode != null) {
        return session.load(entityName, id, new LockOptions(lockMode));
      }
      else {
        return session.load(entityName, id);
      }
    }));
  }

  @Override
  @SuppressWarnings({ "unchecked", "deprecation" })
  public <T> List<T> loadAll(Class<T> entityClass) throws DataAccessException {
    return nonNull(executeWithNativeSession(session -> {
      Criteria criteria = session.createCriteria(entityClass);
      criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
      prepareCriteria(criteria);
      return criteria.list();
    }));
  }

  @Override
  public void load(Object entity, Serializable id) throws DataAccessException {
    executeWithNativeSession(session -> {
      session.load(entity, id);
      return null;
    });
  }

  @Override
  public void refresh(Object entity) throws DataAccessException {
    refresh(entity, null);
  }

  @Override
  public void refresh(Object entity, @Nullable LockMode lockMode) throws DataAccessException {
    executeWithNativeSession(session -> {
      if (lockMode != null) {
        session.refresh(entity, new LockOptions(lockMode));
      }
      else {
        session.refresh(entity);
      }
      return null;
    });
  }

  @Override
  public boolean contains(Object entity) throws DataAccessException {
    Boolean result = executeWithNativeSession(session -> session.contains(entity));
    Assert.state(result != null, "No contains result");
    return result;
  }

  @Override
  public void evict(Object entity) throws DataAccessException {
    executeWithNativeSession(session -> {
      session.evict(entity);
      return null;
    });
  }

  @Override
  public void initialize(Object proxy) throws DataAccessException {
    try {
      Hibernate.initialize(proxy);
    }
    catch (HibernateException ex) {
      throw SessionFactoryUtils.convertHibernateAccessException(ex);
    }
  }

  @Override
  public Filter enableFilter(String filterName) throws IllegalStateException {
    Session session = obtainSessionFactory().getCurrentSession();
    Filter filter = session.getEnabledFilter(filterName);
    if (filter == null) {
      filter = session.enableFilter(filterName);
    }
    return filter;
  }

  //-------------------------------------------------------------------------
  // Convenience methods for storing individual objects
  //-------------------------------------------------------------------------

  @Override
  public void lock(Object entity, LockMode lockMode) throws DataAccessException {
    executeWithNativeSession(session -> {
      session.buildLockRequest(new LockOptions(lockMode)).lock(entity);
      return null;
    });
  }

  @Override
  public void lock(String entityName, Object entity, LockMode lockMode)
          throws DataAccessException {

    executeWithNativeSession(session -> {
      session.buildLockRequest(new LockOptions(lockMode)).lock(entityName, entity);
      return null;
    });
  }

  @Override
  public Serializable save(Object entity) throws DataAccessException {
    return nonNull(executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      return session.save(entity);
    }));
  }

  @Override
  public Serializable save(String entityName, Object entity) throws DataAccessException {
    return nonNull(executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      return session.save(entityName, entity);
    }));
  }

  @Override
  public void update(Object entity) throws DataAccessException {
    update(entity, null);
  }

  @Override
  public void update(Object entity, @Nullable LockMode lockMode) throws DataAccessException {
    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      session.update(entity);
      if (lockMode != null) {
        session.buildLockRequest(new LockOptions(lockMode)).lock(entity);
      }
      return null;
    });
  }

  @Override
  public void update(String entityName, Object entity) throws DataAccessException {
    update(entityName, entity, null);
  }

  @Override
  public void update(String entityName, Object entity, @Nullable LockMode lockMode)
          throws DataAccessException {

    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      session.update(entityName, entity);
      if (lockMode != null) {
        session.buildLockRequest(new LockOptions(lockMode)).lock(entityName, entity);
      }
      return null;
    });
  }

  @Override
  public void saveOrUpdate(Object entity) throws DataAccessException {
    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      session.saveOrUpdate(entity);
      return null;
    });
  }

  @Override
  public void saveOrUpdate(String entityName, Object entity) throws DataAccessException {
    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      session.saveOrUpdate(entityName, entity);
      return null;
    });
  }

  @Override
  public void replicate(Object entity, ReplicationMode replicationMode) throws DataAccessException {
    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      session.replicate(entity, replicationMode);
      return null;
    });
  }

  @Override
  public void replicate(String entityName, Object entity, ReplicationMode replicationMode)
          throws DataAccessException {

    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      session.replicate(entityName, entity, replicationMode);
      return null;
    });
  }

  @Override
  public void persist(Object entity) throws DataAccessException {
    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      session.persist(entity);
      return null;
    });
  }

  @Override
  public void persist(String entityName, Object entity) throws DataAccessException {
    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      session.persist(entityName, entity);
      return null;
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T merge(T entity) throws DataAccessException {
    return nonNull(executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      return (T) session.merge(entity);
    }));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T merge(String entityName, T entity) throws DataAccessException {
    return nonNull(executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      return (T) session.merge(entityName, entity);
    }));
  }

  @Override
  public void delete(Object entity) throws DataAccessException {
    delete(entity, null);
  }

  @Override
  public void delete(Object entity, @Nullable LockMode lockMode) throws DataAccessException {
    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      if (lockMode != null) {
        session.buildLockRequest(new LockOptions(lockMode)).lock(entity);
      }
      session.delete(entity);
      return null;
    });
  }

  @Override
  public void delete(String entityName, Object entity) throws DataAccessException {
    delete(entityName, entity, null);
  }

  @Override
  public void delete(String entityName, Object entity, @Nullable LockMode lockMode)
          throws DataAccessException {

    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      if (lockMode != null) {
        session.buildLockRequest(new LockOptions(lockMode)).lock(entityName, entity);
      }
      session.delete(entityName, entity);
      return null;
    });
  }

  @Override
  public void deleteAll(Collection<?> entities) throws DataAccessException {
    executeWithNativeSession(session -> {
      checkWriteOperationAllowed(session);
      for (Object entity : entities) {
        session.delete(entity);
      }
      return null;
    });
  }

  @Override
  public void flush() throws DataAccessException {
    executeWithNativeSession(session -> {
      session.flush();
      return null;
    });
  }

  @Override
  public void clear() throws DataAccessException {
    executeWithNativeSession(session -> {
      session.clear();
      return null;
    });
  }

  //-------------------------------------------------------------------------
  // Convenience finder methods for detached criteria
  //-------------------------------------------------------------------------

  @Override
  public List<?> findByCriteria(DetachedCriteria criteria) throws DataAccessException {
    return findByCriteria(criteria, -1, -1);
  }

  @Override
  public List<?> findByCriteria(DetachedCriteria criteria, int firstResult, int maxResults)
          throws DataAccessException {

    Assert.notNull(criteria, "DetachedCriteria is required");
    return nonNull(executeWithNativeSession(session -> {
      Criteria executableCriteria = criteria.getExecutableCriteria(session);
      prepareCriteria(executableCriteria);
      if (firstResult >= 0) {
        executableCriteria.setFirstResult(firstResult);
      }
      if (maxResults > 0) {
        executableCriteria.setMaxResults(maxResults);
      }
      return executableCriteria.list();
    }));
  }

  @Override
  public <T> List<T> findByExample(T exampleEntity) throws DataAccessException {
    return findByExample(null, exampleEntity, -1, -1);
  }

  @Override
  public <T> List<T> findByExample(String entityName, T exampleEntity) throws DataAccessException {
    return findByExample(entityName, exampleEntity, -1, -1);
  }

  @Override
  public <T> List<T> findByExample(T exampleEntity, int firstResult, int maxResults) throws DataAccessException {
    return findByExample(null, exampleEntity, firstResult, maxResults);
  }

  @Override
  @SuppressWarnings({ "unchecked", "deprecation" })
  public <T> List<T> findByExample(@Nullable String entityName, T exampleEntity, int firstResult, int maxResults)
          throws DataAccessException {

    Assert.notNull(exampleEntity, "Example entity is required");
    return nonNull(executeWithNativeSession(session -> {
      Criteria executableCriteria = (entityName != null ?
                                     session.createCriteria(entityName) : session.createCriteria(exampleEntity.getClass()));
      executableCriteria.add(Example.create(exampleEntity));
      prepareCriteria(executableCriteria);
      if (firstResult >= 0) {
        executableCriteria.setFirstResult(firstResult);
      }
      if (maxResults > 0) {
        executableCriteria.setMaxResults(maxResults);
      }
      return executableCriteria.list();
    }));
  }

  //-------------------------------------------------------------------------
  // Convenience finder methods for HQL strings
  //-------------------------------------------------------------------------

  @Override
  public List<?> find(String queryString, @Nullable Object... values) throws DataAccessException {
    return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
      Query<?> queryObject = session.createQuery(queryString);
      prepareQuery(queryObject);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          queryObject.setParameter(i, values[i]);
        }
      }
      return queryObject.list();
    }));
  }

  @Override
  public List<?> findByNamedParam(String queryString, String paramName, Object value)
          throws DataAccessException {

    return findByNamedParam(queryString, new String[] { paramName }, new Object[] { value });
  }

  @Override
  public List<?> findByNamedParam(String queryString, String[] paramNames, Object[] values)
          throws DataAccessException {

    if (paramNames.length != values.length) {
      throw new IllegalArgumentException("Length of paramNames array must match length of values array");
    }
    return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
      Query<?> queryObject = session.createQuery(queryString);
      prepareQuery(queryObject);
      for (int i = 0; i < values.length; i++) {
        applyNamedParameterToQuery(queryObject, paramNames[i], values[i]);
      }
      return queryObject.list();
    }));
  }

  @Override
  public List<?> findByValueBean(String queryString, Object valueBean) throws DataAccessException {
    return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
      Query<?> queryObject = session.createQuery(queryString);
      prepareQuery(queryObject);
      queryObject.setProperties(valueBean);
      return queryObject.list();
    }));
  }

  //-------------------------------------------------------------------------
  // Convenience finder methods for named queries
  //-------------------------------------------------------------------------

  @Override
  public List<?> findByNamedQuery(String queryName, @Nullable Object... values) throws DataAccessException {
    return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
      Query<?> queryObject = session.getNamedQuery(queryName);
      prepareQuery(queryObject);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          queryObject.setParameter(i, values[i]);
        }
      }
      return queryObject.list();
    }));
  }

  @Override
  public List<?> findByNamedQueryAndNamedParam(String queryName, String paramName, Object value)
          throws DataAccessException {

    return findByNamedQueryAndNamedParam(queryName, new String[] { paramName }, new Object[] { value });
  }

  @Override
  public List<?> findByNamedQueryAndNamedParam(
          String queryName, @Nullable String[] paramNames, @Nullable Object[] values)
          throws DataAccessException {

    if (values != null && (paramNames == null || paramNames.length != values.length)) {
      throw new IllegalArgumentException("Length of paramNames array must match length of values array");
    }
    return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
      Query<?> queryObject = session.getNamedQuery(queryName);
      prepareQuery(queryObject);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          applyNamedParameterToQuery(queryObject, paramNames[i], values[i]);
        }
      }
      return queryObject.list();
    }));
  }

  @Override
  public List<?> findByNamedQueryAndValueBean(String queryName, Object valueBean) throws DataAccessException {
    return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
      Query<?> queryObject = session.getNamedQuery(queryName);
      prepareQuery(queryObject);
      queryObject.setProperties(valueBean);
      return queryObject.list();
    }));
  }

  //-------------------------------------------------------------------------
  // Convenience query methods for iteration and bulk updates/deletes
  //-------------------------------------------------------------------------

  @Override
  public Iterator<?> iterate(String queryString, @Nullable Object... values) throws DataAccessException {
    return nonNull(executeWithNativeSession((HibernateCallback<Iterator<?>>) session -> {
      Query<?> queryObject = session.createQuery(queryString);
      prepareQuery(queryObject);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          queryObject.setParameter(i, values[i]);
        }
      }
      return queryObject.iterate();
    }));
  }

  @Override
  public void closeIterator(Iterator<?> it) throws DataAccessException {
    try {
      Hibernate.close(it);
    }
    catch (HibernateException ex) {
      throw SessionFactoryUtils.convertHibernateAccessException(ex);
    }
  }

  @Override
  public int bulkUpdate(String queryString, @Nullable Object... values) throws DataAccessException {
    Integer result = executeWithNativeSession(session -> {
      Query<?> queryObject = session.createQuery(queryString);
      prepareQuery(queryObject);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          queryObject.setParameter(i, values[i]);
        }
      }
      return queryObject.executeUpdate();
    });
    Assert.state(result != null, "No update count");
    return result;
  }

  //-------------------------------------------------------------------------
  // Helper methods used by the operations above
  //-------------------------------------------------------------------------

  /**
   * Check whether write operations are allowed on the given Session.
   * <p>Default implementation throws an InvalidDataAccessApiUsageException in
   * case of {@code FlushMode.MANUAL}. Can be overridden in subclasses.
   *
   * @param session current Hibernate Session
   * @throws InvalidDataAccessApiUsageException if write operations are not allowed
   * @see #setCheckWriteOperations
   * @see Session#getFlushMode()
   * @see FlushMode#MANUAL
   */
  protected void checkWriteOperationAllowed(Session session) throws InvalidDataAccessApiUsageException {
    if (isCheckWriteOperations() && session.getHibernateFlushMode().lessThan(FlushMode.COMMIT)) {
      throw new InvalidDataAccessApiUsageException(
              "Write operations are not allowed in read-only mode (FlushMode.MANUAL): " +
                      "Turn your Session into FlushMode.COMMIT/AUTO or remove 'readOnly' marker from transaction definition.");
    }
  }

  /**
   * Prepare the given Criteria object, applying cache settings and/or
   * a transaction timeout.
   *
   * @param criteria the Criteria object to prepare
   * @see #setCacheQueries
   * @see #setQueryCacheRegion
   */
  protected void prepareCriteria(Criteria criteria) {
    if (isCacheQueries()) {
      criteria.setCacheable(true);
      if (getQueryCacheRegion() != null) {
        criteria.setCacheRegion(getQueryCacheRegion());
      }
    }
    if (getFetchSize() > 0) {
      criteria.setFetchSize(getFetchSize());
    }
    if (getMaxResults() > 0) {
      criteria.setMaxResults(getMaxResults());
    }

    ResourceHolderSupport sessionHolder =
            TransactionSynchronizationManager.getResource(obtainSessionFactory());
    if (sessionHolder != null && sessionHolder.hasTimeout()) {
      criteria.setTimeout(sessionHolder.getTimeToLiveInSeconds());
    }
  }

  /**
   * Prepare the given Query object, applying cache settings and/or
   * a transaction timeout.
   *
   * @param queryObject the Query object to prepare
   * @see #setCacheQueries
   * @see #setQueryCacheRegion
   */
  protected void prepareQuery(Query<?> queryObject) {
    if (isCacheQueries()) {
      queryObject.setCacheable(true);
      if (getQueryCacheRegion() != null) {
        queryObject.setCacheRegion(getQueryCacheRegion());
      }
    }
    if (getFetchSize() > 0) {
      queryObject.setFetchSize(getFetchSize());
    }
    if (getMaxResults() > 0) {
      queryObject.setMaxResults(getMaxResults());
    }

    ResourceHolderSupport sessionHolder =
            TransactionSynchronizationManager.getResource(obtainSessionFactory());
    if (sessionHolder != null && sessionHolder.hasTimeout()) {
      queryObject.setTimeout(sessionHolder.getTimeToLiveInSeconds());
    }
  }

  /**
   * Apply the given name parameter to the given Query object.
   *
   * @param queryObject the Query object
   * @param paramName the name of the parameter
   * @param value the value of the parameter
   * @throws HibernateException if thrown by the Query object
   */
  protected void applyNamedParameterToQuery(Query<?> queryObject, String paramName, Object value)
          throws HibernateException {

    if (value instanceof Collection) {
      queryObject.setParameterList(paramName, (Collection<?>) value);
    }
    else if (value instanceof Object[]) {
      queryObject.setParameterList(paramName, (Object[]) value);
    }
    else {
      queryObject.setParameter(paramName, value);
    }
  }

  private static <T> T nonNull(@Nullable T result) {
    Assert.state(result != null, "No result");
    return result;
  }

  /**
   * Invocation handler that suppresses close calls on Hibernate Sessions.
   * Also prepares returned Query and Criteria objects.
   *
   * @see Session#close
   */
  private class CloseSuppressingInvocationHandler implements InvocationHandler {

    private final Session target;

    public CloseSuppressingInvocationHandler(Session target) {
      this.target = target;
    }

    @Override
    @Nullable
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // Invocation on Session interface coming in...

      switch (method.getName()) {
        case "equals":
          // Only consider equal when proxies are identical.
          return (proxy == args[0]);
        case "hashCode":
          // Use hashCode of Session proxy.
          return System.identityHashCode(proxy);
        case "close":
          // Handle close method: suppress, not valid.
          return null;
      }

      // Invoke method on target Session.
      try {
        Object retVal = method.invoke(this.target, args);

        // If return value is a Query or Criteria, apply transaction timeout.
        // Applies to createQuery, getNamedQuery, createCriteria.
        if (retVal instanceof Criteria) {
          prepareCriteria(((Criteria) retVal));
        }
        else if (retVal instanceof Query) {
          prepareQuery(((Query<?>) retVal));
        }

        return retVal;
      }
      catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "deprecation", "unchecked", "serial" })
  private class CloseSuppressingSessionProxy extends SessionDelegatorBaseImpl {

    public CloseSuppressingSessionProxy(SessionImplementor delegate) {
      super(delegate);
    }

    @Override
    public void close() throws HibernateException {

    }

    @Override
    public QueryImplementor getNamedQuery(String name) {
      QueryImplementor namedQuery = delegate.getNamedQuery(name);
      prepareQuery(namedQuery);
      return namedQuery;
    }

    @Override
    public NativeQueryImplementor getNamedSQLQuery(String name) {
      NativeQueryImplementor namedQuery = delegate.getNamedSQLQuery(name);
      prepareQuery(namedQuery);
      return namedQuery;
    }

    @Override
    public NativeQueryImplementor getNamedNativeQuery(String name) {
      NativeQueryImplementor namedNativeQuery = delegate.getNamedNativeQuery(name);
      prepareQuery(namedNativeQuery);
      return namedNativeQuery;
    }

    @Override
    public QueryImplementor createQuery(String queryString) {
      QueryImplementor query = delegate.createQuery(queryString);
      prepareQuery(query);
      return query;
    }

    @Override
    public <T> QueryImplementor<T> createQuery(String queryString, Class<T> resultType) {
      QueryImplementor<T> query = delegate.createQuery(queryString, resultType);
      prepareQuery(query);
      return query;
    }

    @Override
    public <T> QueryImplementor<T> createQuery(CriteriaQuery<T> criteriaQuery) {
      QueryImplementor<T> query = delegate.createQuery(criteriaQuery);
      prepareQuery(query);
      return query;
    }

    @Override
    public QueryImplementor createQuery(CriteriaUpdate updateQuery) {
      QueryImplementor query = delegate.createQuery(updateQuery);
      prepareQuery(query);
      return query;
    }

    @Override
    public QueryImplementor createQuery(CriteriaDelete deleteQuery) {
      QueryImplementor query = delegate.createQuery(deleteQuery);
      prepareQuery(query);
      return query;
    }

    @Override
    public QueryImplementor createNamedQuery(String name) {
      QueryImplementor namedQuery = delegate.createNamedQuery(name);
      prepareQuery(namedQuery);
      return namedQuery;
    }

    @Override
    public <T> QueryImplementor<T> createNamedQuery(String name, Class<T> resultClass) {
      QueryImplementor<T> namedQuery = delegate.createNamedQuery(name, resultClass);
      prepareQuery(namedQuery);
      return namedQuery;
    }

    @Override
    public NativeQueryImplementor createNativeQuery(String sqlString) {
      NativeQueryImplementor nativeQuery = delegate.createNativeQuery(sqlString);
      prepareQuery(nativeQuery);
      return nativeQuery;
    }

    @Override
    public NativeQueryImplementor createNativeQuery(String sqlString, Class resultClass) {
      NativeQueryImplementor nativeQuery = delegate.createNativeQuery(sqlString, resultClass);
      prepareQuery(nativeQuery);
      return nativeQuery;
    }

    @Override
    public NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMapping) {
      NativeQueryImplementor nativeQuery = delegate.createNativeQuery(sqlString, resultSetMapping);
      prepareQuery(nativeQuery);
      return nativeQuery;
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
      StoredProcedureQuery query = delegate.createNamedStoredProcedureQuery(name);
      if (query instanceof Query<?>) {
        prepareQuery((Query<?>) query);
      }
      return query;
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
      StoredProcedureQuery query = delegate.createStoredProcedureQuery(procedureName);
      if (query instanceof Query<?>) {
        prepareQuery((Query<?>) query);
      }
      return query;
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
      StoredProcedureQuery query = delegate.createStoredProcedureQuery(procedureName, resultClasses);
      if (query instanceof Query<?>) {
        prepareQuery((Query<?>) query);
      }
      return query;
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
      return delegate.createStoredProcedureQuery(procedureName, resultSetMappings);
    }

    @Override
    public Criteria createCriteria(Class persistentClass) {
      Criteria criteria = delegate.createCriteria(persistentClass);
      prepareCriteria(criteria);
      return criteria;
    }

    @Override
    public Criteria createCriteria(Class persistentClass, String alias) {
      Criteria criteria = delegate.createCriteria(persistentClass, alias);
      prepareCriteria(criteria);
      return criteria;
    }

    @Override
    public Criteria createCriteria(String entityName) {
      Criteria criteria = delegate.createCriteria(entityName);
      prepareCriteria(criteria);
      return criteria;
    }

    @Override
    public Criteria createCriteria(String entityName, String alias) {
      Criteria criteria = delegate.createCriteria(entityName, alias);
      prepareCriteria(criteria);
      return criteria;
    }

  }

}
