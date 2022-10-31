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

import com.zaxxer.hikari.HikariDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HikariDataSourcePoolMetadata}.
 *
 * @author Stephane Nicoll
 */
public class HikariDataSourcePoolMetadataTests
        extends AbstractDataSourcePoolMetadataTests<HikariDataSourcePoolMetadata> {

  private final HikariDataSourcePoolMetadata dataSourceMetadata = new HikariDataSourcePoolMetadata(
          createDataSource(0, 2));

  @Override
  protected HikariDataSourcePoolMetadata getDataSourceMetadata() {
    return this.dataSourceMetadata;
  }

  @Override
  public void getValidationQuery() {
    HikariDataSource dataSource = createDataSource(0, 4);
    dataSource.setConnectionTestQuery("SELECT FROM FOO");
    assertThat(new HikariDataSourcePoolMetadata(dataSource).getValidationQuery()).isEqualTo("SELECT FROM FOO");
  }

  @Override
  public void getDefaultAutoCommit() {
    HikariDataSource dataSource = createDataSource(0, 4);
    dataSource.setAutoCommit(false);
    assertThat(new HikariDataSourcePoolMetadata(dataSource).getDefaultAutoCommit()).isFalse();
  }

  private HikariDataSource createDataSource(int minSize, int maxSize) {
    HikariDataSource dataSource = initializeBuilder().type(HikariDataSource.class).build();
    dataSource.setMinimumIdle(minSize);
    dataSource.setMaximumPoolSize(maxSize);
    dataSource.setIdleTimeout(5000);
    return dataSource;
  }

}
