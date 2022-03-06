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

import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Interface that defines an abstraction for finding and managing
 * JPA PersistenceUnitInfos. Used by
 * {@link cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * in order to obtain a {@link PersistenceUnitInfo}
 * for building a concrete {@link jakarta.persistence.EntityManagerFactory}.
 *
 * <p>Obtaining a PersistenceUnitInfo instance is an exclusive process.
 * A PersistenceUnitInfo instance is not available for further calls
 * anymore once it has been obtained.
 *
 * @author Juergen Hoeller
 * @see DefaultPersistenceUnitManager
 * @see cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean#setPersistenceUnitManager
 * @since 4.0
 */
public interface PersistenceUnitManager {

  /**
   * Obtain the default PersistenceUnitInfo from this manager.
   *
   * @return the PersistenceUnitInfo (never {@code null})
   * @throws IllegalStateException if there is no default PersistenceUnitInfo defined
   * or it has already been obtained
   */
  PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() throws IllegalStateException;

  /**
   * Obtain the specified PersistenceUnitInfo from this manager.
   *
   * @param persistenceUnitName the name of the desired persistence unit
   * @return the PersistenceUnitInfo (never {@code null})
   * @throws IllegalArgumentException if no PersistenceUnitInfo with the given
   * name is defined
   * @throws IllegalStateException if the PersistenceUnitInfo with the given
   * name has already been obtained
   */
  PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName)
          throws IllegalArgumentException, IllegalStateException;

}
