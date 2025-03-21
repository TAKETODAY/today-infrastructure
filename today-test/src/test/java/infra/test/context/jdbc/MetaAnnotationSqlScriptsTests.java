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

package infra.test.context.jdbc;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import infra.test.annotation.DirtiesContext;
import infra.test.context.ContextConfiguration;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Integration tests that verify support for using {@link Sql @Sql} and
 * {@link SqlGroup @SqlGroup} as meta-annotations.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@DirtiesContext
class MetaAnnotationSqlScriptsTests extends AbstractTransactionalTests {

  @Test
  @MetaSql
  void metaSqlAnnotation() {
    assertNumUsers(1);
  }

  @Test
  @MetaSqlGroup
  void metaSqlGroupAnnotation() {
    assertNumUsers(1);
  }

  @Sql({ "drop-schema.sql", "schema.sql", "data.sql" })
  @Retention(RUNTIME)
  @Target(METHOD)
  @interface MetaSql {
  }

  @SqlGroup({ @Sql("drop-schema.sql"), @Sql("schema.sql"), @Sql("data.sql") })
  @Retention(RUNTIME)
  @Target(METHOD)
  @interface MetaSqlGroup {
  }

}
