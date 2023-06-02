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

package cn.taketoday.framework.jdbc;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.function.Consumer;

import javax.sql.DataSource;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.jdbc.config.DataSourceUnwrapper;
import cn.taketoday.jdbc.datasource.DelegatingDataSource;
import cn.taketoday.jdbc.datasource.SingleConnectionDataSource;
import cn.taketoday.jdbc.datasource.SmartDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceUnwrapper}.
 *
 * @author Stephane Nicoll
 */
class DataSourceUnwrapperTests {

  @Test
  void unwrapWithTarget() {
    DataSource dataSource = new HikariDataSource();
    assertThat(DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class, HikariDataSource.class))
            .isSameAs(dataSource);
  }

  @Test
  void unwrapWithWrongTarget() {
    DataSource dataSource = new HikariDataSource();
    assertThat(DataSourceUnwrapper.unwrap(dataSource, SmartDataSource.class, SingleConnectionDataSource.class))
            .isNull();
  }

  @Test
  void unwrapWithDelegate() {
    DataSource dataSource = new HikariDataSource();
    DataSource actual = wrapInDelegate(wrapInDelegate(dataSource));
    assertThat(DataSourceUnwrapper.unwrap(actual, HikariConfigMXBean.class, HikariDataSource.class))
            .isSameAs(dataSource);
  }

  @Test
  void unwrapWithProxy() {
    DataSource dataSource = new HikariDataSource();
    DataSource actual = wrapInProxy(wrapInProxy(dataSource));
    assertThat(DataSourceUnwrapper.unwrap(actual, HikariConfigMXBean.class, HikariDataSource.class))
            .isSameAs(dataSource);
  }

  @Test
  void unwrapWithProxyAndDelegate() {
    DataSource dataSource = new HikariDataSource();
    DataSource actual = wrapInProxy(wrapInDelegate(dataSource));
    assertThat(DataSourceUnwrapper.unwrap(actual, HikariConfigMXBean.class, HikariDataSource.class))
            .isSameAs(dataSource);
  }

  @Test
  void unwrapWithSeveralLevelOfWrapping() {
    DataSource dataSource = new HikariDataSource();
    DataSource actual = wrapInProxy(wrapInDelegate(wrapInDelegate(wrapInProxy(wrapInDelegate(dataSource)))));
    assertThat(DataSourceUnwrapper.unwrap(actual, HikariConfigMXBean.class, HikariDataSource.class))
            .isSameAs(dataSource);
  }

  @Test
  void unwrapDataSourceProxy() {
    org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
    DataSource actual = wrapInDelegate(wrapInProxy(dataSource));
    assertThat(DataSourceUnwrapper.unwrap(actual, PoolConfiguration.class, DataSourceProxy.class))
            .isSameAs(dataSource);
  }

  @Test
  void unwrappingIsNotAttemptedWhenTargetIsNotAnInterface() {
    DataSource dataSource = mock(DataSource.class);
    assertThat(DataSourceUnwrapper.unwrap(dataSource, HikariDataSource.class)).isNull();
    then(dataSource).shouldHaveNoMoreInteractions();
  }

  @Test
  void unwrappingIsNotAttemptedWhenDataSourceIsNotWrapperForTarget() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    assertThat(DataSourceUnwrapper.unwrap(dataSource, Consumer.class)).isNull();
    then(dataSource).should().isWrapperFor(Consumer.class);
    then(dataSource).shouldHaveNoMoreInteractions();
  }

  private DataSource wrapInProxy(DataSource dataSource) {
    return (DataSource) new ProxyFactory(dataSource).getProxy();
  }

  private DataSource wrapInDelegate(DataSource dataSource) {
    return new DelegatingDataSource(dataSource);
  }

}
