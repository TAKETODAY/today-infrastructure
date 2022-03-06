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

package cn.taketoday.orm.jpa.persistenceunit;

import java.util.List;

import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Extension of the standard JPA PersistenceUnitInfo interface, for advanced collaboration
 * between Framework's {@link cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * and {@link PersistenceUnitManager} implementations.
 *
 * @author Juergen Hoeller
 * @see PersistenceUnitManager
 * @see cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean
 * @since 4.0
 */
public interface SmartPersistenceUnitInfo extends PersistenceUnitInfo {

  /**
   * Return a list of managed Java packages, to be introspected by the persistence provider.
   * Typically found through scanning but not exposable through {@link #getManagedClassNames()}.
   *
   * @return a list of names of managed Java packages (potentially empty)
   * @since 4.0
   */
  List<String> getManagedPackages();

  /**
   * Set the persistence provider's own package name, for exclusion from class transformation.
   *
   * @see #addTransformer(jakarta.persistence.spi.ClassTransformer)
   * @see #getNewTempClassLoader()
   */
  void setPersistenceProviderPackageName(String persistenceProviderPackageName);

}
