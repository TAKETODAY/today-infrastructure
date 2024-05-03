/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.orm.jpa.vendor;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.NativeDetector;
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Configuration#addPackage
 * @since 4.0
 */
class HibernateJpaPersistenceProvider extends HibernatePersistenceProvider {

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })  // on Hibernate 6
  public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
    final List<String> mergedClassesAndPackages = new ArrayList<>(info.getManagedClassNames());
    if (info instanceof SmartPersistenceUnitInfo smartInfo) {
      mergedClassesAndPackages.addAll(smartInfo.getManagedPackages());
    }
    return new EntityManagerFactoryBuilderImpl(
            new PersistenceUnitInfoDescriptor(info) {
              @Override
              public List<String> getManagedClassNames() {
                return mergedClassesAndPackages;
              }

              @Override
              public void pushClassTransformer(EnhancementContext enhancementContext) {
                if (!NativeDetector.inNativeImage()) {
                  super.pushClassTransformer(enhancementContext);
                }
              }
            }, properties).build();
  }

}
