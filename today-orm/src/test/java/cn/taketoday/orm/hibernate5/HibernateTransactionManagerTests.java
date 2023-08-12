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

import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.jdbc.datasource.ConnectionHolder;
import cn.taketoday.jdbc.datasource.DriverManagerDataSource;
import cn.taketoday.jdbc.datasource.LazyConnectionDataSourceProxy;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.CannotCreateTransactionException;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.UnexpectedRollbackException;
import cn.taketoday.transaction.jta.JtaTransactionManager;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionTemplate;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import static org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode.IMMEDIATE_ACQUISITION_AND_HOLD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willThrow;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
//@Disabled
@SuppressWarnings({ "rawtypes", "unchecked" })
public class HibernateTransactionManagerTests {

  @AfterEach
  public void tearDown() {
    assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
    assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
  }

  @Test
  public void testTransactionCommit() throws Exception {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);
    QueryImplementor query = mock(QueryImplementor.class);

    List list = new ArrayList();
    list.add("test");
    given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
    given(sf.openSession()).willReturn(session);
    given(session.getTransaction()).willReturn(tx);
    given(session.connection()).willReturn(con);
    given(session.isOpen()).willReturn(true);
    given(session.createQuery("some query string")).willReturn(query);
    given(query.list()).willReturn(list);
    given(session.isConnected()).willReturn(true);
    given(session.unwrap(SessionImplementor.class)).willReturn(session);

    apply(con, session);

    LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean() {
      @Override
      protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
        return sf;
      }
    };
    lsfb.afterPropertiesSet();
    SessionFactory sfProxy = lsfb.getObject();

    HibernateTransactionManager tm = new HibernateTransactionManager();
    tm.setSessionFactory(sfProxy);
    tm.setDataSource(ds);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    tt.setTimeout(10);
    assertFalse(TransactionSynchronizationManager.hasResource(sfProxy), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    Object result = tt.execute(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sfProxy), "Has thread session");
      assertTrue(TransactionSynchronizationManager.hasResource(ds), "Has thread connection");
      assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      Session session1 = ((SessionHolder) TransactionSynchronizationManager.getResource(sfProxy)).getSession();
      return session1.createQuery("some query string").list();
    });
    assertSame(result, list, "Correct result list");

    assertFalse(TransactionSynchronizationManager.hasResource(sfProxy), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    verify(tx).setTimeout(10);
    verify(tx).begin();
    verify(tx).commit();
    verify(session).close();
  }

  private void apply(@Nullable Connection con, ImplementingSession session) {
    JdbcCoordinator jdbcCoordinator = mock();
    LogicalConnectionImplementor connectionImplementor = mock();
    given(session.getJdbcCoordinator()).willReturn(jdbcCoordinator);
    given(jdbcCoordinator.getLogicalConnection()).willReturn(connectionImplementor);
    given(connectionImplementor.getConnectionHandlingMode()).willReturn(IMMEDIATE_ACQUISITION_AND_HOLD);
    given(connectionImplementor.getPhysicalConnection()).willReturn(con);
    given(connectionImplementor.isPhysicallyConnected()).willReturn(true);
    given(session.isOpen()).willReturn(true);
    given(session.unwrap(SessionImplementor.class)).willReturn(session);

  }

  @Test
  public void testTransactionRollback() throws Exception {
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);

    given(sf.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    given(session.isConnected()).willReturn(true);
    given(session.connection()).willReturn(con);

    apply(con, session);

    PlatformTransactionManager tm = new HibernateTransactionManager(sf);
    TransactionTemplate tt = new TransactionTemplate(tm);
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    try {
      tt.executeWithoutResult(status -> {
        assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
        throw new RuntimeException("application exception");
      });
      fail("Should have thrown RuntimeException");
    }
    catch (RuntimeException ex) {
      // expected
    }

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");
    verify(session).close();
    verify(tx).rollback();
  }

  @Test
  public void testTransactionRollbackOnly() throws Exception {
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);

    given(sf.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session.isConnected()).willReturn(true);
    given(session.connection()).willReturn(con);
    apply(con, session);

    PlatformTransactionManager tm = new HibernateTransactionManager(sf);
    TransactionTemplate tt = new TransactionTemplate(tm);
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");

    tt.execute(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
      Session session1 = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
      session1.flush();
      status.setRollbackOnly();
      return null;
    });

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    verify(session).flush();
    verify(session).close();
    verify(tx).rollback();
  }

  @Test
  public void testParticipatingTransactionWithCommit() throws Exception {
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);

    given(sf.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session.isConnected()).willReturn(true);
    given(session.connection()).willReturn(con);
    apply(con, session);

    LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean() {
      @Override
      protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
        return sf;
      }
    };
    lsfb.afterPropertiesSet();
    SessionFactory sfProxy = lsfb.getObject();

    PlatformTransactionManager tm = new HibernateTransactionManager(sfProxy);
    TransactionTemplate tt = new TransactionTemplate(tm);
    List l = new ArrayList();
    l.add("test");

    Object result = tt.execute(status -> tt.execute(status1 -> {
      Session session1 = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
      session1.flush();
      return l;
    }));
    assertSame(result, l, "Correct result list");

    verify(session).flush();
    verify(session).close();
    verify(tx).commit();
  }

  @Test
  public void testParticipatingTransactionWithRollback() throws Exception {
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);

    given(sf.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session.isConnected()).willReturn(true);
    given(session.connection()).willReturn(con);
    apply(con, session);

    PlatformTransactionManager tm = new HibernateTransactionManager(sf);
    TransactionTemplate tt = new TransactionTemplate(tm);
    try {
      tt.executeWithoutResult(status -> tt.executeWithoutResult(status1 -> {
        throw new RuntimeException("application exception");
      }));
      fail("Should have thrown RuntimeException");
    }
    catch (RuntimeException ex) {
      // expected
    }

    verify(session).close();
    verify(tx).rollback();
  }

  @Test
  public void testParticipatingTransactionWithRollbackOnly() throws Exception {
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);

    given(sf.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    given(session.isConnected()).willReturn(true);
    given(session.connection()).willReturn(con);
    apply(con, session);

    PlatformTransactionManager tm = new HibernateTransactionManager(sf);
    TransactionTemplate tt = new TransactionTemplate(tm);
    List l = new ArrayList();
    l.add("test");

    try {
      tt.executeWithoutResult(status -> tt.execute(status1 -> {
        status1.setRollbackOnly();
        return null;
      }));
      fail("Should have thrown UnexpectedRollbackException");
    }
    catch (UnexpectedRollbackException ex) {
      // expected
    }

    verify(session).close();
    verify(tx).rollback();
  }

  @Test
  public void testParticipatingTransactionWithRequiresNew() throws Exception {
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session1 = mock(ImplementingSession.class);
    ImplementingSession session2 = mock(ImplementingSession.class);
    Connection con = mock(Connection.class);
    Transaction tx = mock(Transaction.class);

    given(sf.openSession()).willReturn(session1, session2);
    given(session1.beginTransaction()).willReturn(tx);
    given(session1.isOpen()).willReturn(true);
    given(session2.beginTransaction()).willReturn(tx);
    given(session2.isOpen()).willReturn(true);
    given(session2.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session1.isConnected()).willReturn(true);
    given(session1.connection()).willReturn(con);
    given(session2.isConnected()).willReturn(true);
    given(session2.connection()).willReturn(con);
    apply(con, session1);
    apply(con, session2);

    PlatformTransactionManager tm = new HibernateTransactionManager(sf);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    tt.execute(status -> {
      SessionHolder holder = TransactionSynchronizationManager.getResource(sf);
      assertNotNull(holder, "Has thread session");
      assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      tt.execute(status1 -> {
        Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
        assertNotSame(session, holder.getSession(), "Not enclosing session");
        session.flush();
        assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        return null;
      });
      assertSame(holder.getSession(), ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession(), "Same thread session as before");
      assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      return null;
    });
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");

    verify(session2).flush();
    verify(session1).close();
    verify(session2).close();
    verify(tx, times(2)).commit();
  }

  @Test
  public void testParticipatingTransactionWithNotSupported() throws Exception {
    SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Connection con = mock(Connection.class);
    Transaction tx = mock(Transaction.class);
    ServiceRegistryImplementor registryImplementor = mock();
    given(sf.getServiceRegistry()).willReturn(registryImplementor);

    given(sf.openSession()).willReturn(session);
    given(session.getSessionFactory()).willReturn(sf);
    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    given(session.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session.isConnected()).willReturn(true);
    given(session.connection()).willReturn(con);
    apply(con, session);

    HibernateTransactionManager tm = new HibernateTransactionManager(sf);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    tt.execute(status -> {
      SessionHolder holder = TransactionSynchronizationManager.getResource(sf);
      assertNotNull(holder, "Has thread session");
      assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
      tt.execute(status1 -> {
        assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
        assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        return null;
      });
      assertSame(holder.getSession(), ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession(), "Same thread session as before");
      assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      return null;
    });
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");

    verify(session).close();
    verify(tx).commit();
  }

  @Test
  public void testTransactionWithPropagationSupports() throws Exception {
    SessionFactory sf = mock(SessionFactory.class);
    Session session = mock(Session.class);

    given(sf.openSession()).willReturn(session);
    given(session.getSessionFactory()).willReturn(sf);
    given(session.getHibernateFlushMode()).willReturn(FlushMode.MANUAL);

    LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean() {
      @Override
      protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
        return sf;
      }
    };
    lsfb.afterPropertiesSet();
    SessionFactory sfProxy = lsfb.getObject();

    PlatformTransactionManager tm = new HibernateTransactionManager(sfProxy);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    assertFalse(TransactionSynchronizationManager.hasResource(sfProxy), "Hasn't thread session");

    tt.execute(status -> {
      assertFalse(TransactionSynchronizationManager.hasResource(sfProxy), "Hasn't thread session");
      assertFalse(status.isNewTransaction(), "Is not new transaction");
      assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
      Session session1 = sf.openSession();
      session1.flush();
      session1.close();
      return null;
    });

    assertFalse(TransactionSynchronizationManager.hasResource(sfProxy), "Hasn't thread session");
    InOrder ordered = inOrder(session);
    ordered.verify(session).flush();
    ordered.verify(session).close();
  }

  @Test
  public void testTransactionWithPropagationSupportsAndCurrentSession() throws Exception {
    SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
    ImplementingSession session = mock(ImplementingSession.class);
    given(sf.openSession()).willReturn(session);
    given(session.getSessionFactory()).willReturn(sf);
    given(session.getHibernateFlushMode()).willReturn(FlushMode.MANUAL);
    apply(null, session);

    ServiceRegistryImplementor registryImplementor = mock();
    given(sf.getServiceRegistry()).willReturn(registryImplementor);

    LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean() {
      @Override
      protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
        return sf;
      }
    };
    lsfb.afterPropertiesSet();
    SessionFactory sfProxy = lsfb.getObject();

    PlatformTransactionManager tm = new HibernateTransactionManager(sfProxy);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    assertFalse(TransactionSynchronizationManager.hasResource(sfProxy), "Hasn't thread session");

    tt.execute(status -> {
      assertFalse(TransactionSynchronizationManager.hasResource(sfProxy), "Hasn't thread session");
      assertFalse(status.isNewTransaction(), "Is not new transaction");
      assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
      Session session1 = new HibernateSessionContext(sf).currentSession();
      assertTrue(TransactionSynchronizationManager.hasResource(sfProxy), "Has thread session");
      session1.flush();
      return null;
    });

    assertFalse(TransactionSynchronizationManager.hasResource(sfProxy), "Hasn't thread session");
    InOrder ordered = inOrder(session);
    ordered.verify(session).flush();
    ordered.verify(session).close();
  }

  @Test
  public void testTransactionWithPropagationSupportsAndInnerTransaction() throws Exception {
    SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
    ImplementingSession session1 = mock(ImplementingSession.class);
    ImplementingSession session2 = mock(ImplementingSession.class);
    Connection con = mock(Connection.class);
    Transaction tx = mock(Transaction.class);

    given(sf.openSession()).willReturn(session1, session2);
    given(session1.getSessionFactory()).willReturn(sf);
    given(session1.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session2.beginTransaction()).willReturn(tx);
    given(session2.connection()).willReturn(con);
    given(session2.getHibernateFlushMode()).willReturn(FlushMode.AUTO);
    given(session2.isOpen()).willReturn(true);
    given(session2.isConnected()).willReturn(true);
    apply(con, session1);
    apply(con, session2);
    ServiceRegistryImplementor registryImplementor = mock();
    given(sf.getServiceRegistry()).willReturn(registryImplementor);

    LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean() {
      @Override
      protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
        return sf;
      }
    };
    lsfb.afterPropertiesSet();
    SessionFactory sfProxy = lsfb.getObject();

    PlatformTransactionManager tm = new HibernateTransactionManager(sfProxy);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    TransactionTemplate tt2 = new TransactionTemplate(tm);
    tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    tt.execute(status -> {
      assertFalse(TransactionSynchronizationManager.hasResource(sfProxy), "Hasn't thread session");
      assertFalse(status.isNewTransaction(), "Is not new transaction");
      assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
      Session session = sfProxy.openSession();
      assertSame(session1, session);
      tt2.execute(status1 -> {
        assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        Session session3 = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
        assertSame(session2, session3);
        session3.flush();
        return null;
      });
      session.flush();
      session.close();
      assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
      return null;
    });
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");

    verify(session1).flush();
    verify(session1).close();
    verify(session2).flush();
    verify(session2).close();
    verify(tx).commit();
  }

  @Test
  public void testTransactionCommitWithEntityInterceptor() throws Exception {
    Interceptor entityInterceptor = mock(Interceptor.class);
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    SessionBuilder options = mock(SessionBuilder.class);
    Transaction tx = mock(Transaction.class);

    given(sf.withOptions()).willReturn(options);
    given(options.interceptor(entityInterceptor)).willReturn(options);
    given(options.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    given(session.isConnected()).willReturn(true);
    given(session.connection()).willReturn(con);
    apply(con, session);

    HibernateTransactionManager tm = new HibernateTransactionManager(sf);
    tm.setEntityInterceptor(entityInterceptor);
    tm.setAllowResultAccessAfterCompletion(true);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    tt.executeWithoutResult(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
    });

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    verify(session).close();
    verify(tx).commit();
  }

  @Test
  public void testTransactionCommitWithEntityInterceptorBeanName() throws Exception {
    Interceptor entityInterceptor = mock(Interceptor.class);
    Interceptor entityInterceptor2 = mock(Interceptor.class);
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    SessionBuilder options = mock(SessionBuilder.class);
    Transaction tx = mock(Transaction.class);

    given(sf.withOptions()).willReturn(options);
    given(options.interceptor(entityInterceptor)).willReturn(options);
    given(options.interceptor(entityInterceptor2)).willReturn(options);
    given(options.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    given(session.isConnected()).willReturn(true);
    given(session.connection()).willReturn(con);
    apply(con, session);

    BeanFactory beanFactory = mock(BeanFactory.class);
    given(beanFactory.getBean("entityInterceptor", Interceptor.class)).willReturn(
            entityInterceptor, entityInterceptor2);

    HibernateTransactionManager tm = new HibernateTransactionManager(sf);
    tm.setEntityInterceptorBeanName("entityInterceptor");
    tm.setBeanFactory(beanFactory);

    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    for (int i = 0; i < 2; i++) {
      tt.executeWithoutResult(status -> {
        assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
      });
    }

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    verify(session, times(2)).close();
    verify(tx, times(2)).commit();
  }

  @Test
  public void testTransactionCommitWithReadOnly() throws Exception {
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);
    QueryImplementor query = mock(QueryImplementor.class);

    List list = new ArrayList();
    list.add("test");
    given(sf.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    given(session.connection()).willReturn(con);
    given(session.isOpen()).willReturn(true);
    given(session.createQuery("some query string")).willReturn(query);
    given(query.list()).willReturn(list);
    given(session.isConnected()).willReturn(true);
    given(con.isReadOnly()).willReturn(true);
    apply(con, session);

    HibernateTransactionManager tm = new HibernateTransactionManager(sf);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setReadOnly(true);
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    Object result = tt.execute(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
      assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      Session session1 = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
      return session1.createQuery("some query string").list();
    });
    assertSame(result, list, "Correct result list");

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    verify(session).setHibernateFlushMode(FlushMode.MANUAL);
    verify(con).setReadOnly(true);
    verify(tx).commit();
    verify(con).setReadOnly(false);
    verify(session).close();
  }

  @Test
  public void testTransactionCommitWithFlushFailure() throws Exception {
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);

    given(sf.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    SQLException sqlEx = new SQLException("argh", "27");
    Exception rootCause = null;
    ConstraintViolationException jdbcEx = new ConstraintViolationException("mymsg", sqlEx, null);
    rootCause = jdbcEx;
    willThrow(jdbcEx).given(tx).commit();
    given(session.isConnected()).willReturn(true);
    given(session.connection()).willReturn(con);
    apply(con, session);

    HibernateTransactionManager tm = new HibernateTransactionManager(sf);
    TransactionTemplate tt = new TransactionTemplate(tm);
    List l = new ArrayList();
    l.add("test");
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    try {
      tt.execute(status -> {
        assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
        return l;
      });
      fail("Should have thrown DataIntegrityViolationException");
    }
    catch (DataIntegrityViolationException ex) {
      // expected
      assertEquals(rootCause, ex.getCause());
      assertTrue(ex.getMessage().contains("mymsg"));
    }

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    verify(session).close();
    verify(tx).rollback();
  }

  @Test
  public void testTransactionCommitWithPreBound() throws Exception {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);

    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    given(session.getHibernateFlushMode()).willReturn(FlushMode.MANUAL);
    given(session.connection()).willReturn(con);
    given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
    given(session.isConnected()).willReturn(true);
    apply(con, session);

    HibernateTransactionManager tm = new HibernateTransactionManager();
    tm.setSessionFactory(sf);
    tm.setDataSource(ds);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    List l = new ArrayList();
    l.add("test");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");
    TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
    assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");

    Object result = tt.execute(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
      assertTrue(TransactionSynchronizationManager.hasResource(ds), "Has thread connection");
      SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sf);
      assertNotNull(sessionHolder.getTransaction(), "Has thread transaction");
      Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
      assertEquals(session, sess);
      return l;
    });
    assertSame(result, l, "Correct result list");

    assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
    SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sf);
    assertNull(sessionHolder.getTransaction(), "Hasn't thread transaction");
    TransactionSynchronizationManager.unbindResource(sf);
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    InOrder ordered = inOrder(session, con);
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    ordered.verify(session).setHibernateFlushMode(FlushMode.AUTO);
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    ordered.verify(session).setHibernateFlushMode(FlushMode.MANUAL);
    verify(tx).commit();
//    verify(session).disconnect();
  }

  @Test
  public void testTransactionCommitWithPreBoundAndResultAccessAfterCommit() throws Exception {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);

    given(session.beginTransaction()).willReturn(tx);
    given(session.isOpen()).willReturn(true);
    given(session.getHibernateFlushMode()).willReturn(FlushMode.MANUAL);
    given(session.connection()).willReturn(con);
    given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
    given(con.getHoldability()).willReturn(ResultSet.CLOSE_CURSORS_AT_COMMIT);
    given(session.isConnected()).willReturn(true);
    apply(con, session);

    HibernateTransactionManager tm = new HibernateTransactionManager();
    tm.setSessionFactory(sf);
    tm.setDataSource(ds);
    tm.setAllowResultAccessAfterCompletion(true);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    List l = new ArrayList();
    l.add("test");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");
    TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
    assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");

    Object result = tt.execute(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
      assertTrue(TransactionSynchronizationManager.hasResource(ds), "Has thread connection");
      SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sf);
      assertNotNull(sessionHolder.getTransaction(), "Has thread transaction");
      Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
      assertEquals(session, sess);
      return l;
    });
    assertSame(result, l, "Correct result list");

    assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
    SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sf);
    assertNull(sessionHolder.getTransaction(), "Hasn't thread transaction");
    TransactionSynchronizationManager.unbindResource(sf);
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    InOrder ordered = inOrder(session, con);
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    ordered.verify(con).setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
    ordered.verify(session).setHibernateFlushMode(FlushMode.AUTO);
    ordered.verify(con).setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    ordered.verify(session).setHibernateFlushMode(FlushMode.MANUAL);
    verify(tx).commit();
  }

  @Test
  public void testTransactionRollbackWithPreBound() throws Exception {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx1 = mock(Transaction.class);
    Transaction tx2 = mock(Transaction.class);

    given(session.beginTransaction()).willReturn(tx1, tx2);
    given(session.isOpen()).willReturn(true);
    given(session.getHibernateFlushMode()).willReturn(FlushMode.MANUAL);
    given(session.isConnected()).willReturn(true);
    given(session.connection()).willReturn(con);
    apply(con, session);

    HibernateTransactionManager tm = new HibernateTransactionManager();
    tm.setSessionFactory(sf);
    tm.setDataSource(ds);
    TransactionTemplate tt = new TransactionTemplate(tm);
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");
    TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
    assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");

    try {
      tt.executeWithoutResult(status -> {
        assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
        assertTrue(TransactionSynchronizationManager.hasResource(ds), "Has thread connection");
        SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sf);
        assertEquals(tx1, sessionHolder.getTransaction());
        tt.executeWithoutResult(status1 -> {
          status1.setRollbackOnly();
          Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
          assertEquals(session, sess);
        });
      });
      fail("Should have thrown UnexpectedRollbackException");
    }
    catch (UnexpectedRollbackException ex) {
      // expected
    }

    assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
    SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sf);
    assertNull(sessionHolder.getTransaction(), "Hasn't thread transaction");
    assertFalse(sessionHolder.isRollbackOnly(), "Not marked rollback-only");

    tt.executeWithoutResult(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
      assertTrue(TransactionSynchronizationManager.hasResource(ds), "Has thread connection");
      SessionHolder sessionHolder1 = TransactionSynchronizationManager.getResource(sf);
      assertEquals(tx2, sessionHolder1.getTransaction());
      Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
      assertEquals(session, sess);
    });

    assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
    assertNull(sessionHolder.getTransaction(), "Hasn't thread transaction");
    TransactionSynchronizationManager.unbindResource(sf);
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    verify(tx1).rollback();
    verify(tx2).commit();
    InOrder ordered = inOrder(session);
    ordered.verify(session).clear();
    ordered.verify(session).setHibernateFlushMode(FlushMode.AUTO);
    ordered.verify(session).setHibernateFlushMode(FlushMode.MANUAL);
//    ordered.verify(session).disconnect();
  }

  @Test
  public void testTransactionRollbackWithHibernateManagedSession() throws Exception {
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx1 = mock(Transaction.class);
    Transaction tx2 = mock(Transaction.class);

    given(sf.getCurrentSession()).willReturn(session);
    given(session.isOpen()).willReturn(true);
    given(session.getTransaction()).willReturn(tx1, tx2);
    given(session.beginTransaction()).willReturn(tx1, tx2);
    given(session.getHibernateFlushMode()).willReturn(FlushMode.MANUAL);
    apply(null, session);

    HibernateTransactionManager tm = new HibernateTransactionManager();
    tm.setSessionFactory(sf);
    tm.setPrepareConnection(false);
    tm.setHibernateManagedSession(true);
    TransactionTemplate tt = new TransactionTemplate(tm);

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");

    try {
      tt.executeWithoutResult(status -> {
        assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
        tt.executeWithoutResult(status1 -> {
          status1.setRollbackOnly();
          Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
          assertEquals(session, sess);
        });
      });
      fail("Should have thrown UnexpectedRollbackException");
    }
    catch (UnexpectedRollbackException ex) {
      // expected
    }

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");

    tt.executeWithoutResult(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
      Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
      assertEquals(session, sess);
    });

    verify(tx1).rollback();
    verify(tx2).commit();
    InOrder ordered = inOrder(session);
    ordered.verify(session).setHibernateFlushMode(FlushMode.AUTO);
    ordered.verify(session).setHibernateFlushMode(FlushMode.MANUAL);
  }

  @Test
  public void testExistingTransactionWithPropagationNestedAndRollback() throws Exception {
    doTestExistingTransactionWithPropagationNestedAndRollback(false);
  }

  @Test
  public void testExistingTransactionWithManualSavepointAndRollback() throws Exception {
    doTestExistingTransactionWithPropagationNestedAndRollback(true);
  }

  private void doTestExistingTransactionWithPropagationNestedAndRollback(boolean manualSavepoint)
          throws Exception {

    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    DatabaseMetaData md = mock(DatabaseMetaData.class);
    Savepoint sp = mock(Savepoint.class);
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);
    QueryImplementor query = mock(QueryImplementor.class);

    apply(con, session);

    List list = new ArrayList();
    list.add("test");
    given(sf.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    given(session.connection()).willReturn(con);
    given(session.isOpen()).willReturn(true);
    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + 1)).willReturn(sp);
    given(session.createQuery("some query string")).willReturn(query);
    given(query.list()).willReturn(list);
    given(session.isConnected()).willReturn(true);

    HibernateTransactionManager tm = new HibernateTransactionManager();
    tm.setNestedTransactionAllowed(true);
    tm.setSessionFactory(sf);
    tm.setDataSource(ds);
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    Object result = tt.execute(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
      assertTrue(TransactionSynchronizationManager.hasResource(ds), "Has thread connection");
      if (manualSavepoint) {
        Object savepoint = status.createSavepoint();
        status.rollbackToSavepoint(savepoint);
      }
      else {
        tt.executeWithoutResult(status1 -> {
          assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
          assertTrue(TransactionSynchronizationManager.hasResource(ds), "Has thread connection");
          status1.setRollbackOnly();
        });
      }
      Session session1 = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
      return session1.createQuery("some query string").list();
    });
    assertSame(result, list, "Correct result list");

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    verify(con).setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + 1);
    verify(con).rollback(sp);
    verify(session).close();
    verify(tx).commit();
  }

  @Test
  public void testTransactionCommitWithNonExistingDatabase() throws Exception {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
    lsfb.setDataSource(ds);
    Properties props = new Properties();
    props.setProperty("hibernate.dialect", HSQLDialect.class.getName());
    lsfb.setHibernateProperties(props);
    lsfb.afterPropertiesSet();
    SessionFactory sf = lsfb.getObject();

    HibernateTransactionManager tm = new HibernateTransactionManager();
    tm.setSessionFactory(sf);
    tm.afterPropertiesSet();
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    tt.setTimeout(10);
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    try {
      tt.execute(status -> {
        assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
        assertTrue(TransactionSynchronizationManager.hasResource(ds), "Has thread connection");
        Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
        return session.createQuery("from java.lang.Object").list();
      });
      fail("Should have thrown CannotCreateTransactionException");
    }
    catch (CannotCreateTransactionException ex) {
      // expected
    }

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");
  }

  @Test
  public void testTransactionCommitWithPreBoundSessionAndNonExistingDatabase() throws Exception {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
    lsfb.setDataSource(ds);
    Properties props = new Properties();
    props.setProperty("hibernate.dialect", HSQLDialect.class.getName());
    lsfb.setHibernateProperties(props);
    lsfb.afterPropertiesSet();
    SessionFactory sf = lsfb.getObject();

    HibernateTransactionManager tm = new HibernateTransactionManager();
    tm.setSessionFactory(sf);
    tm.afterPropertiesSet();
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    tt.setTimeout(10);
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    Session session = sf.openSession();
    try (session) {
      TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
      tt.execute(status -> {
        assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
        assertTrue(TransactionSynchronizationManager.hasResource(ds), "Has thread connection");
        Session session1 = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
        return session1.createQuery("from java.lang.Object").list();
      });
      fail("Should have thrown CannotCreateTransactionException");
    }
    catch (CannotCreateTransactionException ex) {
      // expected
      SessionHolder holder = TransactionSynchronizationManager.getResource(sf);
      assertFalse(holder.isSynchronizedWithTransaction());
    }
    finally {
      TransactionSynchronizationManager.unbindResource(sf);
    }

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");
  }

  @Test
  public void testTransactionCommitWithNonExistingDatabaseAndLazyConnection() throws Exception {
    DriverManagerDataSource dsTarget = new DriverManagerDataSource();
    LazyConnectionDataSourceProxy ds = new LazyConnectionDataSourceProxy();
    ds.setTargetDataSource(dsTarget);
    ds.setDefaultAutoCommit(true);
    ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    //ds.setDefaultTransactionIsolationName("TRANSACTION_READ_COMMITTED");

    LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
    lsfb.setDataSource(ds);
    Properties props = new Properties();
    props.setProperty("hibernate.dialect", HSQLDialect.class.getName());
    props.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
    lsfb.setHibernateProperties(props);
    lsfb.afterPropertiesSet();
    SessionFactory sf = lsfb.getObject();

    HibernateTransactionManager tm = new HibernateTransactionManager();
    tm.setSessionFactory(sf);
    tm.afterPropertiesSet();
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    tt.setTimeout(10);
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    tt.execute(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
      assertTrue(TransactionSynchronizationManager.hasResource(ds), "Has thread connection");
      Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
      return session.createQuery("from java.lang.Object").list();
    });

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.hasResource(ds), "Hasn't thread connection");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");
  }

  @Test
  public void testTransactionFlush() throws Exception {
    SessionFactory sf = mock(SessionFactory.class);
    ImplementingSession session = mock(ImplementingSession.class);
    Transaction tx = mock(Transaction.class);

    given(sf.openSession()).willReturn(session);
    given(session.beginTransaction()).willReturn(tx);
    apply(null, session);

    HibernateTransactionManager tm = new HibernateTransactionManager(sf);
    tm.setPrepareConnection(false);
    TransactionTemplate tt = new TransactionTemplate(tm);
    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    tt.executeWithoutResult(status -> {
      assertTrue(TransactionSynchronizationManager.hasResource(sf), "Has thread session");
      assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      status.flush();
    });

    assertFalse(TransactionSynchronizationManager.hasResource(sf), "Hasn't thread session");
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(), "JTA synchronizations not active");

    verify(session).flush();
    verify(tx).commit();
    verify(session).close();
  }

  @Test
  public void testSetJtaTransactionManager() throws Exception {
    DataSource ds = mock(DataSource.class);
    TransactionManager tm = mock(TransactionManager.class);
    UserTransaction ut = mock(UserTransaction.class);
    TransactionSynchronizationRegistry tsr = mock(TransactionSynchronizationRegistry.class);
    JtaTransactionManager jtm = new JtaTransactionManager();
    jtm.setTransactionManager(tm);
    jtm.setUserTransaction(ut);
    jtm.setTransactionSynchronizationRegistry(tsr);
    LocalSessionFactoryBuilder lsfb = new LocalSessionFactoryBuilder(ds);
    lsfb.setJtaTransactionManager(jtm);
    Object jtaPlatform = lsfb.getProperties().get(AvailableSettings.JTA_PLATFORM);
    assertNotNull(jtaPlatform);
    assertSame(tm, jtaPlatform.getClass().getMethod("retrieveTransactionManager").invoke(jtaPlatform));
    assertSame(ut, jtaPlatform.getClass().getMethod("retrieveUserTransaction").invoke(jtaPlatform));
//    assertTrue(lsfb.getProperties().get(AvailableSettings.TRANSACTION_STRATEGY) instanceof CMTTransactionFactory);
  }

  @Test
  public void testSetTransactionManager() throws Exception {
    DataSource ds = mock(DataSource.class);
    TransactionManager tm = mock(TransactionManager.class);
    LocalSessionFactoryBuilder lsfb = new LocalSessionFactoryBuilder(ds);
    lsfb.setJtaTransactionManager(tm);
    Object jtaPlatform = lsfb.getProperties().get(AvailableSettings.JTA_PLATFORM);
    assertNotNull(jtaPlatform);
    assertSame(tm, jtaPlatform.getClass().getMethod("retrieveTransactionManager").invoke(jtaPlatform));
//    assertTrue(lsfb.getProperties().get(AvailableSettings.TRANSACTION_STRATEGY) instanceof CMTTransactionFactory);
  }

  public interface ImplementingSession extends Session, SessionImplementor {
  }

}