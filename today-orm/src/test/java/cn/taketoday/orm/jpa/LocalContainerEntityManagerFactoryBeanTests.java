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

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.instrument.InstrumentationLoadTimeWeaver;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.OptimisticLockingFailureException;
import cn.taketoday.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import cn.taketoday.orm.jpa.testfixture.SerializationTestUtils;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.interceptor.DefaultTransactionAttribute;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import jakarta.persistence.spi.ProviderUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
@SuppressWarnings("rawtypes")
public class LocalContainerEntityManagerFactoryBeanTests extends AbstractEntityManagerFactoryBeanTests {

  // Static fields set by inner class DummyPersistenceProvider

  private static Map actualProps;

  private static PersistenceUnitInfo actualPui;

  @Test
  public void testValidPersistenceUnit() throws Exception {
    parseValidPersistenceUnit();
  }

  @Test
  public void testExceptionTranslationWithNoDialect() throws Exception {
    LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
    cefb.getObject();
    assertThat(cefb.getJpaDialect()).as("No dialect set").isNull();

    RuntimeException in1 = new RuntimeException("in1");
    PersistenceException in2 = new PersistenceException();
    assertThat(cefb.translateExceptionIfPossible(in1)).as("No translation here").isNull();
    DataAccessException dex = cefb.translateExceptionIfPossible(in2);
    assertThat(dex).isNotNull();
    assertThat(dex.getCause()).isSameAs(in2);
  }

  @Test
  public void testEntityManagerFactoryIsProxied() throws Exception {
    LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
    EntityManagerFactory emf = cefb.getObject();
    assertThat(cefb.getObject()).as("EntityManagerFactory reference must be cached after init").isSameAs(emf);

    assertThat(emf).as("EMF must be proxied").isNotSameAs(mockEmf);
    assertThat(emf.equals(emf)).isTrue();

    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setSerializationId("emf-bf");
    bf.registerSingleton("emf", cefb);
    cefb.setBeanFactory(bf);
    cefb.setBeanName("emf");
    assertThat(SerializationTestUtils.serializeAndDeserialize(emf)).isNotNull();
  }

  @Test
  public void testApplicationManagedEntityManagerWithoutTransaction() throws Exception {
    Object testEntity = new Object();
    EntityManager mockEm = mock(EntityManager.class);

    given(mockEmf.createEntityManager()).willReturn(mockEm);

    LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
    EntityManagerFactory emf = cefb.getObject();
    assertThat(cefb.getObject()).as("EntityManagerFactory reference must be cached after init").isSameAs(emf);

    assertThat(emf).as("EMF must be proxied").isNotSameAs(mockEmf);
    EntityManager em = emf.createEntityManager();
    assertThat(em.contains(testEntity)).isFalse();

    cefb.destroy();

    verify(mockEmf).close();
  }

  @Test
  public void testApplicationManagedEntityManagerWithTransaction() throws Exception {
    Object testEntity = new Object();

    EntityTransaction mockTx = mock(EntityTransaction.class);

    // This one's for the tx (shared)
    EntityManager sharedEm = mock(EntityManager.class);
    given(sharedEm.getTransaction()).willReturn(new NoOpEntityTransaction());

    // This is the application-specific one
    EntityManager mockEm = mock(EntityManager.class);
    given(mockEm.getTransaction()).willReturn(mockTx);

    given(mockEmf.createEntityManager()).willReturn(sharedEm, mockEm);

    LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();

    JpaTransactionManager jpatm = new JpaTransactionManager();
    jpatm.setEntityManagerFactory(cefb.getObject());

    TransactionStatus txStatus = jpatm.getTransaction(new DefaultTransactionAttribute());

    EntityManagerFactory emf = cefb.getObject();
    assertThat(cefb.getObject()).as("EntityManagerFactory reference must be cached after init").isSameAs(emf);

    assertThat(emf).as("EMF must be proxied").isNotSameAs(mockEmf);
    EntityManager em = emf.createEntityManager();
    em.joinTransaction();
    assertThat(em.contains(testEntity)).isFalse();

    jpatm.commit(txStatus);

    cefb.destroy();

    verify(mockTx).begin();
    verify(mockTx).commit();
    verify(mockEm).contains(testEntity);
    verify(mockEmf).close();
  }

  @Test
  public void testApplicationManagedEntityManagerWithTransactionAndCommitException() throws Exception {
    Object testEntity = new Object();

    EntityTransaction mockTx = mock(EntityTransaction.class);
    willThrow(new OptimisticLockException()).given(mockTx).commit();

    // This one's for the tx (shared)
    EntityManager sharedEm = mock(EntityManager.class);
    given(sharedEm.getTransaction()).willReturn(new NoOpEntityTransaction());

    // This is the application-specific one
    EntityManager mockEm = mock(EntityManager.class);
    given(mockEm.getTransaction()).willReturn(mockTx);

    given(mockEmf.createEntityManager()).willReturn(sharedEm, mockEm);

    LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();

    JpaTransactionManager jpatm = new JpaTransactionManager();
    jpatm.setEntityManagerFactory(cefb.getObject());

    TransactionStatus txStatus = jpatm.getTransaction(new DefaultTransactionAttribute());

    EntityManagerFactory emf = cefb.getObject();
    assertThat(cefb.getObject()).as("EntityManagerFactory reference must be cached after init").isSameAs(emf);

    assertThat(emf).as("EMF must be proxied").isNotSameAs(mockEmf);
    EntityManager em = emf.createEntityManager();
    em.joinTransaction();
    assertThat(em.contains(testEntity)).isFalse();

    assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() ->
            jpatm.commit(txStatus));

    cefb.destroy();

    verify(mockTx).begin();
    verify(mockEm).contains(testEntity);
    verify(mockEmf).close();
  }

  @Test
  public void testApplicationManagedEntityManagerWithJtaTransaction() throws Exception {
    Object testEntity = new Object();

    // This one's for the tx (shared)
    EntityManager sharedEm = mock(EntityManager.class);
    given(sharedEm.getTransaction()).willReturn(new NoOpEntityTransaction());

    // This is the application-specific one
    EntityManager mockEm = mock(EntityManager.class);

    given(mockEmf.createEntityManager()).willReturn(sharedEm, mockEm);

    LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
    MutablePersistenceUnitInfo pui = ((MutablePersistenceUnitInfo) cefb.getPersistenceUnitInfo());
    pui.setTransactionType(PersistenceUnitTransactionType.JTA);

    JpaTransactionManager jpatm = new JpaTransactionManager();
    jpatm.setEntityManagerFactory(cefb.getObject());

    TransactionStatus txStatus = jpatm.getTransaction(new DefaultTransactionAttribute());

    EntityManagerFactory emf = cefb.getObject();
    assertThat(cefb.getObject()).as("EntityManagerFactory reference must be cached after init").isSameAs(emf);

    assertThat(emf).as("EMF must be proxied").isNotSameAs(mockEmf);
    EntityManager em = emf.createEntityManager();
    em.joinTransaction();
    assertThat(em.contains(testEntity)).isFalse();

    jpatm.commit(txStatus);

    cefb.destroy();

    verify(mockEm).joinTransaction();
    verify(mockEm).contains(testEntity);
    verify(mockEmf).close();
  }

  public LocalContainerEntityManagerFactoryBean parseValidPersistenceUnit() throws Exception {
    LocalContainerEntityManagerFactoryBean emfb = createEntityManagerFactoryBean(
            "cn/taketoday/orm/jpa/domain/persistence.xml", null,
            "Person");
    return emfb;
  }

  @Test
  public void testInvalidPersistenceUnitName() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            createEntityManagerFactoryBean("cn/taketoday/orm/jpa/domain/persistence.xml", null, "call me Bob"));
  }

  protected LocalContainerEntityManagerFactoryBean createEntityManagerFactoryBean(
          String persistenceXml, Properties props, String entityManagerName) throws Exception {

    // This will be set by DummyPersistenceProvider
    actualPui = null;
    actualProps = null;

    LocalContainerEntityManagerFactoryBean containerEmfb = new LocalContainerEntityManagerFactoryBean();

    containerEmfb.setPersistenceUnitName(entityManagerName);
    containerEmfb.setPersistenceProviderClass(DummyContainerPersistenceProvider.class);
    if (props != null) {
      containerEmfb.setJpaProperties(props);
    }
    containerEmfb.setLoadTimeWeaver(new InstrumentationLoadTimeWeaver());
    containerEmfb.setPersistenceXmlLocation(persistenceXml);
    containerEmfb.afterPropertiesSet();

    assertThat(actualPui.getPersistenceUnitName()).isEqualTo(entityManagerName);
    if (props != null) {
      assertThat((Object) actualProps).isEqualTo(props);
    }
    //checkInvariants(containerEmfb);

    return containerEmfb;

    //containerEmfb.destroy();
    //emfMc.verify();
  }

  @Test
  public void testRejectsMissingPersistenceUnitInfo() throws Exception {
    LocalContainerEntityManagerFactoryBean containerEmfb = new LocalContainerEntityManagerFactoryBean();
    String entityManagerName = "call me Bob";

    containerEmfb.setPersistenceUnitName(entityManagerName);
    containerEmfb.setPersistenceProviderClass(DummyContainerPersistenceProvider.class);

    assertThatIllegalArgumentException().isThrownBy(
            containerEmfb::afterPropertiesSet);
  }

  private static class DummyContainerPersistenceProvider implements PersistenceProvider {

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo pui, Map map) {
      actualPui = pui;
      actualProps = map;
      return mockEmf;
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emfName, Map properties) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ProviderUtil getProviderUtil() {
      throw new UnsupportedOperationException();
    }

    // JPA 2.1 method
    @Override
    public void generateSchema(PersistenceUnitInfo persistenceUnitInfo, Map map) {
      throw new UnsupportedOperationException();
    }

    // JPA 2.1 method
    @Override
    public boolean generateSchema(String persistenceUnitName, Map map) {
      throw new UnsupportedOperationException();
    }
  }

  private static class NoOpEntityTransaction implements EntityTransaction {

    @Override
    public void begin() {
    }

    @Override
    public void commit() {
    }

    @Override
    public void rollback() {
    }

    @Override
    public void setRollbackOnly() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean getRollbackOnly() {
      return false;
    }

    @Override
    public boolean isActive() {
      return false;
    }
  }

}
