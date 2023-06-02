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
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PropertyValueException;
import org.hibernate.QueryException;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.query.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.dao.CannotAcquireLockException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.dao.InvalidDataAccessResourceUsageException;
import cn.taketoday.orm.jpa.testfixture.beans.TestBean;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;
import static org.mockito.BDDMockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/28 09:51
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class HibernateTemplateTests {

  private SessionFactory sessionFactory;

  private Session session;

  private HibernateTemplate hibernateTemplate;

  @BeforeEach
  public void setUp() {
    this.sessionFactory = mock(SessionFactory.class);
    this.session = mock(Session.class);
    this.hibernateTemplate = new HibernateTemplate(sessionFactory);
    given(sessionFactory.getCurrentSession()).willReturn(session);
  }

  @AfterEach
  public void tearDown() {
    assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
  }

  @Test
  public void testExecuteWithNewSession() {
    given(sessionFactory.getCurrentSession()).willThrow(new HibernateException("no current session"));
    given(sessionFactory.openSession()).willReturn(session);
    given(session.isOpen()).willReturn(true);

    final List l = new ArrayList();
    l.add("test");
    List result = hibernateTemplate.execute(session -> l);
    assertSame(result, l, "Correct result list");
    verify(session).close();
  }

  @Test
  public void testExecuteWithNewSessionAndFilter() {
    given(sessionFactory.getCurrentSession()).willThrow(new HibernateException("no current session"));
    given(sessionFactory.openSession()).willReturn(session);
    given(session.isOpen()).willReturn(true);

    hibernateTemplate.setFilterNames("myFilter");

    final List l = new ArrayList();
    l.add("test");
    List result = hibernateTemplate.execute(session -> l);
    assertSame(result, l, "Correct result list");
    verify(session).enableFilter("myFilter");
    verify(session).close();
  }

  @Test
  public void testExecuteWithNewSessionAndFilters() {
    given(sessionFactory.getCurrentSession()).willThrow(new HibernateException("no current session"));
    given(sessionFactory.openSession()).willReturn(session);
    given(session.isOpen()).willReturn(true);

    hibernateTemplate.setFilterNames("myFilter", "yourFilter");

    final List l = new ArrayList();
    l.add("test");
    List result = hibernateTemplate.execute(session -> l);
    assertSame(result, l, "Correct result list");
    InOrder ordered = inOrder(session);
    ordered.verify(session).enableFilter("myFilter");
    ordered.verify(session).enableFilter("yourFilter");
    ordered.verify(session).close();
  }

  @Test
  public void testExecuteWithThreadBound() {
    final List l = new ArrayList();
    l.add("test");
    List result = hibernateTemplate.execute(session -> l);
    assertSame(result, l, "Correct result list");
  }

  @Test
  public void testExecuteWithThreadBoundAndFilter() {
    hibernateTemplate.setFilterNames("myFilter");

    final List l = new ArrayList();
    l.add("test");
    List result = hibernateTemplate.execute(session -> l);
    assertSame(result, l, "Correct result list");

    InOrder ordered = inOrder(session);
    ordered.verify(session).enableFilter("myFilter");
    ordered.verify(session).disableFilter("myFilter");
  }

  @Test
  public void testExecuteWithThreadBoundAndFilters() {
    hibernateTemplate.setFilterNames("myFilter", "yourFilter");

    final List l = new ArrayList();
    l.add("test");
    List result = hibernateTemplate.execute(session -> l);
    assertSame(result, l, "Correct result list");

    InOrder ordered = inOrder(session);
    ordered.verify(session).enableFilter("myFilter");
    ordered.verify(session).enableFilter("yourFilter");
    ordered.verify(session).disableFilter("myFilter");
    ordered.verify(session).disableFilter("yourFilter");
  }

  @Test
  public void testExecuteWithThreadBoundAndParameterizedFilter() {
    Filter filter = mock(Filter.class);
    given(session.enableFilter("myFilter")).willReturn(filter);
    hibernateTemplate.setFilterNames("myFilter");

    final List l = new ArrayList();
    l.add("test");
    Filter f = hibernateTemplate.enableFilter("myFilter");
    assertSame(f, filter, "Correct filter");

    InOrder ordered = inOrder(session);
    ordered.verify(session).getEnabledFilter("myFilter");
    ordered.verify(session).enableFilter("myFilter");
  }

  @Test
  public void testExecuteWithThreadBoundAndParameterizedExistingFilter() {
    Filter filter = mock(Filter.class);
    given(session.enableFilter("myFilter")).willReturn(filter);
    hibernateTemplate.setFilterNames("myFilter");

    final List l = new ArrayList();
    l.add("test");
    Filter f = hibernateTemplate.enableFilter("myFilter");
    assertSame(f, filter, "Correct filter");

    verify(session).getEnabledFilter("myFilter");
  }

  @Test
  public void testExecuteWithCacheQueries() {
    Query query1 = mock(Query.class);
    Query query2 = mock(Query.class);
    Criteria criteria = mock(Criteria.class);
    given(session.createQuery("some query")).willReturn(query1);
    given(query1.setCacheable(true)).willReturn(query1);
    given(session.getNamedQuery("some query name")).willReturn(query2);
    given(query2.setCacheable(true)).willReturn(query2);
    given(session.createCriteria(TestBean.class)).willReturn(criteria);
    given(criteria.setCacheable(true)).willReturn(criteria);

    hibernateTemplate.setCacheQueries(true);
    hibernateTemplate.execute(sess -> {
      assertNotSame(session, sess);
      assertTrue(Proxy.isProxyClass(sess.getClass()));
      sess.createQuery("some query");
      sess.getNamedQuery("some query name");
      sess.createCriteria(TestBean.class);
      // should be ignored
      sess.close();
      return null;
    });
  }

  @Test
  public void testExecuteWithCacheQueriesAndCacheRegion() {
    Query query1 = mock(Query.class);
    Query query2 = mock(Query.class);
    Criteria criteria = mock(Criteria.class);
    given(session.createQuery("some query")).willReturn(query1);
    given(query1.setCacheable(true)).willReturn(query1);
    given(query1.setCacheRegion("myRegion")).willReturn(query1);
    given(session.getNamedQuery("some query name")).willReturn(query2);
    given(query2.setCacheable(true)).willReturn(query2);
    given(query2.setCacheRegion("myRegion")).willReturn(query2);
    given(session.createCriteria(TestBean.class)).willReturn(criteria);
    given(criteria.setCacheable(true)).willReturn(criteria);
    given(criteria.setCacheRegion("myRegion")).willReturn(criteria);

    hibernateTemplate.setCacheQueries(true);
    hibernateTemplate.setQueryCacheRegion("myRegion");
    hibernateTemplate.execute(sess -> {
      assertNotSame(session, sess);
      assertTrue(Proxy.isProxyClass(sess.getClass()));
      sess.createQuery("some query");
      sess.getNamedQuery("some query name");
      sess.createCriteria(TestBean.class);
      // should be ignored
      sess.close();
      return null;
    });
  }

  @Test
  public void testExecuteWithCacheQueriesAndCacheRegionAndNativeSession() {
    Query query1 = mock(Query.class);
    Query query2 = mock(Query.class);
    Criteria criteria = mock(Criteria.class);

    given(session.createQuery("some query")).willReturn(query1);
    given(session.getNamedQuery("some query name")).willReturn(query2);
    given(session.createCriteria(TestBean.class)).willReturn(criteria);

    hibernateTemplate.setExposeNativeSession(true);
    hibernateTemplate.setCacheQueries(true);
    hibernateTemplate.setQueryCacheRegion("myRegion");
    hibernateTemplate.execute(sess -> {
      assertSame(session, sess);
      sess.createQuery("some query");
      sess.getNamedQuery("some query name");
      sess.createCriteria(TestBean.class);
      return null;
    });
  }

  @Test
  public void testExecuteWithFetchSizeAndMaxResults() {
    Query query1 = mock(Query.class);
    Query query2 = mock(Query.class);
    Criteria criteria = mock(Criteria.class);

    given(session.createQuery("some query")).willReturn(query1);
    given(query1.setFetchSize(10)).willReturn(query1);
    given(query1.setMaxResults(20)).willReturn(query1);
    given(session.getNamedQuery("some query name")).willReturn(query2);
    given(query2.setFetchSize(10)).willReturn(query2);
    given(query2.setMaxResults(20)).willReturn(query2);
    given(session.createCriteria(TestBean.class)).willReturn(criteria);
    given(criteria.setFetchSize(10)).willReturn(criteria);
    given(criteria.setMaxResults(20)).willReturn(criteria);

    hibernateTemplate.setFetchSize(10);
    hibernateTemplate.setMaxResults(20);
    hibernateTemplate.execute(sess -> {
      sess.createQuery("some query");
      sess.getNamedQuery("some query name");
      sess.createCriteria(TestBean.class);
      return null;
    });
  }

  @Test
  public void testGet() {
    TestBean tb = new TestBean();
    given(session.get(TestBean.class, "")).willReturn(tb);
    Object result = hibernateTemplate.get(TestBean.class, "");
    assertSame(result, tb, "Correct result");
  }

  @Test
  public void testGetWithEntityName() {
    TestBean tb = new TestBean();
    given(session.get("myEntity", "")).willReturn(tb);
    Object result = hibernateTemplate.get("myEntity", "");
    assertSame(result, tb, "Correct result");
  }

  @Test
  public void testLoad() {
    TestBean tb = new TestBean();
    given(session.load(TestBean.class, "")).willReturn(tb);
    Object result = hibernateTemplate.load(TestBean.class, "");
    assertSame(result, tb, "Correct result");
  }

  @Test
  public void testLoadWithNotFound() {
    ObjectNotFoundException onfex = new ObjectNotFoundException("id", TestBean.class.getName());
    given(session.load(TestBean.class, "id")).willThrow(onfex);
    try {
      hibernateTemplate.load(TestBean.class, "id");
      fail("Should have thrown HibernateObjectRetrievalFailureException");
    }
    catch (HibernateObjectRetrievalFailureException ex) {
      // expected
      assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
      assertEquals("id", ex.getIdentifier());
      assertEquals(onfex, ex.getCause());
    }
  }

  @Test
  public void testLoadWithEntityName() {
    TestBean tb = new TestBean();
    given(session.load("myEntity", "")).willReturn(tb);
    Object result = hibernateTemplate.load("myEntity", "");
    assertSame(result, tb, "Correct result");
  }

  @Test
  public void testLoadWithObject() {
    TestBean tb = new TestBean();
    hibernateTemplate.load(tb, "");
    verify(session).load(tb, "");
  }

  @Test
  public void testLoadAll() {
    Criteria criteria = mock(Criteria.class);
    List list = new ArrayList();
    given(session.createCriteria(TestBean.class)).willReturn(criteria);
    given(criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).willReturn(criteria);
    given(criteria.list()).willReturn(list);
    List result = hibernateTemplate.loadAll(TestBean.class);
    assertSame(result, list, "Correct result");
  }

  @Test
  public void testLoadAllWithCacheable() {
    Criteria criteria = mock(Criteria.class);
    List list = new ArrayList();
    given(session.createCriteria(TestBean.class)).willReturn(criteria);
    given(criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).willReturn(criteria);
    given(criteria.setCacheable(true)).willReturn(criteria);
    given(criteria.list()).willReturn(list);

    hibernateTemplate.setCacheQueries(true);
    List result = hibernateTemplate.loadAll(TestBean.class);
    assertSame(result, list, "Correct result");
    verify(criteria).setCacheable(true);
  }

  @Test
  public void testLoadAllWithCacheableAndCacheRegion() {
    Criteria criteria = mock(Criteria.class);
    List list = new ArrayList();
    given(session.createCriteria(TestBean.class)).willReturn(criteria);
    given(criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).willReturn(criteria);
    given(criteria.setCacheable(true)).willReturn(criteria);
    given(criteria.setCacheRegion("myCacheRegion")).willReturn(criteria);
    given(criteria.list()).willReturn(list);

    hibernateTemplate.setCacheQueries(true);
    hibernateTemplate.setQueryCacheRegion("myCacheRegion");
    List result = hibernateTemplate.loadAll(TestBean.class);
    assertSame(result, list, "Correct result");
    verify(criteria).setCacheable(true);
    verify(criteria).setCacheRegion("myCacheRegion");
  }

  @Test
  public void testRefresh() {
    TestBean tb = new TestBean();
    hibernateTemplate.refresh(tb);
    verify(session).refresh(tb);
  }

  @Test
  public void testContains() {
    TestBean tb = new TestBean();
    given(session.contains(tb)).willReturn(true);
    assertTrue(hibernateTemplate.contains(tb));
  }

  @Test
  public void testEvict() {
    TestBean tb = new TestBean();
    hibernateTemplate.evict(tb);
    verify(session).evict(tb);
  }

  @Test
  public void testSave() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session.save(tb)).willReturn(0);
    assertEquals(hibernateTemplate.save(tb), 0, "Correct return value");
  }

  @Test
  public void testSaveWithEntityName() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session.save("myEntity", tb)).willReturn(0);
    assertEquals(hibernateTemplate.save("myEntity", tb), 0, "Correct return value");
  }

  @Test
  public void testUpdate() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    hibernateTemplate.update(tb);
    verify(session).update(tb);
  }

  @Test
  public void testUpdateWithEntityName() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    hibernateTemplate.update("myEntity", tb);
    verify(session).update("myEntity", tb);
  }

  @Test
  public void testSaveOrUpdate() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    hibernateTemplate.saveOrUpdate(tb);
    verify(session).saveOrUpdate(tb);
  }

  @Test
  public void testSaveOrUpdateWithFlushModeNever() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.MANUAL);
    try {
      hibernateTemplate.saveOrUpdate(tb);
      fail("Should have thrown InvalidDataAccessApiUsageException");
    }
    catch (InvalidDataAccessApiUsageException ex) {
      // expected
    }
  }

  @Test
  public void testSaveOrUpdateWithEntityName() {
    TestBean tb = new TestBean();

    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    hibernateTemplate.saveOrUpdate("myEntity", tb);
    verify(session).saveOrUpdate("myEntity", tb);
  }

  @Test
  public void testReplicate() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    hibernateTemplate.replicate(tb, ReplicationMode.LATEST_VERSION);
    verify(session).replicate(tb, ReplicationMode.LATEST_VERSION);
  }

  @Test
  public void testReplicateWithEntityName() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    hibernateTemplate.replicate("myEntity", tb, ReplicationMode.LATEST_VERSION);
    verify(session).replicate("myEntity", tb, ReplicationMode.LATEST_VERSION);
  }

  @Test
  public void testPersist() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    hibernateTemplate.persist(tb);
    verify(session).persist(tb);
  }

  @Test
  public void testPersistWithEntityName() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    hibernateTemplate.persist("myEntity", tb);
    verify(session).persist("myEntity", tb);
  }

  @Test
  public void testMerge() {
    TestBean tb = new TestBean();
    TestBean tbMerged = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session.merge(tb)).willReturn(tbMerged);
    assertSame(tbMerged, hibernateTemplate.merge(tb));
  }

  @Test
  public void testMergeWithEntityName() {
    TestBean tb = new TestBean();
    TestBean tbMerged = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session.merge("myEntity", tb)).willReturn(tbMerged);
    assertSame(tbMerged, hibernateTemplate.merge("myEntity", tb));
  }

  @Test
  public void testDelete() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    hibernateTemplate.delete(tb);
    verify(session).delete(tb);
  }

  @Test
  public void testDeleteWithEntityName() {
    TestBean tb = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    hibernateTemplate.delete("myEntity", tb);
    verify(session).delete("myEntity", tb);
  }

  @Test
  public void testDeleteAll() {
    TestBean tb1 = new TestBean();
    TestBean tb2 = new TestBean();
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    List tbs = new ArrayList();
    tbs.add(tb1);
    tbs.add(tb2);
    hibernateTemplate.deleteAll(tbs);
    verify(session).delete(same(tb1));
    verify(session).delete(same(tb2));
  }

  @Test
  public void testFlush() {
    hibernateTemplate.flush();
    verify(session).flush();
  }

  @Test
  public void testClear() {
    hibernateTemplate.clear();
    verify(session).clear();
  }

  @Test
  public void testFind() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.find("some query string");
    assertSame(result, list, "Correct list");
  }

  @Test
  public void testFindWithParameter() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setParameter(0, "myvalue")).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.find("some query string", "myvalue");
    assertSame(result, list, "Correct list");
    verify(query).setParameter(0, "myvalue");
  }

  @Test
  public void testFindWithParameters() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setParameter(0, "myvalue1")).willReturn(query);
    given(query.setParameter(1, 2)).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.find("some query string", "myvalue1", 2);
    assertSame(result, list, "Correct list");
    verify(query).setParameter(0, "myvalue1");
    verify(query).setParameter(1, 2);
  }

  @Test
  public void testFindWithNamedParameter() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setParameter("myparam", "myvalue")).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.findByNamedParam("some query string", "myparam", "myvalue");
    assertSame(result, list, "Correct list");
    verify(query).setParameter("myparam", "myvalue");
  }

  @Test
  public void testFindWithNamedParameters() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setParameter("myparam1", "myvalue1")).willReturn(query);
    given(query.setParameter("myparam2", 2)).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.findByNamedParam("some query string",
            new String[] { "myparam1", "myparam2" },
            new Object[] { "myvalue1", 2 });
    assertSame(result, list, "Correct list");
    verify(query).setParameter("myparam1", "myvalue1");
    verify(query).setParameter("myparam2", 2);
  }

  @Test
  public void testFindByValueBean() {
    Query query = mock(Query.class);
    TestBean tb = new TestBean();
    List list = new ArrayList();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setProperties(tb)).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.findByValueBean("some query string", tb);
    assertSame(result, list, "Correct list");
    verify(query).setProperties(tb);
  }

  @Test
  public void testFindByNamedQuery() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.getNamedQuery("some query name")).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.findByNamedQuery("some query name");
    assertSame(result, list, "Correct list");
  }

  @Test
  public void testFindByNamedQueryWithParameter() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.getNamedQuery("some query name")).willReturn(query);
    given(query.setParameter(0, "myvalue")).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.findByNamedQuery("some query name", "myvalue");
    assertSame(result, list, "Correct list");
    verify(query).setParameter(0, "myvalue");
  }

  @Test
  public void testFindByNamedQueryWithParameters() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.getNamedQuery("some query name")).willReturn(query);
    given(query.setParameter(0, "myvalue1")).willReturn(query);
    given(query.setParameter(1, 2)).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.findByNamedQuery("some query name", "myvalue1", 2);
    assertSame(result, list, "Correct list");
    verify(query).setParameter(0, "myvalue1");
    verify(query).setParameter(1, 2);
  }

  @Test
  public void testFindByNamedQueryWithNamedParameter() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.getNamedQuery("some query name")).willReturn(query);
    given(query.setParameter("myparam", "myvalue")).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.findByNamedQueryAndNamedParam("some query name", "myparam", "myvalue");
    assertSame(result, list, "Correct list");
    verify(query).setParameter("myparam", "myvalue");
  }

  @Test
  public void testFindByNamedQueryWithNamedParameters() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.getNamedQuery("some query name")).willReturn(query);
    given(query.setParameter("myparam1", "myvalue1")).willReturn(query);
    given(query.setParameter("myparam2", 2)).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.findByNamedQueryAndNamedParam("some query name",
            new String[] { "myparam1", "myparam2" },
            new Object[] { "myvalue1", 2 });
    assertSame(result, list, "Correct list");
    verify(query).setParameter("myparam1", "myvalue1");
    verify(query).setParameter("myparam2", 2);
  }

  @Test
  public void testFindByNamedQueryAndValueBean() {
    Query query = mock(Query.class);
    TestBean tb = new TestBean();
    List list = new ArrayList();
    given(session.getNamedQuery("some query name")).willReturn(query);
    given(query.setProperties(tb)).willReturn(query);
    given(query.list()).willReturn(list);
    List result = hibernateTemplate.findByNamedQueryAndValueBean("some query name", tb);
    assertSame(result, list, "Correct list");
    verify(query).setProperties(tb);
  }

  @Test
  public void testFindWithCacheable() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setCacheable(true)).willReturn(query);
    given(query.list()).willReturn(list);
    hibernateTemplate.setCacheQueries(true);
    List result = hibernateTemplate.find("some query string");
    assertSame(result, list, "Correct list");
    verify(query).setCacheable(true);
  }

  @Test
  public void testFindWithCacheableAndCacheRegion() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setCacheable(true)).willReturn(query);
    given(query.setCacheRegion("myCacheRegion")).willReturn(query);
    given(query.list()).willReturn(list);
    hibernateTemplate.setCacheQueries(true);
    hibernateTemplate.setQueryCacheRegion("myCacheRegion");
    List result = hibernateTemplate.find("some query string");
    assertSame(result, list, "Correct list");
    verify(query).setCacheable(true);
    verify(query).setCacheRegion("myCacheRegion");
  }

  @Test
  public void testFindByNamedQueryWithCacheable() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.getNamedQuery("some query name")).willReturn(query);
    given(query.setCacheable(true)).willReturn(query);
    given(query.list()).willReturn(list);
    hibernateTemplate.setCacheQueries(true);
    List result = hibernateTemplate.findByNamedQuery("some query name");
    assertSame(result, list, "Correct list");
    verify(query).setCacheable(true);
  }

  @Test
  public void testFindByNamedQueryWithCacheableAndCacheRegion() {
    Query query = mock(Query.class);
    List list = new ArrayList();
    given(session.getNamedQuery("some query name")).willReturn(query);
    given(query.setCacheable(true)).willReturn(query);
    given(query.setCacheRegion("myCacheRegion")).willReturn(query);
    given(query.list()).willReturn(list);
    hibernateTemplate.setCacheQueries(true);
    hibernateTemplate.setQueryCacheRegion("myCacheRegion");
    List result = hibernateTemplate.findByNamedQuery("some query name");
    assertSame(result, list, "Correct list");
    verify(query).setCacheable(true);
    verify(query).setCacheRegion("myCacheRegion");
  }

  @Test
  public void testIterate() {
    Query query = mock(Query.class);
    Iterator it = Collections.EMPTY_LIST.iterator();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.iterate()).willReturn(it);
    Iterator result = hibernateTemplate.iterate("some query string");
    assertSame(result, it, "Correct list");
  }

  @Test
  public void testIterateWithParameter() {
    Query query = mock(Query.class);
    Iterator it = Collections.EMPTY_LIST.iterator();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setParameter(0, "myvalue")).willReturn(query);
    given(query.iterate()).willReturn(it);
    Iterator result = hibernateTemplate.iterate("some query string", "myvalue");
    assertSame(result, it, "Correct list");
    verify(query).setParameter(0, "myvalue");
  }

  @Test
  public void testIterateWithParameters() {
    Query query = mock(Query.class);
    Iterator it = Collections.emptyIterator();
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setParameter(0, "myvalue1")).willReturn(query);
    given(query.setParameter(1, 2)).willReturn(query);
    given(query.iterate()).willReturn(it);
    Iterator result = hibernateTemplate.iterate("some query string", "myvalue1", 2);
    assertSame(result, it, "Correct list");
    verify(query).setParameter(0, "myvalue1");
    verify(query).setParameter(1, 2);
  }

  @Test
  public void testBulkUpdate() {
    Query query = mock(Query.class);
    given(session.createQuery("some query string")).willReturn(query);
    given(query.executeUpdate()).willReturn(5);
    int result = hibernateTemplate.bulkUpdate("some query string");
    assertEquals(5, result, "Correct list");
  }

  @Test
  public void testBulkUpdateWithParameter() {
    Query query = mock(Query.class);
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setParameter(0, "myvalue")).willReturn(query);
    given(query.executeUpdate()).willReturn(5);
    int result = hibernateTemplate.bulkUpdate("some query string", "myvalue");
    assertEquals(5, result, "Correct list");
    verify(query).setParameter(0, "myvalue");
  }

  @Test
  public void testBulkUpdateWithParameters() {
    Query query = mock(Query.class);
    given(session.createQuery("some query string")).willReturn(query);
    given(query.setParameter(0, "myvalue1")).willReturn(query);
    given(query.setParameter(1, 2)).willReturn(query);
    given(query.executeUpdate()).willReturn(5);
    int result = hibernateTemplate.bulkUpdate("some query string", "myvalue1", 2);
    assertEquals(5, result, "Correct list");
    verify(query).setParameter(0, "myvalue1");
    verify(query).setParameter(1, 2);
  }

  @Test
  public void testExceptions() {
    SQLException sqlEx = new SQLException("argh", "27");

    final JDBCConnectionException jcex = new JDBCConnectionException("mymsg", sqlEx);
    try {
      hibernateTemplate.execute(session -> {
        throw jcex;
      });
      fail("Should have thrown DataAccessResourceFailureException");
    }
    catch (DataAccessResourceFailureException ex) {
      // expected
      assertEquals(jcex, ex.getCause());
      assertTrue(ex.getMessage().contains("mymsg"));
    }

    final SQLGrammarException sgex = new SQLGrammarException("mymsg", sqlEx);
    try {
      hibernateTemplate.execute(session -> {
        throw sgex;
      });
      fail("Should have thrown InvalidDataAccessResourceUsageException");
    }
    catch (InvalidDataAccessResourceUsageException ex) {
      // expected
      assertEquals(sgex, ex.getCause());
      assertTrue(ex.getMessage().contains("mymsg"));
    }

    final LockAcquisitionException laex = new LockAcquisitionException("mymsg", sqlEx);
    try {
      hibernateTemplate.execute(session -> {
        throw laex;
      });
      fail("Should have thrown CannotAcquireLockException");
    }
    catch (CannotAcquireLockException ex) {
      // expected
      assertEquals(laex, ex.getCause());
      assertTrue(ex.getMessage().contains("mymsg"));
    }

    final ConstraintViolationException cvex = new ConstraintViolationException("mymsg", sqlEx, "myconstraint");
    try {
      hibernateTemplate.execute(new HibernateCallback<Object>() {
        @Override
        public Object doInHibernate(Session session) {
          throw cvex;
        }
      });
      fail("Should have thrown DataIntegrityViolationException");
    }
    catch (DataIntegrityViolationException ex) {
      // expected
      assertEquals(cvex, ex.getCause());
      assertTrue(ex.getMessage().contains("mymsg"));
    }

    final DataException dex = new DataException("mymsg", sqlEx);
    try {
      hibernateTemplate.execute(session -> {
        throw dex;
      });
      fail("Should have thrown DataIntegrityViolationException");
    }
    catch (DataIntegrityViolationException ex) {
      // expected
      assertEquals(dex, ex.getCause());
      assertTrue(ex.getMessage().contains("mymsg"));
    }

    final JDBCException jdex = new JDBCException("mymsg", sqlEx);
    try {
      hibernateTemplate.execute(session -> {
        throw jdex;
      });
      fail("Should have thrown HibernateJdbcException");
    }
    catch (HibernateJdbcException ex) {
      // expected
      assertEquals(jdex, ex.getCause());
      assertTrue(ex.getMessage().contains("mymsg"));
    }

    final PropertyValueException pvex = new PropertyValueException("mymsg", "myentity", "myproperty");
    try {
      hibernateTemplate.execute(session -> {
        throw pvex;
      });
      fail("Should have thrown DataIntegrityViolationException");
    }
    catch (DataIntegrityViolationException ex) {
      // expected
      assertEquals(pvex, ex.getCause());
      assertTrue(ex.getMessage().contains("mymsg"));
    }

    try {
      hibernateTemplate.execute(session -> {
        throw new PersistentObjectException("");
      });
      fail("Should have thrown InvalidDataAccessApiUsageException");
    }
    catch (InvalidDataAccessApiUsageException ex) {
      // expected
    }

    try {
      hibernateTemplate.execute(session -> {
        throw new TransientObjectException("");
      });
      fail("Should have thrown InvalidDataAccessApiUsageException");
    }
    catch (InvalidDataAccessApiUsageException ex) {
      // expected
    }

    final ObjectDeletedException odex = new ObjectDeletedException("msg", "id", TestBean.class.getName());
    try {
      hibernateTemplate.execute(session -> {
        throw odex;
      });
      fail("Should have thrown InvalidDataAccessApiUsageException");
    }
    catch (InvalidDataAccessApiUsageException ex) {
      // expected
      assertEquals(odex, ex.getCause());
    }

    final QueryException qex = new QueryException("msg", "query");
    try {
      hibernateTemplate.execute(session -> {
        throw qex;
      });
      fail("Should have thrown InvalidDataAccessResourceUsageException");
    }
    catch (HibernateQueryException ex) {
      // expected
      assertEquals(qex, ex.getCause());
      assertEquals("query", ex.getQueryString());
    }

    final UnresolvableObjectException uoex = new UnresolvableObjectException("id", TestBean.class.getName());
    try {
      hibernateTemplate.execute(session -> {
        throw uoex;
      });
      fail("Should have thrown HibernateObjectRetrievalFailureException");
    }
    catch (HibernateObjectRetrievalFailureException ex) {
      // expected
      assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
      assertEquals("id", ex.getIdentifier());
      assertEquals(uoex, ex.getCause());
    }

    final ObjectNotFoundException onfe = new ObjectNotFoundException("id", TestBean.class.getName());
    try {
      hibernateTemplate.execute(session -> {
        throw onfe;
      });
      fail("Should have thrown HibernateObjectRetrievalFailureException");
    }
    catch (HibernateObjectRetrievalFailureException ex) {
      // expected
      assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
      assertEquals("id", ex.getIdentifier());
      assertEquals(onfe, ex.getCause());
    }

    final WrongClassException wcex = new WrongClassException("msg", "id", TestBean.class.getName());
    try {
      hibernateTemplate.execute(session -> {
        throw wcex;
      });
      fail("Should have thrown HibernateObjectRetrievalFailureException");
    }
    catch (HibernateObjectRetrievalFailureException ex) {
      // expected
      assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
      assertEquals("id", ex.getIdentifier());
      assertEquals(wcex, ex.getCause());
    }

    final NonUniqueResultException nuex = new NonUniqueResultException(2);
    try {
      hibernateTemplate.execute(session -> {
        throw nuex;
      });
      fail("Should have thrown IncorrectResultSizeDataAccessException");
    }
    catch (IncorrectResultSizeDataAccessException ex) {
      // expected
      assertEquals(1, ex.getExpectedSize());
      assertEquals(-1, ex.getActualSize());
    }

    final StaleObjectStateException sosex = new StaleObjectStateException(TestBean.class.getName(), "id");
    try {
      hibernateTemplate.execute(session -> {
        throw sosex;
      });
      fail("Should have thrown HibernateOptimisticLockingFailureException");
    }
    catch (HibernateOptimisticLockingFailureException ex) {
      // expected
      assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
      assertEquals("id", ex.getIdentifier());
      assertEquals(sosex, ex.getCause());
    }

    final StaleStateException ssex = new StaleStateException("msg");
    try {
      hibernateTemplate.execute(session -> {
        throw ssex;
      });
      fail("Should have thrown HibernateOptimisticLockingFailureException");
    }
    catch (HibernateOptimisticLockingFailureException ex) {
      // expected
      assertNull(ex.getPersistentClassName());
      assertNull(ex.getIdentifier());
      assertEquals(ssex, ex.getCause());
    }

    final HibernateException hex = new HibernateException("msg");
    try {
      hibernateTemplate.execute(session -> {
        throw hex;
      });
      fail("Should have thrown HibernateSystemException");
    }
    catch (HibernateSystemException ex) {
      // expected
      assertEquals(hex, ex.getCause());
    }
  }

}