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

package cn.taketoday.orm.jpa.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.jpa.JpaTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.SynchronizationType;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 4.1.2
 */
public class PersistenceContextTransactionTests {

  private EntityManagerFactory factory;

  private EntityManager manager;

  private EntityTransaction tx;

  private TransactionTemplate tt;

  private EntityManagerHoldingBean bean;

  @BeforeEach
  public void setup() {
    factory = mock(EntityManagerFactory.class);
    manager = mock(EntityManager.class);
    tx = mock(EntityTransaction.class);

    JpaTransactionManager tm = new JpaTransactionManager(factory);
    tt = new TransactionTemplate(tm);

    given(factory.createEntityManager()).willReturn(manager);
    given(manager.getTransaction()).willReturn(tx);
    given(manager.isOpen()).willReturn(true);

    bean = new EntityManagerHoldingBean();
    @SuppressWarnings("serial")
    PersistenceAnnotationBeanPostProcessor pabpp = new PersistenceAnnotationBeanPostProcessor() {
      @Override
      protected EntityManagerFactory findEntityManagerFactory(@Nullable String unitName, String requestingBeanName) {
        return factory;
      }
    };
    pabpp.postProcessProperties(null, bean, "bean");

    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

  @AfterEach
  public void clear() {
    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  public void testTransactionCommitWithSharedEntityManager() {
    given(manager.getTransaction()).willReturn(tx);

    tt.execute(status -> {
      bean.sharedEntityManager.flush();
      return null;
    });

    verify(tx).commit();
    verify(manager).flush();
    verify(manager).close();
  }

  @Test
  public void testTransactionCommitWithSharedEntityManagerAndPropagationSupports() {
    given(manager.isOpen()).willReturn(true);

    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    tt.execute(status -> {
      bean.sharedEntityManager.clear();
      return null;
    });

    verify(manager).clear();
    verify(manager).close();
  }

  @Test
  public void testTransactionCommitWithExtendedEntityManager() {
    given(manager.getTransaction()).willReturn(tx);

    tt.execute(status -> {
      bean.extendedEntityManager.flush();
      return null;
    });

    verify(tx, times(2)).commit();
    verify(manager).flush();
    verify(manager).close();
  }

  @Test
  public void testTransactionCommitWithExtendedEntityManagerAndPropagationSupports() {
    given(manager.isOpen()).willReturn(true);

    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    tt.execute(status -> {
      bean.extendedEntityManager.flush();
      return null;
    });

    verify(manager).flush();
  }

  @Test
  public void testTransactionCommitWithSharedEntityManagerUnsynchronized() {
    given(manager.getTransaction()).willReturn(tx);

    tt.execute(status -> {
      bean.sharedEntityManagerUnsynchronized.flush();
      return null;
    });

    verify(tx).commit();
    verify(manager).flush();
    verify(manager, times(2)).close();
  }

  @Test
  public void testTransactionCommitWithSharedEntityManagerUnsynchronizedAndPropagationSupports() {
    given(manager.isOpen()).willReturn(true);

    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    tt.execute(status -> {
      bean.sharedEntityManagerUnsynchronized.clear();
      return null;
    });

    verify(manager).clear();
    verify(manager).close();
  }

  @Test
  public void testTransactionCommitWithExtendedEntityManagerUnsynchronized() {
    given(manager.getTransaction()).willReturn(tx);

    tt.execute(status -> {
      bean.extendedEntityManagerUnsynchronized.flush();
      return null;
    });

    verify(tx).commit();
    verify(manager).flush();
    verify(manager).close();
  }

  @Test
  public void testTransactionCommitWithExtendedEntityManagerUnsynchronizedAndPropagationSupports() {
    given(manager.isOpen()).willReturn(true);

    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    tt.execute(status -> {
      bean.extendedEntityManagerUnsynchronized.flush();
      return null;
    });

    verify(manager).flush();
  }

  @Test
  public void testTransactionCommitWithSharedEntityManagerUnsynchronizedJoined() {
    given(manager.getTransaction()).willReturn(tx);

    tt.execute(status -> {
      bean.sharedEntityManagerUnsynchronized.joinTransaction();
      bean.sharedEntityManagerUnsynchronized.flush();
      return null;
    });

    verify(tx).commit();
    verify(manager).flush();
    verify(manager, times(2)).close();
  }

  @Test
  public void testTransactionCommitWithExtendedEntityManagerUnsynchronizedJoined() {
    given(manager.getTransaction()).willReturn(tx);

    tt.execute(status -> {
      bean.extendedEntityManagerUnsynchronized.joinTransaction();
      bean.extendedEntityManagerUnsynchronized.flush();
      return null;
    });

    verify(tx, times(2)).commit();
    verify(manager).flush();
    verify(manager).close();
  }

  @Test
  public void testTransactionCommitWithExtendedEntityManagerUnsynchronizedJoinedAndPropagationSupports() {
    given(manager.isOpen()).willReturn(true);

    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    tt.execute(status -> {
      bean.extendedEntityManagerUnsynchronized.joinTransaction();
      bean.extendedEntityManagerUnsynchronized.flush();
      return null;
    });

    verify(manager).flush();
  }

  public static class EntityManagerHoldingBean {

    @PersistenceContext
    public EntityManager sharedEntityManager;

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    public EntityManager extendedEntityManager;

    @PersistenceContext(synchronization = SynchronizationType.UNSYNCHRONIZED)
    public EntityManager sharedEntityManagerUnsynchronized;

    @PersistenceContext(type = PersistenceContextType.EXTENDED, synchronization = SynchronizationType.UNSYNCHRONIZED)
    public EntityManager extendedEntityManagerUnsynchronized;
  }

}
