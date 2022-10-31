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

package cn.taketoday.annotation.config.jpa;

import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.jdbc.EmbeddedDatabaseConnection;
import cn.taketoday.framework.jdbc.SchemaManagement;
import cn.taketoday.framework.jdbc.SchemaManagementProvider;

/**
 * A {@link SchemaManagementProvider} that invokes a configurable number of
 * {@link SchemaManagementProvider} instances for embedded data sources only.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class HibernateDefaultDdlAutoProvider implements SchemaManagementProvider {

  private final Iterable<SchemaManagementProvider> providers;

  HibernateDefaultDdlAutoProvider(Iterable<SchemaManagementProvider> providers) {
    this.providers = providers;
  }

  String getDefaultDdlAuto(DataSource dataSource) {
    if (!EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
      return "none";
    }
    SchemaManagement schemaManagement = getSchemaManagement(dataSource);
    if (SchemaManagement.MANAGED.equals(schemaManagement)) {
      return "none";
    }
    return "create-drop";
  }

  @Override
  public SchemaManagement getSchemaManagement(DataSource dataSource) {
    return StreamSupport.stream(this.providers.spliterator(), false)
            .map((provider) -> provider.getSchemaManagement(dataSource)).filter(SchemaManagement.MANAGED::equals)
            .findFirst().orElse(SchemaManagement.UNMANAGED);
  }

}
