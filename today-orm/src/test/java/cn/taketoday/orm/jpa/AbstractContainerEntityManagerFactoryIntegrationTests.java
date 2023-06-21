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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import cn.taketoday.core.testfixture.io.SerializationTestUtils;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.orm.jpa.domain.DriversLicense;
import cn.taketoday.orm.jpa.domain.Person;
import cn.taketoday.transaction.TransactionDefinition;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * Integration tests for LocalContainerEntityManagerFactoryBean.
 * Uses an in-memory database.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractContainerEntityManagerFactoryIntegrationTests
        extends AbstractEntityManagerFactoryIntegrationTests {

  @Test
  public void testEntityManagerFactoryImplementsEntityManagerFactoryInfo() {
    boolean condition = entityManagerFactory instanceof EntityManagerFactoryInfo;
    assertThat(condition).as("Must have introduced config interface").isTrue();
    EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
    assertThat(emfi.getPersistenceUnitName()).isEqualTo("Person");
    assertThat(emfi.getPersistenceUnitInfo()).as("PersistenceUnitInfo must be available").isNotNull();
    assertThat(emfi.getNativeEntityManagerFactory()).as("Raw EntityManagerFactory must be available").isNotNull();
  }

  @Test
  public void testStateClean() {
    assertThat(countRowsInTable("person")).as("Should be no people from previous transactions").isEqualTo(0);
  }

  @Test
  public void testJdbcTx1_1() {
    testJdbcTx2();
  }

  @Test
  public void testJdbcTx1_2() {
    testJdbcTx2();
  }

  @Test
  public void testJdbcTx1_3() {
    testJdbcTx2();
  }

  @Test
  public void testJdbcTx2() {
    assertThat(countRowsInTable("person")).as("Any previous tx must have been rolled back").isEqualTo(0);
    executeSqlScript("/cn/taketoday/orm/jpa/insertPerson.sql");
  }

  @Test
  public void testEntityManagerProxyIsProxy() {
    assertThat(Proxy.isProxyClass(sharedEntityManager.getClass())).isTrue();
    Query q = sharedEntityManager.createQuery("select p from Person as p");
    q.getResultList();

    assertThat(sharedEntityManager.isOpen()).as("Should be open to start with").isTrue();
    sharedEntityManager.close();
    assertThat(sharedEntityManager.isOpen()).as("Close should have been silently ignored").isTrue();
  }

  @Test
  public void testBogusQuery() {
    assertThatRuntimeException().isThrownBy(() -> {
      Query query = sharedEntityManager.createQuery("It's raining toads");
      // required in OpenJPA case
      query.executeUpdate();
    });
  }

  @Test
  public void testGetReferenceWhenNoRow() {
    assertThatException().isThrownBy(() -> {
              Person notThere = sharedEntityManager.getReference(Person.class, 666);
              // We may get here (as with Hibernate). Either behaviour is valid:
              // throw exception on first access or on getReference itself.
              notThere.getFirstName();
            })
            .matches(ex -> ex.getClass().getName().endsWith("NotFoundException"));
  }

  @Test
  public void testLazyLoading() throws Exception {
    try {
      Person tony = new Person();
      tony.setFirstName("Tony");
      tony.setLastName("Blair");
      tony.setDriversLicense(new DriversLicense("8439DK"));
      sharedEntityManager.persist(tony);
      assertThat(DataSourceUtils.getConnection(jdbcTemplate.getDataSource()).getTransactionIsolation())
              .isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
      setComplete();
      endTransaction();

      transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
      startNewTransaction();
      assertThat(DataSourceUtils.getConnection(jdbcTemplate.getDataSource()).getTransactionIsolation())
              .isEqualTo(TransactionDefinition.ISOLATION_SERIALIZABLE);
      sharedEntityManager.clear();
      Person newTony = entityManagerFactory.createEntityManager().getReference(Person.class, tony.getId());
      assertThat(tony).isNotSameAs(newTony);
      endTransaction();

      transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_DEFAULT);
      startNewTransaction();
      assertThat(DataSourceUtils.getConnection(jdbcTemplate.getDataSource()).getTransactionIsolation())
              .isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
      endTransaction();

      assertThat(newTony.getDriversLicense()).isNotNull();
      newTony.getDriversLicense().getSerialNumber();
    }
    finally {
      deleteFromTables("person", "drivers_license");
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMultipleResults() {
    // Add with JDBC
    String firstName = "Tony";
    insertPerson(firstName);

    assertThat(Proxy.isProxyClass(sharedEntityManager.getClass())).isTrue();
    Query q = sharedEntityManager.createQuery("select p from Person as p");
    List<Person> people = q.getResultList();

    assertThat(people).hasSize(1);
    assertThat(people.get(0).getFirstName()).isEqualTo(firstName);
  }

  protected void insertPerson(String firstName) {
    String INSERT_PERSON = "INSERT INTO PERSON (ID, FIRST_NAME, LAST_NAME) VALUES (?, ?, ?)";
    jdbcTemplate.update(INSERT_PERSON, 1, firstName, "Blair");
  }

  @Test
  public void testEntityManagerProxyRejectsProgrammaticTxManagement() {
    assertThatIllegalStateException().as("Should not be able to create transactions on container managed EntityManager").isThrownBy(
            sharedEntityManager::getTransaction);
  }

  @Test
  public void testInstantiateAndSaveWithSharedEmProxy() {
    testInstantiateAndSave(sharedEntityManager);
  }

  protected void testInstantiateAndSave(EntityManager em) {
    assertThat(countRowsInTable("person")).as("Should be no people from previous transactions").isEqualTo(0);
    Person p = new Person();
    p.setFirstName("Tony");
    p.setLastName("Blair");
    em.persist(p);

    em.flush();
    assertThat(countRowsInTable("person")).as("1 row must have been inserted").isEqualTo(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testQueryNoPersons() {
    EntityManager em = entityManagerFactory.createEntityManager();
    Query q = em.createQuery("select p from Person as p");
    List<Person> people = q.getResultList();
    assertThat(people).isEmpty();
    assertThatExceptionOfType(NoResultException.class).isThrownBy(q::getSingleResult);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testQueryNoPersonsNotTransactional() {
    endTransaction();

    EntityManager em = entityManagerFactory.createEntityManager();
    Query q = em.createQuery("select p from Person as p");
    List<Person> people = q.getResultList();
    assertThat(people).isEmpty();
    assertThatExceptionOfType(NoResultException.class).isThrownBy(q::getSingleResult);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testQueryNoPersonsShared() {
    Query q = this.sharedEntityManager.createQuery("select p from Person as p");
    q.setFlushMode(FlushModeType.AUTO);
    List<Person> people = q.getResultList();
    assertThat(people).isEmpty();
    assertThatExceptionOfType(NoResultException.class).isThrownBy(q::getSingleResult);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testQueryNoPersonsSharedNotTransactional() {
    endTransaction();

    EntityManager em = this.sharedEntityManager;
    Query q = em.createQuery("select p from Person as p");
    q.setFlushMode(FlushModeType.AUTO);
    List<Person> people = q.getResultList();
    assertThat(people).isEmpty();
    assertThatException()
            .isThrownBy(q::getSingleResult)
            .withMessageContaining("closed");
    // We would typically expect an IllegalStateException, but Hibernate throws a
    // PersistenceException. So we assert the contents of the exception message instead.

    Query q2 = em.createQuery("select p from Person as p");
    q2.setFlushMode(FlushModeType.AUTO);
    assertThatExceptionOfType(NoResultException.class).isThrownBy(q2::getSingleResult);
  }

  @Test
  public void testCanSerializeProxies() throws Exception {
    assertThat(SerializationTestUtils.serializeAndDeserialize(entityManagerFactory)).isNotNull();
    assertThat(SerializationTestUtils.serializeAndDeserialize(sharedEntityManager)).isNotNull();
  }

}
