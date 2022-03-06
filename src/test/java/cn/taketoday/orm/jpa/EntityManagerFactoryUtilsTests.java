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

package cn.taketoday.orm.jpa;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.EmptyResultDataAccessException;
import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TransactionRequiredException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Costin Leau
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
public class EntityManagerFactoryUtilsTests {

  /*
   * Test method for
   * 'cn.taketoday.orm.jpa.EntityManagerFactoryUtils.doGetEntityManager(EntityManagerFactory)'
   */
  @Test
  public void testDoGetEntityManager() {
    // test null assertion
    assertThatIllegalArgumentException().isThrownBy(() ->
            EntityManagerFactoryUtils.doGetTransactionalEntityManager(null, null));
    EntityManagerFactory factory = mock(EntityManagerFactory.class);

    // no tx active
    assertThat(EntityManagerFactoryUtils.doGetTransactionalEntityManager(factory, null)).isNull();
    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
  }

  @Test
  public void testDoGetEntityManagerWithTx() throws Exception {
    try {
      EntityManagerFactory factory = mock(EntityManagerFactory.class);
      EntityManager manager = mock(EntityManager.class);

      TransactionSynchronizationManager.initSynchronization();
      given(factory.createEntityManager()).willReturn(manager);

      // no tx active
      assertThat(EntityManagerFactoryUtils.doGetTransactionalEntityManager(factory, null)).isSameAs(manager);
      Assertions.assertThat(((EntityManagerHolder) TransactionSynchronizationManager.unbindResource(factory)).getEntityManager()).isSameAs(manager);
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
  }

  @Test
  public void testTranslatesIllegalStateException() {
    IllegalStateException ise = new IllegalStateException();
    DataAccessException dex = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ise);
    assertThat(dex.getCause()).isSameAs(ise);
    boolean condition = dex instanceof InvalidDataAccessApiUsageException;
    assertThat(condition).isTrue();
  }

  @Test
  public void testTranslatesIllegalArgumentException() {
    IllegalArgumentException iae = new IllegalArgumentException();
    DataAccessException dex = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(iae);
    assertThat(dex.getCause()).isSameAs(iae);
    boolean condition = dex instanceof InvalidDataAccessApiUsageException;
    assertThat(condition).isTrue();
  }

  /**
   * We do not convert unknown exceptions. They may result from user code.
   */
  @Test
  public void testDoesNotTranslateUnfamiliarException() {
    UnsupportedOperationException userRuntimeException = new UnsupportedOperationException();
    assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(userRuntimeException)).as("Exception should not be wrapped").isNull();
  }

  /*
   * Test method for
   * 'cn.taketoday.orm.jpa.EntityManagerFactoryUtils.convertJpaAccessException(PersistenceException)'
   */
  @Test
  @SuppressWarnings("serial")
  public void testConvertJpaPersistenceException() {
    EntityNotFoundException entityNotFound = new EntityNotFoundException();
    assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(entityNotFound).getClass()).isSameAs(JpaObjectRetrievalFailureException.class);

    NoResultException noResult = new NoResultException();
    assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(noResult).getClass()).isSameAs(EmptyResultDataAccessException.class);

    NonUniqueResultException nonUniqueResult = new NonUniqueResultException();
    assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(nonUniqueResult).getClass()).isSameAs(IncorrectResultSizeDataAccessException.class);

    OptimisticLockException optimisticLock = new OptimisticLockException();
    assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(optimisticLock).getClass()).isSameAs(JpaOptimisticLockingFailureException.class);

    EntityExistsException entityExists = new EntityExistsException("foo");
    assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(entityExists).getClass()).isSameAs(DataIntegrityViolationException.class);

    TransactionRequiredException transactionRequired = new TransactionRequiredException("foo");
    assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(transactionRequired).getClass()).isSameAs(InvalidDataAccessApiUsageException.class);

    PersistenceException unknown = new PersistenceException() {
    };
    assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(unknown).getClass()).isSameAs(JpaSystemException.class);
  }

}
