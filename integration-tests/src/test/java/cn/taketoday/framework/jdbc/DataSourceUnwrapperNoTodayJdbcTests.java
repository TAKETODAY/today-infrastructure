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

import javax.sql.DataSource;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.jdbc.config.DataSourceUnwrapper;
import cn.taketoday.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DataSourceUnwrapper} when spring-jdbc is not available.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions("today-jdbc-*.jar")
class DataSourceUnwrapperNoTodayJdbcTests {

  @Test
  void unwrapWithProxy() {
    DataSource dataSource = new HikariDataSource();
    DataSource actual = wrapInProxy(wrapInProxy(dataSource));
    assertThat(DataSourceUnwrapper.unwrap(actual, HikariConfigMXBean.class, HikariDataSource.class))
            .isSameAs(dataSource);
  }

  @Test
  void unwrapDataSourceProxy() {
    org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
    DataSource actual = wrapInProxy(wrapInProxy(dataSource));
    assertThat(DataSourceUnwrapper.unwrap(actual, PoolConfiguration.class, DataSourceProxy.class))
            .isSameAs(dataSource);
  }

  private DataSource wrapInProxy(DataSource dataSource) {
    return (DataSource) new ProxyFactory(dataSource).getProxy();
  }

}
