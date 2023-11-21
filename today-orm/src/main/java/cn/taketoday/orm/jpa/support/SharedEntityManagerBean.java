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

package cn.taketoday.orm.jpa.support;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.jpa.EntityManagerFactoryAccessor;
import cn.taketoday.orm.jpa.EntityManagerFactoryInfo;
import cn.taketoday.orm.jpa.SharedEntityManagerCreator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * {@link FactoryBean} that exposes a shared JPA {@link EntityManager}
 * reference for a given EntityManagerFactory. Typically used for an EntityManagerFactory
 * created by {@link cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean},
 * as direct alternative to a JNDI lookup for a Jakarta EE server's EntityManager reference.
 *
 * <p>The shared EntityManager will behave just like an EntityManager fetched from an
 * application server's JNDI environment, as defined by the JPA specification.
 * It will delegate all calls to the current transactional EntityManager, if any;
 * otherwise, it will fall back to a newly created EntityManager per operation.
 *
 * <p>Can be passed to DAOs that expect a shared EntityManager reference rather than an
 * EntityManagerFactory. Note that Framework's {@link cn.taketoday.orm.jpa.JpaTransactionManager}
 * always needs an EntityManagerFactory in order to create new transactional EntityManager instances.
 *
 * @author Juergen Hoeller
 * @see #setEntityManagerFactory
 * @see #setEntityManagerInterface
 * @see cn.taketoday.orm.jpa.LocalEntityManagerFactoryBean
 * @see cn.taketoday.orm.jpa.JpaTransactionManager
 * @since 4.0
 */
public class SharedEntityManagerBean extends EntityManagerFactoryAccessor
        implements FactoryBean<EntityManager>, InitializingBean {

  @Nullable
  private Class<? extends EntityManager> entityManagerInterface;

  private boolean synchronizedWithTransaction = true;

  @Nullable
  private EntityManager shared;

  /**
   * Specify the EntityManager interface to expose.
   * <p>Default is the EntityManager interface as defined by the
   * EntityManagerFactoryInfo, if available. Else, the standard
   * {@code jakarta.persistence.EntityManager} interface will be used.
   *
   * @see cn.taketoday.orm.jpa.EntityManagerFactoryInfo#getEntityManagerInterface()
   * @see EntityManager
   */
  public void setEntityManagerInterface(Class<? extends EntityManager> entityManagerInterface) {
    Assert.notNull(entityManagerInterface, "'entityManagerInterface' is required");
    this.entityManagerInterface = entityManagerInterface;
  }

  /**
   * Set whether to automatically join ongoing transactions (according
   * to the JPA 2.1 SynchronizationType rules). Default is "true".
   */
  public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
    this.synchronizedWithTransaction = synchronizedWithTransaction;
  }

  @Override
  public final void afterPropertiesSet() {
    EntityManagerFactory emf = getEntityManagerFactory();
    if (emf == null) {
      throw new IllegalArgumentException("'entityManagerFactory' or 'persistenceUnitName' is required");
    }
    if (emf instanceof EntityManagerFactoryInfo emfInfo) {
      if (entityManagerInterface == null) {
        entityManagerInterface = emfInfo.getEntityManagerInterface();
        if (entityManagerInterface == null) {
          entityManagerInterface = EntityManager.class;
        }
      }
    }
    else {
      if (entityManagerInterface == null) {
        this.entityManagerInterface = EntityManager.class;
      }
    }
    shared = SharedEntityManagerCreator.createSharedEntityManager(
            emf, getJpaPropertyMap(), synchronizedWithTransaction, entityManagerInterface);
  }

  @Override
  @Nullable
  public EntityManager getObject() {
    return shared;
  }

  @Override
  public Class<? extends EntityManager> getObjectType() {
    return entityManagerInterface != null ? entityManagerInterface : EntityManager.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
