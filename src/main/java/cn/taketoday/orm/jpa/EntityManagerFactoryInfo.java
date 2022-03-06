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

import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.lang.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Metadata interface for a Framework-managed JPA {@link EntityManagerFactory}.
 *
 * <p>This facility can be obtained from Framework-managed EntityManagerFactory
 * proxies through casting the EntityManagerFactory handle to this interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface EntityManagerFactoryInfo {

  /**
   * Return the underlying PersistenceProvider that the underlying
   * EntityManagerFactory was created with.
   *
   * @return the PersistenceProvider used to create this EntityManagerFactory,
   * or {@code null} if the standard JPA provider autodetection process
   * was used to configure the EntityManagerFactory
   */
  @Nullable
  PersistenceProvider getPersistenceProvider();

  /**
   * Return the PersistenceUnitInfo used to create this
   * EntityManagerFactory, if the in-container API was used.
   *
   * @return the PersistenceUnitInfo used to create this EntityManagerFactory,
   * or {@code null} if the in-container contract was not used to
   * configure the EntityManagerFactory
   */
  @Nullable
  PersistenceUnitInfo getPersistenceUnitInfo();

  /**
   * Return the name of the persistence unit used to create this
   * EntityManagerFactory, or {@code null} if it is an unnamed default.
   * <p>If {@code getPersistenceUnitInfo()} returns non-null, the result of
   * {@code getPersistenceUnitName()} must be equal to the value returned by
   * {@code PersistenceUnitInfo.getPersistenceUnitName()}.
   *
   * @see #getPersistenceUnitInfo()
   * @see PersistenceUnitInfo#getPersistenceUnitName()
   */
  @Nullable
  String getPersistenceUnitName();

  /**
   * Return the JDBC DataSource that this EntityManagerFactory
   * obtains its JDBC Connections from.
   *
   * @return the JDBC DataSource, or {@code null} if not known
   */
  @Nullable
  DataSource getDataSource();

  /**
   * Return the (potentially vendor-specific) EntityManager interface
   * that this factory's EntityManagers will implement.
   * <p>A {@code null} return value suggests that autodetection is supposed
   * to happen: either based on a target {@code EntityManager} instance
   * or simply defaulting to {@code jakarta.persistence.EntityManager}.
   */
  @Nullable
  Class<? extends EntityManager> getEntityManagerInterface();

  /**
   * Return the vendor-specific JpaDialect implementation for this
   * EntityManagerFactory, or {@code null} if not known.
   */
  @Nullable
  JpaDialect getJpaDialect();

  /**
   * Return the ClassLoader that the application's beans are loaded with.
   * <p>Proxies will be generated in this ClassLoader.
   */
  ClassLoader getBeanClassLoader();

  /**
   * Return the raw underlying EntityManagerFactory.
   *
   * @return the unadorned EntityManagerFactory (never {@code null})
   */
  EntityManagerFactory getNativeEntityManagerFactory();

  /**
   * Create a native JPA EntityManager to be used as the framework-managed
   * resource behind an application-level EntityManager handle.
   * <p>This exposes a native {@code EntityManager} from the underlying
   * {@link #getNativeEntityManagerFactory() native EntityManagerFactory},
   * taking {@link JpaVendorAdapter#postProcessEntityManager(EntityManager)}
   * into account.
   *
   * @see #getNativeEntityManagerFactory()
   * @see EntityManagerFactory#createEntityManager()
   */
  EntityManager createNativeEntityManager(@Nullable Map<?, ?> properties);

}
