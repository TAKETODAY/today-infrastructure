/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetDatabase;
import org.eclipse.persistence.jpa.JpaEntityManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import cn.taketoday.lang.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.spi.PersistenceProvider;

/**
 * {@link cn.taketoday.orm.jpa.JpaVendorAdapter} implementation for Eclipse
 * Persistence Services (EclipseLink). Compatible with EclipseLink 3.0/4.0.
 *
 * <p>Exposes EclipseLink's persistence provider and EntityManager extension interface,
 * and adapts {@link AbstractJpaVendorAdapter}'s common configuration settings.
 * No support for the detection of annotated packages (through
 * {@link cn.taketoday.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()})
 * since EclipseLink doesn't use package-level metadata.
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EclipseLinkJpaDialect
 * @see org.eclipse.persistence.jpa.PersistenceProvider
 * @see org.eclipse.persistence.jpa.JpaEntityManager
 * @since 4.0
 */
public class EclipseLinkJpaVendorAdapter extends AbstractJpaVendorAdapter {

  private final PersistenceProvider persistenceProvider = new org.eclipse.persistence.jpa.PersistenceProvider();

  private final EclipseLinkJpaDialect jpaDialect = new EclipseLinkJpaDialect();

  @Override
  public PersistenceProvider getPersistenceProvider() {
    return this.persistenceProvider;
  }

  @Override
  public Map<String, Object> getJpaPropertyMap() {
    Map<String, Object> jpaProperties = new HashMap<>();

    if (getDatabasePlatform() != null) {
      jpaProperties.put(PersistenceUnitProperties.TARGET_DATABASE, getDatabasePlatform());
    }
    else {
      String targetDatabase = determineTargetDatabaseName(getDatabase());
      if (targetDatabase != null) {
        jpaProperties.put(PersistenceUnitProperties.TARGET_DATABASE, targetDatabase);
      }
    }

    if (isGenerateDdl()) {
      jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION,
              PersistenceUnitProperties.CREATE_ONLY);
      jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION_MODE,
              PersistenceUnitProperties.DDL_DATABASE_GENERATION);
    }
    if (isShowSql()) {
      jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +
              org.eclipse.persistence.logging.SessionLog.SQL, Level.FINE.toString());
      jpaProperties.put(PersistenceUnitProperties.LOGGING_PARAMETERS, Boolean.TRUE.toString());
    }

    return jpaProperties;
  }

  /**
   * Determine the EclipseLink target database name for the given database.
   *
   * @param database the specified database
   * @return the EclipseLink target database name, or {@code null} if none found
   */
  @Nullable
  protected String determineTargetDatabaseName(Database database) {
    return switch (database) {
      case DB2 -> TargetDatabase.DB2;
      case DERBY -> TargetDatabase.Derby;
      case HANA -> TargetDatabase.HANA;
      case HSQL -> TargetDatabase.HSQL;
      case INFORMIX -> TargetDatabase.Informix;
      case MYSQL -> TargetDatabase.MySQL;
      case ORACLE -> TargetDatabase.Oracle;
      case POSTGRESQL -> TargetDatabase.PostgreSQL;
      case SQL_SERVER -> TargetDatabase.SQLServer;
      case SYBASE -> TargetDatabase.Sybase;
      default -> null;
    };
  }

  @Override
  public EclipseLinkJpaDialect getJpaDialect() {
    return this.jpaDialect;
  }

  @Override
  public Class<? extends EntityManager> getEntityManagerInterface() {
    return JpaEntityManager.class;
  }

}
