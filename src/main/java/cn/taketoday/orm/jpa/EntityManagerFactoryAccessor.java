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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Base class for any class that needs to access a JPA {@link EntityManagerFactory},
 * usually in order to obtain a JPA {@link EntityManager}. Defines common properties.
 *
 * @author Juergen Hoeller
 * @see EntityManagerFactoryUtils
 * @since 4.0
 */
public abstract class EntityManagerFactoryAccessor implements BeanFactoryAware {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private EntityManagerFactory entityManagerFactory;

  @Nullable
  private String persistenceUnitName;

  private final Map<String, Object> jpaPropertyMap = new HashMap<>();

  /**
   * Set the JPA EntityManagerFactory that should be used to create
   * EntityManagers.
   *
   * @see EntityManagerFactory#createEntityManager()
   * @see EntityManagerFactory#createEntityManager(Map)
   */
  public void setEntityManagerFactory(@Nullable EntityManagerFactory emf) {
    this.entityManagerFactory = emf;
  }

  /**
   * Return the JPA EntityManagerFactory that should be used to create
   * EntityManagers.
   */
  @Nullable
  public EntityManagerFactory getEntityManagerFactory() {
    return this.entityManagerFactory;
  }

  /**
   * Obtain the EntityManagerFactory for actual use.
   *
   * @return the EntityManagerFactory (never {@code null})
   * @throws IllegalStateException in case of no EntityManagerFactory set
   */
  protected final EntityManagerFactory obtainEntityManagerFactory() {
    EntityManagerFactory emf = getEntityManagerFactory();
    Assert.state(emf != null, "No EntityManagerFactory set");
    return emf;
  }

  /**
   * Set the name of the persistence unit to access the EntityManagerFactory for.
   * <p>This is an alternative to specifying the EntityManagerFactory by direct reference,
   * resolving it by its persistence unit name instead. If no EntityManagerFactory and
   * no persistence unit name have been specified, a default EntityManagerFactory will
   * be retrieved through finding a single unique bean of type EntityManagerFactory.
   *
   * @see #setEntityManagerFactory
   */
  public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
    this.persistenceUnitName = persistenceUnitName;
  }

  /**
   * Return the name of the persistence unit to access the EntityManagerFactory for, if any.
   */
  @Nullable
  public String getPersistenceUnitName() {
    return this.persistenceUnitName;
  }

  /**
   * Specify JPA properties, to be passed into
   * {@code EntityManagerFactory.createEntityManager(Map)} (if any).
   * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
   * or a "props" element in XML bean definitions.
   *
   * @see EntityManagerFactory#createEntityManager(Map)
   */
  public void setJpaProperties(Properties jpaProperties) {
    CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
  }

  /**
   * Specify JPA properties as a Map, to be passed into
   * {@code EntityManagerFactory.createEntityManager(Map)} (if any).
   * <p>Can be populated with a "map" or "props" element in XML bean definitions.
   *
   * @see EntityManagerFactory#createEntityManager(Map)
   */
  public void setJpaPropertyMap(@Nullable Map<String, Object> jpaProperties) {
    if (jpaProperties != null) {
      this.jpaPropertyMap.putAll(jpaProperties);
    }
  }

  /**
   * Allow Map access to the JPA properties to be passed to the persistence
   * provider, with the option to add or override specific entries.
   * <p>Useful for specifying entries directly, for example via "jpaPropertyMap[myKey]".
   */
  public Map<String, Object> getJpaPropertyMap() {
    return this.jpaPropertyMap;
  }

  /**
   * Retrieves an EntityManagerFactory by persistence unit name, if none set explicitly.
   * Falls back to a default EntityManagerFactory bean if no persistence unit specified.
   *
   * @see #setPersistenceUnitName
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    if (getEntityManagerFactory() == null) {
      setEntityManagerFactory(EntityManagerFactoryUtils.findEntityManagerFactory(beanFactory, getPersistenceUnitName()));
    }
  }

  /**
   * Obtain a new EntityManager from this accessor's EntityManagerFactory.
   * <p>Can be overridden in subclasses to create specific EntityManager variants.
   *
   * @return a new EntityManager
   * @throws IllegalStateException if this accessor is not configured with an EntityManagerFactory
   * @see EntityManagerFactory#createEntityManager()
   * @see EntityManagerFactory#createEntityManager(Map)
   */
  protected EntityManager createEntityManager() throws IllegalStateException {
    EntityManagerFactory emf = obtainEntityManagerFactory();
    Map<String, Object> properties = getJpaPropertyMap();
    return (CollectionUtils.isNotEmpty(properties) ? emf.createEntityManager(properties) : emf.createEntityManager());
  }

  /**
   * Obtain the transactional EntityManager for this accessor's EntityManagerFactory, if any.
   *
   * @return the transactional EntityManager, or {@code null} if none
   * @throws IllegalStateException if this accessor is not configured with an EntityManagerFactory
   * @see EntityManagerFactoryUtils#getTransactionalEntityManager(EntityManagerFactory)
   * @see EntityManagerFactoryUtils#getTransactionalEntityManager(EntityManagerFactory, Map)
   */
  @Nullable
  protected EntityManager getTransactionalEntityManager() throws IllegalStateException {
    EntityManagerFactory emf = obtainEntityManagerFactory();
    return EntityManagerFactoryUtils.getTransactionalEntityManager(emf, getJpaPropertyMap());
  }

}
