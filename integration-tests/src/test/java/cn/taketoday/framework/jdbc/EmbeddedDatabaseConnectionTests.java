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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.jdbc.EmbeddedDatabaseConnection;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabase;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EmbeddedDatabaseConnection}.
 *
 * @author Stephane Nicoll
 * @author Nidhi Desai
 */
class EmbeddedDatabaseConnectionTests {

  @Test
  void h2CustomDatabaseName() {
    assertThat(EmbeddedDatabaseConnection.H2.getUrl("mydb"))
            .isEqualTo("jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
  }

  @Test
  void derbyCustomDatabaseName() {
    assertThat(EmbeddedDatabaseConnection.DERBY.getUrl("myderbydb"))
            .isEqualTo("jdbc:derby:memory:myderbydb;create=true");
  }

  @Test
  void hsqldbCustomDatabaseName() {
    assertThat(EmbeddedDatabaseConnection.HSQLDB.getUrl("myhsqldb")).isEqualTo("jdbc:hsqldb:mem:myhsqldb");
  }

  @Test
  void getUrlWithNullDatabaseNameForHsqldb() {
    assertThatIllegalArgumentException().isThrownBy(() -> EmbeddedDatabaseConnection.HSQLDB.getUrl(null))
            .withMessageContaining("DatabaseName must not be empty");
  }

  @Test
  void getUrlWithEmptyDatabaseNameForHsqldb() {
    assertThatIllegalArgumentException().isThrownBy(() -> EmbeddedDatabaseConnection.HSQLDB.getUrl("  "))
            .withMessageContaining("DatabaseName must not be empty");
  }

  @ParameterizedTest(name = "{0} - {1}")
  @MethodSource("embeddedDriverAndUrlParameters")
  void isEmbeddedWithDriverAndUrl(String driverClassName, String url, boolean embedded) {
    assertThat(EmbeddedDatabaseConnection.isEmbedded(driverClassName, url)).isEqualTo(embedded);
  }

  static Object[] embeddedDriverAndUrlParameters() {
    return new Object[] {
            new Object[] { EmbeddedDatabaseConnection.H2.getDriverClassName(), "jdbc:h2:~/test", false },
            new Object[] { EmbeddedDatabaseConnection.H2.getDriverClassName(), "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                    true },
            new Object[] { EmbeddedDatabaseConnection.H2.getDriverClassName(), null, true },
            new Object[] { EmbeddedDatabaseConnection.HSQLDB.getDriverClassName(), "jdbc:hsqldb:hsql://localhost",
                    false },
            new Object[] { EmbeddedDatabaseConnection.HSQLDB.getDriverClassName(), "jdbc:hsqldb:mem:test", true },
            new Object[] { EmbeddedDatabaseConnection.HSQLDB.getDriverClassName(), null, true },
            new Object[] { EmbeddedDatabaseConnection.DERBY.getDriverClassName(), "jdbc:derby:memory:test", true },
            new Object[] { EmbeddedDatabaseConnection.DERBY.getDriverClassName(), null, true },
            new Object[] { "com.mysql.cj.jdbc.Driver", "jdbc:mysql:mem:test", false },
            new Object[] { "com.mysql.cj.jdbc.Driver", null, false },
            new Object[] { null, "jdbc:none:mem:test", false }, new Object[] { null, null, false } };
  }

  @Test
  void isEmbeddedWithH2DataSource() {
    testEmbeddedDatabase(new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build());
  }

  @Test
  void isEmbeddedWithHsqlDataSource() {
    testEmbeddedDatabase(new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).build());
  }

  @Test
  void isEmbeddedWithDerbyDataSource() {
    testEmbeddedDatabase(new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.DERBY).build());
  }

  void testEmbeddedDatabase(EmbeddedDatabase database) {
    try {
      assertThat(EmbeddedDatabaseConnection.isEmbedded(database)).isTrue();
    }
    finally {
      database.shutdown();
    }
  }

  @Test
  void isEmbeddedWithUnknownDataSource() throws SQLException {
    assertThat(EmbeddedDatabaseConnection.isEmbedded(mockDataSource("unknown-db", null))).isFalse();
  }

  @Test
  void isEmbeddedWithH2File() throws SQLException {
    assertThat(EmbeddedDatabaseConnection
            .isEmbedded(mockDataSource(EmbeddedDatabaseConnection.H2.getDriverClassName(), "jdbc:h2:~/test")))
            .isFalse();
  }

  @Test
  void isEmbeddedWithMissingDriverClassMetadata() throws SQLException {
    assertThat(EmbeddedDatabaseConnection.isEmbedded(mockDataSource(null, "jdbc:h2:meme:test"))).isFalse();
  }

  @Test
  void isEmbeddedWithMissingUrlMetadata() throws SQLException {
    assertThat(EmbeddedDatabaseConnection
            .isEmbedded(mockDataSource(EmbeddedDatabaseConnection.H2.getDriverClassName(), null))).isTrue();
  }

  DataSource mockDataSource(String productName, String connectionUrl) throws SQLException {
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    given(metaData.getDatabaseProductName()).willReturn(productName);
    given(metaData.getURL()).willReturn(connectionUrl);
    Connection connection = mock(Connection.class);
    given(connection.getMetaData()).willReturn(metaData);
    DataSource dataSource = mock(DataSource.class);
    given(dataSource.getConnection()).willReturn(connection);
    return dataSource;
  }

}
