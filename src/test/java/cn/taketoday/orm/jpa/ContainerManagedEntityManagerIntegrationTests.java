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

import java.lang.reflect.Proxy;
import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.orm.jpa.domain.Person;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TransactionRequiredException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Integration tests using in-memory database for container-managed JPA
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ContainerManagedEntityManagerIntegrationTests extends AbstractEntityManagerFactoryIntegrationTests {

  @Autowired
  private AbstractEntityManagerFactoryBean entityManagerFactoryBean;

  @Test
  public void testExceptionTranslationWithDialectFoundOnIntroducedEntityManagerInfo() throws Exception {
    doTestExceptionTranslationWithDialectFound(((EntityManagerFactoryInfo) entityManagerFactory).getJpaDialect());
  }

  @Test
  public void testExceptionTranslationWithDialectFoundOnEntityManagerFactoryBean() throws Exception {
    assertThat(entityManagerFactoryBean.getJpaDialect()).as("Dialect must have been set").isNotNull();
    doTestExceptionTranslationWithDialectFound(entityManagerFactoryBean);
  }

  protected void doTestExceptionTranslationWithDialectFound(PersistenceExceptionTranslator pet) throws Exception {
    RuntimeException in1 = new RuntimeException("in1");
    PersistenceException in2 = new PersistenceException();
    assertThat(pet.translateExceptionIfPossible(in1)).as("No translation here").isNull();
    DataAccessException dex = pet.translateExceptionIfPossible(in2);
    assertThat(dex).isNotNull();
    assertThat(dex.getCause()).isSameAs(in2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEntityManagerProxyIsProxy() {
    EntityManager em = createContainerManagedEntityManager();
    assertThat(Proxy.isProxyClass(em.getClass())).isTrue();
    Query q = em.createQuery("select p from Person as p");
    List<Person> people = q.getResultList();
    assertThat(people.isEmpty()).isTrue();

    assertThat(em.isOpen()).as("Should be open to start with").isTrue();
    assertThatIllegalStateException().as("Close should not work on container managed EM").isThrownBy(
            em::close);
    assertThat(em.isOpen()).isTrue();
  }

  // This would be legal, at least if not actually _starting_ a tx
  @Test
  public void testEntityManagerProxyRejectsProgrammaticTxManagement() {
    assertThatIllegalStateException().isThrownBy(
            createContainerManagedEntityManager()::getTransaction);
  }

  /*
   * See comments in spec on EntityManager.joinTransaction().
   * We take the view that this is a valid no op.
   */
  @Test
  public void testContainerEntityManagerProxyAllowsJoinTransactionInTransaction() {
    createContainerManagedEntityManager().joinTransaction();
  }

  @Test
  public void testContainerEntityManagerProxyRejectsJoinTransactionWithoutTransaction() {
    endTransaction();
    assertThatExceptionOfType(TransactionRequiredException.class).isThrownBy(
            createContainerManagedEntityManager()::joinTransaction);
  }

  @Test
  public void testInstantiateAndSave() {
    EntityManager em = createContainerManagedEntityManager();
    doInstantiateAndSave(em);
  }

  protected void doInstantiateAndSave(EntityManager em) {
    assertThat(countRowsInTable(em, "person")).as("Should be no people from previous transactions").isEqualTo(0);
    Person p = new Person();

    p.setFirstName("Tony");
    p.setLastName("Blair");
    em.persist(p);

    em.flush();
    assertThat(countRowsInTable(em, "person")).as("1 row must have been inserted").isEqualTo(1);
  }

  @Test
  public void testReuseInNewTransaction() {
    EntityManager em = createContainerManagedEntityManager();
    doInstantiateAndSave(em);
    endTransaction();

    //assertFalse(em.getTransaction().isActive());

    startNewTransaction();
    // Call any method: should cause automatic tx invocation
    assertThat(em.contains(new Person())).isFalse();
    //assertTrue(em.getTransaction().isActive());

    doInstantiateAndSave(em);
    setComplete();
    endTransaction();  // Should rollback
    assertThat(countRowsInTable(em, "person")).as("Tx must have committed back").isEqualTo(1);

    // Now clean up the database
    deleteFromTables("person");
  }

  @Test
  public void testRollbackOccurs() {
    EntityManager em = createContainerManagedEntityManager();
    doInstantiateAndSave(em);
    endTransaction();  // Should rollback
    assertThat(countRowsInTable(em, "person")).as("Tx must have been rolled back").isEqualTo(0);
  }

  @Test
  public void testCommitOccurs() {
    EntityManager em = createContainerManagedEntityManager();
    doInstantiateAndSave(em);
    setComplete();
    endTransaction();  // Should rollback
    assertThat(countRowsInTable(em, "person")).as("Tx must have committed back").isEqualTo(1);

    // Now clean up the database
    deleteFromTables("person");
  }

}
