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

package cn.taketoday.orm.jpa.hibernate;

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import cn.taketoday.orm.jpa.domain.Person;
import cn.taketoday.orm.jpa.EntityManagerFactoryInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate-specific JPA tests with native SessionFactory setup and getCurrentSession interaction.
 *
 * @author Juergen Hoeller
 */
public class HibernateNativeEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private ApplicationContext applicationContext;

  @Override
  protected String[] getConfigLocations() {
    return new String[] { "/cn/taketoday/orm/jpa/hibernate/hibernate-manager-native.xml",
            "/cn/taketoday/orm/jpa/memdb.xml", "/cn/taketoday/orm/jpa/inject.xml" };
  }

  @Override
  @Test
  public void testEntityManagerFactoryImplementsEntityManagerFactoryInfo() {
    boolean condition = entityManagerFactory instanceof EntityManagerFactoryInfo;
    assertThat(condition).as("Must not have introduced config interface").isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEntityListener() {
    String firstName = "Tony";
    insertPerson(firstName);

    List<Person> people = sharedEntityManager.createQuery("select p from Person as p").getResultList();
    assertThat(people.size()).isEqualTo(1);
    assertThat(people.get(0).getFirstName()).isEqualTo(firstName);
    assertThat(people.get(0).postLoaded).isSameAs(applicationContext);
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testCurrentSession() {
    String firstName = "Tony";
    insertPerson(firstName);

    Query q = sessionFactory.getCurrentSession().createQuery("select p from Person as p");
    List<Person> people = q.getResultList();
    assertThat(people.size()).isEqualTo(1);
    assertThat(people.get(0).getFirstName()).isEqualTo(firstName);
    assertThat(people.get(0).postLoaded).isSameAs(applicationContext);
  }

  @Test  // SPR-16956
  public void testReadOnly() {
    assertThat(sessionFactory.getCurrentSession().getHibernateFlushMode()).isSameAs(FlushMode.AUTO);
    assertThat(sessionFactory.getCurrentSession().isDefaultReadOnly()).isFalse();
    endTransaction();

    this.transactionDefinition.setReadOnly(true);
    startNewTransaction();
    assertThat(sessionFactory.getCurrentSession().getHibernateFlushMode()).isSameAs(FlushMode.MANUAL);
    assertThat(sessionFactory.getCurrentSession().isDefaultReadOnly()).isTrue();
  }

}
