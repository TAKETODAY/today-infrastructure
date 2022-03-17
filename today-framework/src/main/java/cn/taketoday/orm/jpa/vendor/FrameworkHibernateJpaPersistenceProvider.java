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

package cn.taketoday.orm.jpa.vendor;

import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Framework-specific subclass of the standard {@link HibernatePersistenceProvider}
 * from the {@code org.hibernate.jpa} package, adding support for
 * {@link SmartPersistenceUnitInfo#getManagedPackages()}.
 *
 * @author Juergen Hoeller
 * @author Joris Kuipers
 * @see Configuration#addPackage
 * @since 4.0
 */
class FrameworkHibernateJpaPersistenceProvider extends HibernatePersistenceProvider {

  @Override
  public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
    ArrayList<String> mergedClassesAndPackages = new ArrayList<>(info.getManagedClassNames());
    if (info instanceof SmartPersistenceUnitInfo unitInfo) {
      mergedClassesAndPackages.addAll(unitInfo.getManagedPackages());
    }
    return new EntityManagerFactoryBuilderImpl(
            new PersistenceUnitInfoDescriptor(info) {
              @Override
              public List<String> getManagedClassNames() {
                return mergedClassesAndPackages;
              }
            }, properties).build();
  }

}
