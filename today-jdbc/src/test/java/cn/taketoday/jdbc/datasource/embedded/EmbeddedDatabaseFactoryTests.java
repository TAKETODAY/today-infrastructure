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

package cn.taketoday.jdbc.datasource.embedded;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import cn.taketoday.jdbc.datasource.init.DatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 */
public class EmbeddedDatabaseFactoryTests {

  private final EmbeddedDatabaseFactory factory = new EmbeddedDatabaseFactory();

  @Test
  void testGetDataSource() {
    StubDatabasePopulator populator = new StubDatabasePopulator();
    factory.setDatabasePopulator(populator);
    EmbeddedDatabase db = factory.getDatabase();
    assertThat(populator.populateCalled).isTrue();
    db.shutdown();
  }

  @Test
  void customizeConfigurerWithAnotherDatabaseName() throws SQLException {
    this.factory.setDatabaseName("original-db-mame");
    this.factory.setDatabaseConfigurer(EmbeddedDatabaseConfigurer.customizeConfigurer(
            EmbeddedDatabaseType.H2, defaultConfigurer ->
                    new EmbeddedDatabaseConfigurerDelegate(defaultConfigurer) {
                      @Override
                      public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
                        super.configureConnectionProperties(properties, "custom-db-name");
                      }
                    }));
    EmbeddedDatabase db = this.factory.getDatabase();
    try (Connection connection = db.getConnection()) {
      assertThat(connection.getMetaData().getURL()).contains("custom-db-name")
              .doesNotContain("original-db-mame");
    }
    db.shutdown();
  }

  @Test
  void customizeConfigurerWithCustomizedUrl() throws SQLException {
    this.factory.setDatabaseName("original-db-mame");
    this.factory.setDatabaseConfigurer(EmbeddedDatabaseConfigurer.customizeConfigurer(
            EmbeddedDatabaseType.H2, defaultConfigurer ->
                    new EmbeddedDatabaseConfigurerDelegate(defaultConfigurer) {
                      @Override
                      public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
                        super.configureConnectionProperties(properties, databaseName);
                        properties.setUrl("jdbc:h2:mem:custom-db-name;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=MariaDB");
                      }
                    }));
    EmbeddedDatabase db = this.factory.getDatabase();
    try (Connection connection = db.getConnection()) {
      assertThat(connection.getMetaData().getURL()).contains("custom-db-name")
              .doesNotContain("original-db-mame");
    }
    db.shutdown();
  }

  private static class StubDatabasePopulator implements DatabasePopulator {

    private boolean populateCalled;

    @Override
    public void populate(Connection connection) {
      this.populateCalled = true;
    }
  }

}
