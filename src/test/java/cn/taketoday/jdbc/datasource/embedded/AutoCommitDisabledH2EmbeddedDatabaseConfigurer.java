/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.datasource.embedded;

import java.sql.Driver;

import cn.taketoday.util.ClassUtils;

/**
 * {@link EmbeddedDatabaseConfigurer} for an H2 embedded database instance
 * with auto-commit disabled.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class AutoCommitDisabledH2EmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

  private final Class<? extends Driver> driverClass;

  public AutoCommitDisabledH2EmbeddedDatabaseConfigurer() throws Exception {
    this.driverClass = ClassUtils.forName("org.h2.Driver",
            AutoCommitDisabledH2EmbeddedDatabaseConfigurer.class.getClassLoader());
  }

  @Override
  public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
    String url = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;AUTOCOMMIT=false", databaseName);

    properties.setDriverClass(this.driverClass);
    properties.setUrl(url);
    properties.setUsername("sa");
    properties.setPassword("");
  }

}
