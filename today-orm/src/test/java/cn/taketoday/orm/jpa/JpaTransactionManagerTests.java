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

package cn.taketoday.orm.jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.transaction.InvalidIsolationLevelException;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class JpaTransactionManagerTests {

  private EntityManagerFactory factory;

  private EntityManager manager;

  private EntityTransaction tx;

  private JpaTransactionManager tm;

  private TransactionTemplate tt;

  @BeforeEach
  public void setup() {
    factory = mock(EntityManagerFactory.class);
    manager = mock(EntityManager.class);
    tx = mock(EntityTransaction.class);

    tm = new JpaTransactionManager(factory);
    tt = new TransactionTemplate(tm);

    given(factory.createEntityManager()).willReturn(manager);
    given(manager.getTransaction()).willReturn(tx);
    given(manager.isOpen()).willReturn(true);
  }

  @AfterEach
  public void verifyTransactionSynchronizationManagerState() {
    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  public void testTransactionCommit() {
    given(manager.getTransaction()).willReturn(tx);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    Object result = tt.execute(status -> {
      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
      EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
      return l;
    });
    assertThat(result).isSameAs(l);

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(tx).commit();
    verify(manager).flush();
    verify(manager).close();
  }

  @Test
  public void testTransactionCommitWithRollbackException() {
    given(manager.getTransaction()).willReturn(tx);
    given(tx.getRollbackOnly()).willReturn(true);
    willThrow(new RollbackException()).given(tx).commit();

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    try {
      Object result = tt.execute(status -> {
        assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
        EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
        return l;
      });
      assertThat(result).isSameAs(l);
    }
    catch (TransactionSystemException tse) {
      // expected
      boolean condition = tse.getCause() instanceof RollbackException;
      assertThat(condition).isTrue();
    }

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(manager).flush();
    verify(manager).close();
  }

  @Test
  public void testTransactionRollback() {
    given(manager.getTransaction()).willReturn(tx);
    given(tx.isActive()).willReturn(true);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
            tt.execute(status -> {
              assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
              EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
              throw new RuntimeException("some exception");
            })).withMessage("some exception");

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(tx).rollback();
    verify(manager).close();
  }

  @Test
  public void testTransactionRollbackWithAlreadyRolledBack() {
    given(manager.getTransaction()).willReturn(tx);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
            tt.execute(status -> {
              assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
              EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
              throw new RuntimeException("some exception");
            }));

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(manager).close();
  }

  @Test
  public void testTransactionRollbackOnly() {
    given(manager.getTransaction()).willReturn(tx);
    given(tx.isActive()).willReturn(true);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    tt.execute(status -> {
      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

      EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
      status.setRollbackOnly();

      return l;
    });

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(manager).flush();
    verify(tx).rollback();
    verify(manager).close();
  }

  @Test
  public void testParticipatingTransactionWithCommit() {
    given(manager.getTransaction()).willReturn(tx);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    tt.execute(status -> {
      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

      return tt.execute(status1 -> {
        EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
        return l;
      });
    });

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(manager).flush();
    verify(tx).commit();
    verify(manager).close();
  }

  @Test
  public void testParticipatingTransactionWithRollback() {
    given(manager.getTransaction()).willReturn(tx);
    given(tx.isActive()).willReturn(true);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
            tt.execute(status -> {
              assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
              return tt.execute(status1 -> {
                EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
                throw new RuntimeException("some exception");
              });
            }));

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(tx).setRollbackOnly();
    verify(tx).rollback();
    verify(manager).close();
  }

  @Test
  public void testParticipatingTransactionWithRollbackOnly() {
    given(manager.getTransaction()).willReturn(tx);
    given(tx.isActive()).willReturn(true);
    given(tx.getRollbackOnly()).willReturn(true);
    willThrow(new RollbackException()).given(tx).commit();

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
                    tt.execute(status -> {
                      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

                      return tt.execute(status1 -> {
                        EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
                        status1.setRollbackOnly();
                        return null;
                      });
                    }))
            .withCauseInstanceOf(RollbackException.class);

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(manager).flush();
    verify(tx).setRollbackOnly();
    verify(manager).close();
  }

  @Test
  public void testParticipatingTransactionWithRequiresNew() {
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    given(factory.createEntityManager()).willReturn(manager);
    given(manager.getTransaction()).willReturn(tx);
    given(manager.isOpen()).willReturn(true);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    Object result = tt.execute(status -> {
      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
      return tt.execute(status1 -> {
        EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
        return l;
      });
    });
    assertThat(result).isSameAs(l);

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(manager).flush();
    verify(manager, times(2)).close();
    verify(tx, times(2)).begin();
  }

  @Test
  public void testParticipatingTransactionWithRequiresNewAndPrebound() {
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    given(manager.getTransaction()).willReturn(tx);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

    try {
      Object result = tt.execute(status -> {
        EntityManagerFactoryUtils.getTransactionalEntityManager(factory);

        assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
        return tt.execute(status1 -> {
          EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
          return l;
        });
      });
      assertThat(result).isSameAs(l);
    }
    finally {
      TransactionSynchronizationManager.unbindResource(factory);
    }

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(tx, times(2)).begin();
    verify(tx, times(2)).commit();
    verify(manager).flush();
    verify(manager).close();
  }

  @Test
  public void testPropagationSupportsAndRequiresNew() {
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    given(manager.getTransaction()).willReturn(tx);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    Object result = tt.execute(status -> {
      assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
      TransactionTemplate tt2 = new TransactionTemplate(tm);
      tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      return tt2.execute(status1 -> {
        EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
        return l;
      });
    });
    assertThat(result).isSameAs(l);

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(tx).commit();
    verify(manager).flush();
    verify(manager).close();
  }

  @Test
  public void testPropagationSupportsAndRequiresNewAndEarlyAccess() {
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    given(factory.createEntityManager()).willReturn(manager);
    given(manager.getTransaction()).willReturn(tx);
    given(manager.isOpen()).willReturn(true);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    Object result = tt.execute(status -> {
      EntityManagerFactoryUtils.getTransactionalEntityManager(factory);

      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
      TransactionTemplate tt2 = new TransactionTemplate(tm);
      tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      return tt2.execute(status1 -> {
        EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
        return l;
      });
    });
    assertThat(result).isSameAs(l);

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(tx).commit();
    verify(manager).flush();
    verify(manager, times(2)).close();
  }

  @Test
  public void testTransactionWithRequiresNewInAfterCompletion() {
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    EntityManager manager2 = mock(EntityManager.class);
    EntityTransaction tx2 = mock(EntityTransaction.class);

    given(manager.getTransaction()).willReturn(tx);
    given(factory.createEntityManager()).willReturn(manager, manager2);
    given(manager2.getTransaction()).willReturn(tx2);
    given(manager2.isOpen()).willReturn(true);

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    tt.execute(status -> {
      EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCompletion(int status) {
          tt.execute(status1 -> {
            EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
            return null;
          });
        }
      });
      return null;
    });

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(tx).commit();
    verify(tx2).begin();
    verify(tx2).commit();
    verify(manager).flush();
    verify(manager).close();
    verify(manager2).flush();
    verify(manager2).close();
  }

  @Test
  public void testTransactionCommitWithPropagationSupports() {
    given(manager.isOpen()).willReturn(true);

    final List<String> l = new ArrayList<>();
    l.add("test");

    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    Object result = tt.execute(status -> {
      boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
      assertThat(condition1).isTrue();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      boolean condition = !status.isNewTransaction();
      assertThat(condition).isTrue();
      EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
      return l;
    });
    assertThat(result).isSameAs(l);

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(manager).flush();
    verify(manager).close();
  }

  @Test
  public void testTransactionRollbackWithPropagationSupports() {
    given(manager.isOpen()).willReturn(true);

    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    tt.execute(status -> {
      boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
      assertThat(condition1).isTrue();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      boolean condition = !status.isNewTransaction();
      assertThat(condition).isTrue();
      EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
      status.setRollbackOnly();
      return null;
    });

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(manager).flush();
    verify(manager).close();
  }

  @Test
  public void testTransactionCommitWithPrebound() {
    given(manager.getTransaction()).willReturn(tx);

    final List<String> l = new ArrayList<>();
    l.add("test");

    boolean condition2 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition2).isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).isTrue();
    TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

    try {
      Object result = tt.execute(status -> {
        assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
        return l;
      });
      assertThat(result).isSameAs(l);

      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
      boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
      assertThat(condition).isTrue();
    }
    finally {
      TransactionSynchronizationManager.unbindResource(factory);
    }

    verify(tx).begin();
    verify(tx).commit();
  }

  @Test
  public void testTransactionRollbackWithPrebound() {
    given(manager.getTransaction()).willReturn(tx);
    given(tx.isActive()).willReturn(true);

    boolean condition2 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition2).isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).isTrue();
    TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

    try {
      tt.execute(status -> {
        assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
        status.setRollbackOnly();
        return null;
      });

      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
      boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
      assertThat(condition).isTrue();
    }
    finally {
      TransactionSynchronizationManager.unbindResource(factory);
    }

    verify(tx).begin();
    verify(tx).rollback();
    verify(manager).clear();
  }

  @Test
  public void testTransactionCommitWithPreboundAndPropagationSupports() {
    final List<String> l = new ArrayList<>();
    l.add("test");

    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    boolean condition2 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition2).isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).isTrue();
    TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

    try {
      Object result = tt.execute(status -> {
        assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        boolean condition = !status.isNewTransaction();
        assertThat(condition).isTrue();
        EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
        return l;
      });
      assertThat(result).isSameAs(l);

      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
      boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
      assertThat(condition).isTrue();
    }
    finally {
      TransactionSynchronizationManager.unbindResource(factory);
    }

    verify(manager).flush();
  }

  @Test
  public void testTransactionRollbackWithPreboundAndPropagationSupports() {
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    boolean condition2 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition2).isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).isTrue();
    TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

    try {
      tt.execute(status -> {
        assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        boolean condition = !status.isNewTransaction();
        assertThat(condition).isTrue();
        EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
        status.setRollbackOnly();
        return null;
      });

      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
      boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
      assertThat(condition).isTrue();
    }
    finally {
      TransactionSynchronizationManager.unbindResource(factory);
    }

    verify(manager).flush();
    verify(manager).clear();
  }

  @Test
  public void testInvalidIsolation() {
    tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

    given(manager.isOpen()).willReturn(true);

    assertThatExceptionOfType(InvalidIsolationLevelException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
            }));

    verify(manager).close();
  }

  @Test
  public void testTransactionFlush() {
    given(manager.getTransaction()).willReturn(tx);

    boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition3).isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
      status.flush();
    });

    boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
    assertThat(condition1).isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();

    verify(tx).commit();
    verify(manager).flush();
    verify(manager).close();
  }

}
