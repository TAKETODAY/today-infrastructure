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

import org.apache.tomcat.jdbc.pool.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatDataSourcePoolMetadata}.
 *
 * @author Stephane Nicoll
 */
public class TomcatDataSourcePoolMetadataTests
        extends AbstractDataSourcePoolMetadataTests<TomcatDataSourcePoolMetadata> {

  private final TomcatDataSourcePoolMetadata dataSourceMetadata = new TomcatDataSourcePoolMetadata(
          createDataSource(0, 2));

  @Override
  protected TomcatDataSourcePoolMetadata getDataSourceMetadata() {
    return this.dataSourceMetadata;
  }

  @Override
  public void getValidationQuery() {
    DataSource dataSource = createDataSource(0, 4);
    dataSource.setValidationQuery("SELECT FROM FOO");
    assertThat(new TomcatDataSourcePoolMetadata(dataSource).getValidationQuery()).isEqualTo("SELECT FROM FOO");
  }

  @Override
  public void getDefaultAutoCommit() {
    DataSource dataSource = createDataSource(0, 4);
    dataSource.setDefaultAutoCommit(false);
    assertThat(new TomcatDataSourcePoolMetadata(dataSource).getDefaultAutoCommit()).isFalse();
  }

  private DataSource createDataSource(int minSize, int maxSize) {
    DataSource dataSource = initializeBuilder().type(DataSource.class).build();
    dataSource.setMinIdle(minSize);
    dataSource.setMaxActive(maxSize);
    dataSource.setMinEvictableIdleTimeMillis(5000);

    // Avoid warnings
    dataSource.setInitialSize(minSize);
    dataSource.setMaxIdle(maxSize);
    return dataSource;
  }

}
