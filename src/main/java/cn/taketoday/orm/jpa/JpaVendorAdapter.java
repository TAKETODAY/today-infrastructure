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

import java.util.Collections;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * SPI interface that allows to plug in vendor-specific behavior
 * into Framework's EntityManagerFactory creators. Serves as single
 * configuration point for all vendor-specific properties.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see AbstractEntityManagerFactoryBean#setJpaVendorAdapter
 * @since 4.0
 */
public interface JpaVendorAdapter {

  /**
   * Return the vendor-specific persistence provider.
   */
  PersistenceProvider getPersistenceProvider();

  /**
   * Return the name of the persistence provider's root package
   * (e.g. "oracle.toplink.essentials"). Will be used for
   * excluding provider classes from temporary class overriding.
   */
  @Nullable
  default String getPersistenceProviderRootPackage() {
    return null;
  }

  /**
   * Return a Map of vendor-specific JPA properties for the given persistence
   * unit, typically based on settings in this JpaVendorAdapter instance.
   * <p>Note that there might be further JPA properties defined on the
   * EntityManagerFactory bean, which might potentially override individual
   * JPA property values specified here.
   * <p>This implementation delegates to {@link #getJpaPropertyMap()} for
   * non-unit-dependent properties. Effectively, this PersistenceUnitInfo-based
   * variant only needs to be implemented if there is an actual need to react
   * to unit-specific characteristics such as the transaction type.
   * <p><b>NOTE:</b> This variant will only be invoked in case of Jakarta EE style
   * container bootstrapping where a {@link PersistenceUnitInfo} is present
   * (i.e. {@link LocalContainerEntityManagerFactoryBean}. In case of simple
   * Java SE style bootstrapping via {@link jakarta.persistence.Persistence}
   * (i.e. {@link LocalEntityManagerFactoryBean}), the parameter-less
   * {@link #getJpaPropertyMap()} variant will be called directly.
   *
   * @param pui the PersistenceUnitInfo for the current persistence unit
   * @return a Map of JPA properties, as accepted by the standard JPA bootstrap
   * facilities, or an empty Map if there are no properties to expose
   * @see PersistenceUnitInfo#getTransactionType()
   * @see PersistenceProvider#createContainerEntityManagerFactory(PersistenceUnitInfo, Map)
   */
  default Map<String, ?> getJpaPropertyMap(PersistenceUnitInfo pui) {
    return getJpaPropertyMap();
  }

  /**
   * Return a Map of vendor-specific JPA properties,
   * typically based on settings in this JpaVendorAdapter instance.
   * <p>Note that there might be further JPA properties defined on the
   * EntityManagerFactory bean, which might potentially override individual
   * JPA property values specified here.
   *
   * @return a Map of JPA properties, as accepted by the standard JPA bootstrap
   * facilities, or an empty Map if there are no properties to expose
   * @see jakarta.persistence.Persistence#createEntityManagerFactory(String, Map)
   */
  default Map<String, ?> getJpaPropertyMap() {
    return Collections.emptyMap();
  }

  /**
   * Return the vendor-specific JpaDialect implementation for this
   * provider, or {@code null} if there is none.
   */
  @Nullable
  default JpaDialect getJpaDialect() {
    return null;
  }

  /**
   * Return the vendor-specific EntityManagerFactory interface
   * that the EntityManagerFactory proxy is supposed to implement.
   * <p>If the provider does not offer any EntityManagerFactory extensions,
   * the adapter should simply return the standard
   * {@link EntityManagerFactory} class here.
   */
  default Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
    return EntityManagerFactory.class;
  }

  /**
   * Return the vendor-specific EntityManager interface
   * that this provider's EntityManagers will implement.
   * <p>If the provider does not offer any EntityManager extensions,
   * the adapter should simply return the standard
   * {@link EntityManager} class here.
   */
  default Class<? extends EntityManager> getEntityManagerInterface() {
    return EntityManager.class;
  }

  /**
   * Optional callback for post-processing the native EntityManagerFactory
   * before active use.
   * <p>This can be used for triggering vendor-specific initialization processes.
   * While this is not expected to be used for most providers, it is included
   * here as a general extension hook.
   */
  default void postProcessEntityManagerFactory(EntityManagerFactory emf) { }

  /**
   * Optional callback for post-processing the native EntityManager
   * before active use.
   * <p>This can be used for setting vendor-specific parameters, e.g.
   * Hibernate filters, on every new EntityManager.
   *
   * @since 4.0
   */
  default void postProcessEntityManager(EntityManager em) { }

}
