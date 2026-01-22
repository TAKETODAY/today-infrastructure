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

package infra.jdbc.config;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Stream;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeHint;
import infra.jdbc.datasource.SimpleDriverDataSource;

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
                    SimpleDriverDataSource.class)
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