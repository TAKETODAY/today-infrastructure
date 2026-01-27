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

package infra.sql.init;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import infra.dao.DataAccessException;
import infra.test.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Base class for testing {@link AbstractScriptDatabaseInitializer} implementations.
 *
 * @param <T> type of the initializer being tested
 * @author Andy Wilkinson
 */
public abstract class AbstractScriptDatabaseInitializerTests<T extends AbstractScriptDatabaseInitializer> {

  @Test
  @WithSchemaSqlResource
  @WithDataSqlResource
  void whenDatabaseIsInitializedThenSchemaAndDataScriptsAreApplied() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(List.of("schema.sql"));
    settings.setDataLocations(List.of("data.sql"));
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThat(initializer.initializeDatabase()).isTrue();
    assertThat(numberOfEmbeddedRows("SELECT COUNT(*) FROM EXAMPLE")).isOne();
  }

  @Test
  void whenDatabaseIsInitializedWithDirectoryLocationsThenFailureIsHelpful() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(List.of("/infra/sql/init"));
    settings.setDataLocations(List.of("/infra/sql/init"));
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThatIllegalStateException().isThrownBy(initializer::initializeDatabase)
            .withMessage("No schema scripts found at location '/infra/sql/init'");
  }

  @Test
  @WithDataSqlResource
  void whenContinueOnErrorIsFalseThenInitializationFailsOnError() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setDataLocations(List.of("data.sql"));
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThatExceptionOfType(DataAccessException.class).isThrownBy(initializer::initializeDatabase);
    assertThatDatabaseWasAccessed(initializer);
  }

  @Test
  @WithDataSqlResource
  void whenContinueOnErrorIsTrueThenInitializationDoesNotFailOnError() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setContinueOnError(true);
    settings.setDataLocations(List.of("data.sql"));
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThat(initializer.initializeDatabase()).isTrue();
    assertThatDatabaseWasAccessed(initializer);
  }

  @Test
  void whenNoScriptsExistAtASchemaLocationThenInitializationFails() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(List.of("does-not-exist.sql"));
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThatIllegalStateException().isThrownBy(initializer::initializeDatabase)
            .withMessage("No schema scripts found at location 'does-not-exist.sql'");
    assertThatDatabaseWasNotAccessed(initializer);
  }

  @Test
  void whenNoScriptsExistAtADataLocationThenInitializationFails() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setDataLocations(List.of("does-not-exist.sql"));
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThatIllegalStateException().isThrownBy(initializer::initializeDatabase)
            .withMessage("No data scripts found at location 'does-not-exist.sql'");
    assertThatDatabaseWasNotAccessed(initializer);
  }

  @Test
  void whenNoScriptsExistAtAnOptionalSchemaLocationThenDatabaseIsNotAccessed() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(List.of("optional:does-not-exist.sql"));
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThat(initializer.initializeDatabase()).isFalse();
    assertThatDatabaseWasNotAccessed(initializer);
  }

  @Test
  void whenNoScriptsExistAtAnOptionalDataLocationThenDatabaseIsNotAccessed() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setDataLocations(List.of("optional:does-not-exist.sql"));
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThat(initializer.initializeDatabase()).isFalse();
    assertThatDatabaseWasNotAccessed(initializer);
  }

  @Test
  @WithSchemaSqlResource
  @WithDataSqlResource
  void whenModeIsNeverThenEmbeddedDatabaseIsNotInitialized() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(List.of("schema.sql"));
    settings.setDataLocations(List.of("data.sql"));
    settings.setMode(DatabaseInitializationMode.NEVER);
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThat(initializer.initializeDatabase()).isFalse();
    assertThatDatabaseWasNotAccessed(initializer);
  }

  @Test
  @WithSchemaSqlResource
  @WithDataSqlResource
  void whenModeIsNeverThenStandaloneDatabaseIsNotInitialized() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(List.of("schema.sql"));
    settings.setDataLocations(List.of("data.sql"));
    settings.setMode(DatabaseInitializationMode.NEVER);
    T initializer = createStandaloneDatabaseInitializer(settings);
    assertThat(initializer.initializeDatabase()).isFalse();
    assertThatDatabaseWasNotAccessed(initializer);
  }

  @Test
  @WithSchemaSqlResource
  @WithDataSqlResource
  void whenModeIsEmbeddedThenEmbeddedDatabaseIsInitialized() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(List.of("schema.sql"));
    settings.setDataLocations(List.of("data.sql"));
    settings.setMode(DatabaseInitializationMode.EMBEDDED);
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThat(initializer.initializeDatabase()).isTrue();
    assertThat(numberOfEmbeddedRows("SELECT COUNT(*) FROM EXAMPLE")).isOne();
  }

  @Test
  @WithSchemaSqlResource
  @WithDataSqlResource
  void whenModeIsEmbeddedThenStandaloneDatabaseIsNotInitialized() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(List.of("schema.sql"));
    settings.setDataLocations(List.of("data.sql"));
    settings.setMode(DatabaseInitializationMode.EMBEDDED);
    T initializer = createStandaloneDatabaseInitializer(settings);
    assertThat(initializer.initializeDatabase()).isFalse();
    assertThatDatabaseWasAccessed(initializer);
  }

  @Test
  @WithSchemaSqlResource
  @WithDataSqlResource
  void whenModeIsAlwaysThenEmbeddedDatabaseIsInitialized() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(List.of("schema.sql"));
    settings.setDataLocations(List.of("data.sql"));
    settings.setMode(DatabaseInitializationMode.ALWAYS);
    T initializer = createEmbeddedDatabaseInitializer(settings);
    assertThat(initializer.initializeDatabase()).isTrue();
    assertThat(numberOfEmbeddedRows("SELECT COUNT(*) FROM EXAMPLE")).isOne();
  }

  @Test
  @WithSchemaSqlResource
  @WithDataSqlResource
  void whenModeIsAlwaysThenStandaloneDatabaseIsInitialized() {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(List.of("schema.sql"));
    settings.setDataLocations(List.of("data.sql"));
    settings.setMode(DatabaseInitializationMode.ALWAYS);
    T initializer = createStandaloneDatabaseInitializer(settings);
    assertThat(initializer.initializeDatabase()).isTrue();
    assertThat(numberOfStandaloneRows("SELECT COUNT(*) FROM EXAMPLE")).isOne();
  }

  protected abstract T createStandaloneDatabaseInitializer(DatabaseInitializationSettings settings);

  protected abstract T createEmbeddedDatabaseInitializer(DatabaseInitializationSettings settings);

  protected abstract int numberOfEmbeddedRows(String sql);

  protected abstract int numberOfStandaloneRows(String sql);

  private void assertThatDatabaseWasAccessed(T initializer) {
    assertDatabaseAccessed(true, initializer);
  }

  private void assertThatDatabaseWasNotAccessed(T initializer) {
    assertDatabaseAccessed(false, initializer);
  }

  protected abstract void assertDatabaseAccessed(boolean accessed, T initializer);

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @WithResource(name = "schema.sql", content = """
          CREATE TABLE EXAMPLE (
          	id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
          	name VARCHAR(30)
          );
          """)
  protected @interface WithSchemaSqlResource {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @WithResource(name = "data.sql", content = "INSERT INTO EXAMPLE VALUES (1, 'Andy');")
  protected @interface WithDataSqlResource {

  }

}
