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

import javax.sql.DataSource;

import cn.taketoday.jdbc.datasource.SimpleDriverDataSource;

/**
 * Creates a {@link SimpleDriverDataSource}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.0
 */
final class SimpleDriverDataSourceFactory implements DataSourceFactory {

  private final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

  @Override
  public ConnectionProperties getConnectionProperties() {
    return new ConnectionProperties() {
      @Override
      public void setDriverClass(Class<? extends Driver> driverClass) {
        dataSource.setDriverClass(driverClass);
      }

      @Override
      public void setUrl(String url) {
        dataSource.setUrl(url);
      }

      @Override
      public void setUsername(String username) {
        dataSource.setUsername(username);
      }

      @Override
      public void setPassword(String password) {
        dataSource.setPassword(password);
      }
    };
  }

  @Override
  public DataSource getDataSource() {
    return this.dataSource;
  }

}
