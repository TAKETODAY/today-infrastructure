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

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProvider;

/**
 * {@link cn.taketoday.beans.factory.FactoryBean} that creates a JPA
 * {@link EntityManagerFactory} according to JPA's standard
 * <i>standalone</i> bootstrap contract. This is the simplest way to set up a
 * shared JPA EntityManagerFactory in a Framework application context; the
 * EntityManagerFactory can then be passed to JPA-based DAOs via
 * dependency injection. Note that switching to a JNDI lookup or to a
 * {@link LocalContainerEntityManagerFactoryBean}
 * definition is just a matter of configuration!
 *
 * <p>Configuration settings are usually read from a {@code META-INF/persistence.xml}
 * config file, residing in the class path, according to the JPA standalone bootstrap
 * contract. Additionally, most JPA providers will require a special VM agent
 * (specified on JVM startup) that allows them to instrument application classes.
 * See the Java Persistence API specification and your provider documentation
 * for setup details.
 *
 * <p>This EntityManagerFactory bootstrap is appropriate for standalone applications
 * which solely use JPA for data access. If you want to set up your persistence
 * provider for an external DataSource and/or for global transactions which span
 * multiple resources, you will need to either deploy it into a full Jakarta EE
 * application server and access the deployed EntityManagerFactory via JNDI,
 * or use Framework's {@link LocalContainerEntityManagerFactoryBean} with appropriate
 * configuration for local setup according to JPA's container contract.
 *
 * <p><b>Note:</b> This FactoryBean has limited configuration power in terms of
 * what configuration it is able to pass to the JPA provider. If you need more
 * flexible configuration, for example passing a Framework-managed JDBC DataSource
 * to the JPA provider, consider using Framework's more powerful
 * {@link LocalContainerEntityManagerFactoryBean} instead.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see #setJpaProperties
 * @see #setJpaVendorAdapter
 * @see JpaTransactionManager#setEntityManagerFactory
 * @see LocalContainerEntityManagerFactoryBean
 * @see cn.taketoday.jndi.JndiObjectFactoryBean
 * @see cn.taketoday.orm.jpa.support.SharedEntityManagerBean
 * @see Persistence#createEntityManagerFactory
 * @see PersistenceProvider#createEntityManagerFactory
 * @since 4.0
 */
@SuppressWarnings("serial")
public class LocalEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

  /**
   * Initialize the EntityManagerFactory for the given configuration.
   *
   * @throws PersistenceException in case of JPA initialization errors
   */
  @Override
  protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
    if (logger.isDebugEnabled()) {
      logger.debug("Building JPA EntityManagerFactory for persistence unit '{}'", getPersistenceUnitName());
    }
    PersistenceProvider provider = getPersistenceProvider();
    if (provider != null) {
      // Create EntityManagerFactory directly through PersistenceProvider.
      EntityManagerFactory emf = provider.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap());
      if (emf == null) {
        throw new IllegalStateException(
                "PersistenceProvider [" + provider + "] did not return an EntityManagerFactory for name '" +
                        getPersistenceUnitName() + "'");
      }
      return emf;
    }
    else {
      // Let JPA perform its standard PersistenceProvider autodetection.
      return Persistence.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap());
    }
  }

}
