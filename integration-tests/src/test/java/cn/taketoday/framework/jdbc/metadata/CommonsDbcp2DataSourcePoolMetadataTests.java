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

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommonsDbcp2DataSourcePoolMetadata}.
 *
 * @author Stephane Nicoll
 */
class CommonsDbcp2DataSourcePoolMetadataTests
        extends AbstractDataSourcePoolMetadataTests<CommonsDbcp2DataSourcePoolMetadata> {

  private final CommonsDbcp2DataSourcePoolMetadata dataSourceMetadata = createDataSourceMetadata(0, 2);

  @Override
  protected CommonsDbcp2DataSourcePoolMetadata getDataSourceMetadata() {
    return this.dataSourceMetadata;
  }

  @Test
  void getPoolUsageWithNoCurrent() {
    CommonsDbcp2DataSourcePoolMetadata dsm = new CommonsDbcp2DataSourcePoolMetadata(createDataSource()) {
      @Override
      public Integer getActive() {
        return null;
      }
    };
    assertThat(dsm.getUsage()).isNull();
  }

  @Test
  void getPoolUsageWithNoMax() {
    CommonsDbcp2DataSourcePoolMetadata dsm = new CommonsDbcp2DataSourcePoolMetadata(createDataSource()) {
      @Override
      public Integer getMax() {
        return null;
      }
    };
    assertThat(dsm.getUsage()).isNull();
  }

  @Test
  void getPoolUsageWithUnlimitedPool() {
    DataSourcePoolMetadata unlimitedDataSource = createDataSourceMetadata(0, -1);
    assertThat(unlimitedDataSource.getUsage()).isEqualTo(-1f);
  }

  @Override
  public void getValidationQuery() {
    BasicDataSource dataSource = createDataSource();
    dataSource.setValidationQuery("SELECT FROM FOO");
    assertThat(new CommonsDbcp2DataSourcePoolMetadata(dataSource).getValidationQuery())
            .isEqualTo("SELECT FROM FOO");
  }

  @Override
  public void getDefaultAutoCommit() {
    BasicDataSource dataSource = createDataSource();
    dataSource.setDefaultAutoCommit(false);
    assertThat(new CommonsDbcp2DataSourcePoolMetadata(dataSource).getDefaultAutoCommit()).isFalse();
  }

  private CommonsDbcp2DataSourcePoolMetadata createDataSourceMetadata(int minSize, int maxSize) {
    BasicDataSource dataSource = createDataSource();
    dataSource.setMinIdle(minSize);
    dataSource.setMaxTotal(maxSize);
    dataSource.setMinEvictableIdleTimeMillis(5000);
    return new CommonsDbcp2DataSourcePoolMetadata(dataSource);
  }

  private BasicDataSource createDataSource() {
    return initializeBuilder().type(BasicDataSource.class).build();
  }

}
