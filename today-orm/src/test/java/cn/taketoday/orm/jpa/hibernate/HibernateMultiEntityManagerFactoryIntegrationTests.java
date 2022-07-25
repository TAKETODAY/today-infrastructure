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

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import cn.taketoday.orm.jpa.EntityManagerFactoryInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Hibernate-specific JPA tests with multiple EntityManagerFactory instances.
 *
 * @author Juergen Hoeller
 */
public class HibernateMultiEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

  @Autowired
  private EntityManagerFactory entityManagerFactory2;

  @Override
  protected String[] getConfigLocations() {
    return new String[] { "/cn/taketoday/orm/jpa/hibernate/hibernate-manager-multi.xml",
            "/cn/taketoday/orm/jpa/memdb.xml" };
  }

  @Override
  @Test
  public void testEntityManagerFactoryImplementsEntityManagerFactoryInfo() {
    boolean condition = this.entityManagerFactory instanceof EntityManagerFactoryInfo;
    assertThat(condition).as("Must have introduced config interface").isTrue();
    EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) this.entityManagerFactory;
    assertThat(emfi.getPersistenceUnitName()).isEqualTo("Drivers");
    assertThat(emfi.getPersistenceUnitInfo()).as("PersistenceUnitInfo must be available").isNotNull();
    assertThat(emfi.getNativeEntityManagerFactory()).as("Raw EntityManagerFactory must be available").isNotNull();
  }

  @Test
  public void testEntityManagerFactory2() {
    EntityManager em = this.entityManagerFactory2.createEntityManager();
    try {
      assertThatIllegalArgumentException().isThrownBy(() ->
              em.createQuery("select tb from TestBean"));
    }
    finally {
      em.close();
    }
  }

}
