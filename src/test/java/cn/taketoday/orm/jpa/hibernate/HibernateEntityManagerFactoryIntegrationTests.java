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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import cn.taketoday.orm.jpa.EntityManagerFactoryInfo;
import cn.taketoday.orm.jpa.EntityManagerProxy;
import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate-specific JPA tests.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 */
@SuppressWarnings("deprecation")
public class HibernateEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

  @Override
  protected String[] getConfigLocations() {
    return new String[] { "/cn/taketoday/orm/jpa/hibernate/hibernate-manager.xml",
            "/cn/taketoday/orm/jpa/memdb.xml", "/cn/taketoday/orm/jpa/inject.xml" };
  }

  @Test
  public void testCanCastNativeEntityManagerFactoryToHibernateEntityManagerFactoryImpl() {
    EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
    boolean condition1 = emfi.getNativeEntityManagerFactory() instanceof org.hibernate.jpa.HibernateEntityManagerFactory;
    assertThat(condition1).isTrue();
    // as of Hibernate 5.2
    boolean condition = emfi.getNativeEntityManagerFactory() instanceof SessionFactory;
    assertThat(condition).isTrue();
  }

  @Test
  public void testCanCastSharedEntityManagerProxyToHibernateEntityManager() {
    boolean condition1 = sharedEntityManager instanceof org.hibernate.jpa.HibernateEntityManager;
    assertThat(condition1).isTrue();
    // as of Hibernate 5.2
    boolean condition = ((EntityManagerProxy) sharedEntityManager).getTargetEntityManager() instanceof Session;
    assertThat(condition).isTrue();
  }

  @Test
  public void testCanUnwrapAopProxy() {
    EntityManager em = entityManagerFactory.createEntityManager();
    EntityManager proxy = ProxyFactory.getProxy(EntityManager.class, new SingletonTargetSource(em));
    boolean condition = em instanceof org.hibernate.jpa.HibernateEntityManager;
    assertThat(condition).isTrue();
    boolean condition1 = proxy instanceof org.hibernate.jpa.HibernateEntityManager;
    assertThat(condition1).isFalse();
    assertThat(proxy.unwrap(org.hibernate.jpa.HibernateEntityManager.class) != null).isTrue();
    assertThat(proxy.unwrap(org.hibernate.jpa.HibernateEntityManager.class)).isSameAs(em);
    assertThat(proxy.getDelegate()).isSameAs(em.getDelegate());
  }

  @Test  // SPR-16956
  public void testReadOnly() {
    assertThat(sharedEntityManager.unwrap(Session.class).getHibernateFlushMode()).isSameAs(FlushMode.AUTO);
    assertThat(sharedEntityManager.unwrap(Session.class).isDefaultReadOnly()).isFalse();
    endTransaction();

    this.transactionDefinition.setReadOnly(true);
    startNewTransaction();
    assertThat(sharedEntityManager.unwrap(Session.class).getHibernateFlushMode()).isSameAs(FlushMode.MANUAL);
    assertThat(sharedEntityManager.unwrap(Session.class).isDefaultReadOnly()).isTrue();
  }

}
