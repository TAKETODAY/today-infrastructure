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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceUnitInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Superclass for unit tests for EntityManagerFactory-creating beans.
 * Note: Subclasses must set expectations on the mock EntityManagerFactory.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
public abstract class AbstractEntityManagerFactoryBeanTests {

  protected static EntityManagerFactory mockEmf;

  @BeforeEach
  public void setUp() throws Exception {
    mockEmf = mock(EntityManagerFactory.class);
  }

  @AfterEach
  public void tearDown() throws Exception {
    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  protected void checkInvariants(AbstractEntityManagerFactoryBean demf) {
    assertThat(EntityManagerFactory.class.isAssignableFrom(demf.getObjectType())).isTrue();
    Object gotObject = demf.getObject();
    boolean condition = gotObject instanceof EntityManagerFactoryInfo;
    assertThat(condition).as("Object created by factory implements EntityManagerFactoryInfo").isTrue();
    EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) demf.getObject();
    assertThat(demf.getObject()).as("Successive invocations of getObject() return same object").isSameAs(emfi);
    assertThat(demf.getObject()).isSameAs(emfi);
    assertThat(mockEmf).isSameAs(emfi.getNativeEntityManagerFactory());
  }

  protected static class DummyEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

    private static final long serialVersionUID = 1L;

    private final EntityManagerFactory emf;

    public DummyEntityManagerFactoryBean(EntityManagerFactory emf) {
      this.emf = emf;
    }

    @Override
    protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
      return emf;
    }

    @Override
    public PersistenceUnitInfo getPersistenceUnitInfo() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getPersistenceUnitName() {
      return "test";
    }
  }

}
