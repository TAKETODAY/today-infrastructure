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

import cn.taketoday.lang.Nullable;

/**
 * A simple {@link PersistenceManagedTypes} implementation that holds the list
 * of managed entities.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 10:24
 */
class SimplePersistenceManagedTypes implements PersistenceManagedTypes {

  private final List<String> managedClassNames;

  private final List<String> managedPackages;

  @Nullable
  private final URL persistenceUnitRootUrl;

  SimplePersistenceManagedTypes(List<String> managedClassNames,
          List<String> managedPackages, @Nullable URL persistenceUnitRootUrl) {
    this.managedClassNames = managedClassNames;
    this.managedPackages = managedPackages;
    this.persistenceUnitRootUrl = persistenceUnitRootUrl;
  }

  SimplePersistenceManagedTypes(List<String> managedClassNames, List<String> managedPackages) {
    this(managedClassNames, managedPackages, null);
  }

  @Override
  public List<String> getManagedClassNames() {
    return this.managedClassNames;
  }

  @Override
  public List<String> getManagedPackages() {
    return this.managedPackages;
  }

  @Override
  @Nullable
  public URL getPersistenceUnitRootUrl() {
    return this.persistenceUnitRootUrl;
  }

}
