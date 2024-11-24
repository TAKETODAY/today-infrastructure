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

import infra.core.annotation.AliasFor;
import infra.test.annotation.DirtiesContext;
import infra.test.context.jdbc.Sql.ExecutionPhase;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Integration tests that verify support for using {@link Sql @Sql} as a
 * merged, composed annotation.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(EmptyDatabaseConfig.class)
@DirtiesContext
class ComposedAnnotationSqlScriptsTests extends AbstractTransactionalTests {

  @Test
  @ComposedSql(
          scripts = { "drop-schema.sql", "schema.sql" },
          statements = "INSERT INTO user VALUES('Dilbert')",
          executionPhase = BEFORE_TEST_METHOD
  )
  void composedSqlAnnotation() {
    assertNumUsers(1);
  }

  @Sql
  @Retention(RUNTIME)
  @interface ComposedSql {

    @AliasFor(annotation = Sql.class)
    String[] value() default {};

    @AliasFor(annotation = Sql.class)
    String[] scripts() default {};

    @AliasFor(annotation = Sql.class)
    String[] statements() default {};

    @AliasFor(annotation = Sql.class)
    ExecutionPhase executionPhase();
  }

}
