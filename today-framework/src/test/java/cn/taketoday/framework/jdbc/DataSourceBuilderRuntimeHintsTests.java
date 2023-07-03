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

package cn.taketoday.framework.jdbc;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeHint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 22:38
 */
class DataSourceBuilderRuntimeHintsTests {

  @Test
  void shouldRegisterDataSourceConstructors() {
    ReflectionHints hints = registerHints();
    Stream
            .of(com.mchange.v2.c3p0.ComboPooledDataSource.class, org.h2.jdbcx.JdbcDataSource.class,
                    com.zaxxer.hikari.HikariDataSource.class, org.apache.commons.dbcp2.BasicDataSource.class,
                    oracle.jdbc.datasource.OracleDataSource.class, oracle.ucp.jdbc.PoolDataSource.class,
                    org.postgresql.ds.PGSimpleDataSource.class,
                    cn.taketoday.jdbc.datasource.SimpleDriverDataSource.class,
                    org.apache.tomcat.jdbc.pool.DataSource.class)
            .forEach((dataSourceType) -> {
              TypeHint typeHint = hints.getTypeHint(dataSourceType);
              assertThat(typeHint).withFailMessage(() -> "No hints found for data source type " + dataSourceType)
                      .isNotNull();
              Set<MemberCategory> memberCategories = typeHint.getMemberCategories();
              assertThat(memberCategories).containsExactly(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
            });
  }

  private ReflectionHints registerHints() {
    RuntimeHints hints = new RuntimeHints();
    new DataSourceBuilderRuntimeHints().registerHints(hints, getClass().getClassLoader());
    return hints.reflection();
  }

}