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

import cn.taketoday.orm.jpa.domain.Person;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TransactionRequiredException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * An application-managed entity manager can join an existing transaction,
 * but such joining must be made programmatically, not transactionally.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ApplicationManagedEntityManagerIntegrationTests extends AbstractEntityManagerFactoryIntegrationTests {

  @Test
  @SuppressWarnings("unchecked")
  public void testEntityManagerProxyIsProxy() {
    EntityManager em = entityManagerFactory.createEntityManager();
    assertThat(Proxy.isProxyClass(em.getClass())).isTrue();
    Query q = em.createQuery("select p from Person as p");
    List<Person> people = q.getResultList();
    assertThat(people).isNotNull();

    assertThat(em.isOpen()).as("Should be open to start with").isTrue();
    em.close();
    assertThat(em.isOpen()).as("Close should work on application managed EM").isFalse();
  }

  @Test
  public void testEntityManagerProxyAcceptsProgrammaticTxJoining() {
    EntityManager em = entityManagerFactory.createEntityManager();
    em.joinTransaction();
  }

  @Test
  public void testInstantiateAndSave() {
    EntityManager em = entityManagerFactory.createEntityManager();
    em.joinTransaction();
    doInstantiateAndSave(em);
  }

  @Test
  public void testCannotFlushWithoutGettingTransaction() {
    EntityManager em = entityManagerFactory.createEntityManager();
    assertThatExceptionOfType(TransactionRequiredException.class).isThrownBy(() ->
            doInstantiateAndSave(em));

    // TODO following lines are a workaround for Hibernate bug
    // If Hibernate throws an exception due to flush(),
    // it actually HAS flushed, meaning that the database
    // was updated outside the transaction
    deleteAllPeopleUsingEntityManager(sharedEntityManager);
    setComplete();
  }

  protected void doInstantiateAndSave(EntityManager em) {
    testStateClean();
    Person p = new Person();

    p.setFirstName("Tony");
    p.setLastName("Blair");
    em.persist(p);

    em.flush();
    assertThat(countRowsInTable(em, "person")).as("1 row must have been inserted").isEqualTo(1);
  }

  @Test
  public void testStateClean() {
    assertThat(countRowsInTable("person")).as("Should be no people from previous transactions").isEqualTo(0);
  }

  @Test
  public void testReuseInNewTransaction() {
    EntityManager em = entityManagerFactory.createEntityManager();
    em.joinTransaction();

    doInstantiateAndSave(em);
    endTransaction();

    assertThat(em.getTransaction().isActive()).isFalse();

    startNewTransaction();
    // Call any method: should cause automatic tx invocation
    assertThat(em.contains(new Person())).isFalse();

    assertThat(em.getTransaction().isActive()).isFalse();
    em.joinTransaction();

    assertThat(em.getTransaction().isActive()).isTrue();

    doInstantiateAndSave(em);
    setComplete();
    endTransaction();  // Should rollback
    assertThat(countRowsInTable(em, "person")).as("Tx must have committed back").isEqualTo(1);

    // Now clean up the database
    startNewTransaction();
    em.joinTransaction();
    deleteAllPeopleUsingEntityManager(em);
    assertThat(countRowsInTable(em, "person")).as("People have been killed").isEqualTo(0);
    setComplete();
  }

  protected void deleteAllPeopleUsingEntityManager(EntityManager em) {
    em.createQuery("delete from Person p").executeUpdate();
  }

  @Test
  public void testRollbackOccurs() {
    EntityManager em = entityManagerFactory.createEntityManager();
    em.joinTransaction();
    doInstantiateAndSave(em);
    endTransaction();  // Should rollback
    assertThat(countRowsInTable(em, "person")).as("Tx must have been rolled back").isEqualTo(0);
  }

  @Test
  public void testCommitOccurs() {
    EntityManager em = entityManagerFactory.createEntityManager();
    em.joinTransaction();
    doInstantiateAndSave(em);

    setComplete();
    endTransaction();  // Should rollback
    assertThat(countRowsInTable(em, "person")).as("Tx must have committed back").isEqualTo(1);

    // Now clean up the database
    deleteFromTables("person");
  }

}
