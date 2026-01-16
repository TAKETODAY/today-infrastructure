/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.jdbc;

import org.jspecify.annotations.Nullable;

import java.util.List;

import javax.sql.DataSource;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.jdbc.config.DataSourceBuilder;

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
