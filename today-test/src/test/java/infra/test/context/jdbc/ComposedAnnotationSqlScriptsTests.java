/*
 * Copyright 2002-present the original author or authors.
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
