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

package cn.taketoday.framework.jdbc.metadata;

import java.sql.SQLException;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OracleUcpDataSourcePoolMetadata}.
 *
 * @author Fabio Grassi
 */
class OracleUcpDataSourcePoolMetadataTests
        extends AbstractDataSourcePoolMetadataTests<OracleUcpDataSourcePoolMetadata> {

  private final OracleUcpDataSourcePoolMetadata dataSourceMetadata = new OracleUcpDataSourcePoolMetadata(
          createDataSource(0, 2));

  @Override
  protected OracleUcpDataSourcePoolMetadata getDataSourceMetadata() {
    return this.dataSourceMetadata;
  }

  @Override
  void getValidationQuery() throws SQLException {
    PoolDataSource dataSource = createDataSource(0, 4);
    dataSource.setSQLForValidateConnection("SELECT NULL FROM DUAL");
    assertThat(new OracleUcpDataSourcePoolMetadata(dataSource).getValidationQuery())
            .isEqualTo("SELECT NULL FROM DUAL");
  }

  @Override
  void getDefaultAutoCommit() throws SQLException {
    PoolDataSource dataSource = createDataSource(0, 4);
    dataSource.setConnectionProperty("autoCommit", "false");
    assertThat(new OracleUcpDataSourcePoolMetadata(dataSource).getDefaultAutoCommit()).isFalse();
  }

  private PoolDataSource createDataSource(int minSize, int maxSize) {
    try {
      PoolDataSource dataSource = initializeBuilder().type(PoolDataSourceImpl.class).build();
      dataSource.setInitialPoolSize(minSize);
      dataSource.setMinPoolSize(minSize);
      dataSource.setMaxPoolSize(maxSize);
      return dataSource;
    }
    catch (SQLException ex) {
      throw new IllegalStateException("Error while configuring PoolDataSource", ex);
    }
  }

}
