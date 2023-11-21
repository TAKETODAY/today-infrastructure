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

import java.net.URL;
import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Provide the list of managed persistent types that an entity manager should
 * consider.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 10:23
 */
public interface PersistenceManagedTypes {

  /**
   * Return the class names the persistence provider must add to its set of
   * managed classes.
   *
   * @return the managed class names
   * @see PersistenceUnitInfo#getManagedClassNames()
   */
  List<String> getManagedClassNames();

  /**
   * Return a list of managed Java packages, to be introspected by the
   * persistence provider.
   *
   * @return the managed packages
   */
  List<String> getManagedPackages();

  /**
   * Return the persistence unit root url or {@code null} if it could not be
   * determined.
   *
   * @return the persistence unit root url
   * @see PersistenceUnitInfo#getPersistenceUnitRootUrl()
   */
  @Nullable
  URL getPersistenceUnitRootUrl();

  /**
   * Create an instance using the specified managed class names.
   *
   * @param managedClassNames the managed class names
   * @return a {@link PersistenceManagedTypes}
   */
  static PersistenceManagedTypes of(String... managedClassNames) {
    Assert.notNull(managedClassNames, "'managedClassNames' is required");
    return new SimplePersistenceManagedTypes(List.of(managedClassNames), List.of());
  }

  /**
   * Create an instance using the specified managed class names and packages.
   *
   * @param managedClassNames the managed class names
   * @param managedPackages the managed packages
   * @return a {@link PersistenceManagedTypes}
   */
  static PersistenceManagedTypes of(List<String> managedClassNames, List<String> managedPackages) {
    return new SimplePersistenceManagedTypes(managedClassNames, managedPackages);
  }

}
