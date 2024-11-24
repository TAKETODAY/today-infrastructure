/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jdbc;

import java.util.List;

import javax.sql.DataSource;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.jdbc.config.DataSourceBuilder;
import infra.lang.Nullable;

/**
 * {@link RuntimeHintsRegistrar} implementation for {@link DataSource} types supported by
 * the {@link DataSourceBuilder}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DataSourceBuilderRuntimeHints implements RuntimeHintsRegistrar {

  private static final List<String> TYPE_NAMES = List.of(
          "com.mchange.v2.c3p0.ComboPooledDataSource",
          "org.h2.jdbcx.JdbcDataSource",
          "com.zaxxer.hikari.HikariDataSource",
          "org.apache.commons.dbcp2.BasicDataSource",
          "oracle.jdbc.datasource.OracleDataSource",
          "oracle.ucp.jdbc.PoolDataSource",
          "org.postgresql.ds.PGSimpleDataSource",
          "infra.jdbc.datasource.SimpleDriverDataSource",
          "org.apache.tomcat.jdbc.pool.DataSource"
  );

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    for (String typeName : TYPE_NAMES) {
      hints.reflection()
              .registerTypeIfPresent(classLoader, typeName,
                      (hint) -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
    }
  }

}
