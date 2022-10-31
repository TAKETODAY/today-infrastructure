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

import cn.taketoday.jdbc.config.DatabaseDriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DatabaseDriver}.
 *
 * @author Phillip Webb
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 */
class DatabaseDriverTests {

  @Test
  void classNameForKnownDatabase() {
    String driverClassName = DatabaseDriver.fromJdbcUrl("jdbc:postgresql://hostname/dbname").getDriverClassName();
    assertThat(driverClassName).isEqualTo("org.postgresql.Driver");
  }

  @Test
  void nullClassNameForUnknownDatabase() {
    String driverClassName = DatabaseDriver.fromJdbcUrl("jdbc:unknowndb://hostname/dbname").getDriverClassName();
    assertThat(driverClassName).isNull();
  }

  @Test
  void unknownOnNullJdbcUrl() {
    DatabaseDriver actual = DatabaseDriver.fromJdbcUrl(null);
    assertThat(actual).isEqualTo(DatabaseDriver.UNKNOWN);
  }

  @Test
  void failureOnMalformedJdbcUrl() {
    assertThatIllegalArgumentException().isThrownBy(() -> DatabaseDriver.fromJdbcUrl("malformed:url"))
            .withMessageContaining("URL must start with");
  }

  @Test
  void unknownOnNullProductName() {
    DatabaseDriver actual = DatabaseDriver.fromProductName(null);
    assertThat(actual).isEqualTo(DatabaseDriver.UNKNOWN);
  }

  @Test
  void databaseProductNameLookups() {
    assertThat(DatabaseDriver.fromProductName("newone")).isEqualTo(DatabaseDriver.UNKNOWN);
    assertThat(DatabaseDriver.fromProductName("Apache Derby")).isEqualTo(DatabaseDriver.DERBY);
    assertThat(DatabaseDriver.fromProductName("H2")).isEqualTo(DatabaseDriver.H2);
    assertThat(DatabaseDriver.fromProductName("HDB")).isEqualTo(DatabaseDriver.HANA);
    assertThat(DatabaseDriver.fromProductName("HSQL Database Engine")).isEqualTo(DatabaseDriver.HSQLDB);
    assertThat(DatabaseDriver.fromProductName("SQLite")).isEqualTo(DatabaseDriver.SQLITE);
    assertThat(DatabaseDriver.fromProductName("MySQL")).isEqualTo(DatabaseDriver.MYSQL);
    assertThat(DatabaseDriver.fromProductName("MariaDB")).isEqualTo(DatabaseDriver.MARIADB);
    assertThat(DatabaseDriver.fromProductName("Oracle")).isEqualTo(DatabaseDriver.ORACLE);
    assertThat(DatabaseDriver.fromProductName("PostgreSQL")).isEqualTo(DatabaseDriver.POSTGRESQL);
    assertThat(DatabaseDriver.fromProductName("Redshift")).isEqualTo(DatabaseDriver.REDSHIFT);
    assertThat(DatabaseDriver.fromProductName("Microsoft SQL Server")).isEqualTo(DatabaseDriver.SQLSERVER);
    assertThat(DatabaseDriver.fromProductName("SQL SERVER")).isEqualTo(DatabaseDriver.SQLSERVER);
    assertThat(DatabaseDriver.fromProductName("DB2")).isEqualTo(DatabaseDriver.DB2);
    assertThat(DatabaseDriver.fromProductName("Firebird 2.5.WI")).isEqualTo(DatabaseDriver.FIREBIRD);
    assertThat(DatabaseDriver.fromProductName("Firebird 2.1.LI")).isEqualTo(DatabaseDriver.FIREBIRD);
    assertThat(DatabaseDriver.fromProductName("DB2/LINUXX8664")).isEqualTo(DatabaseDriver.DB2);
    assertThat(DatabaseDriver.fromProductName("DB2 UDB for AS/400")).isEqualTo(DatabaseDriver.DB2_AS400);
    assertThat(DatabaseDriver.fromProductName("DB3 XDB for AS/400")).isEqualTo(DatabaseDriver.DB2_AS400);
    assertThat(DatabaseDriver.fromProductName("Teradata")).isEqualTo(DatabaseDriver.TERADATA);
    assertThat(DatabaseDriver.fromProductName("Informix Dynamic Server")).isEqualTo(DatabaseDriver.INFORMIX);
    assertThat(DatabaseDriver.fromProductName("Apache Phoenix")).isEqualTo(DatabaseDriver.PHOENIX);
  }

  @Test
  void databaseJdbcUrlLookups() {
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:newone://localhost")).isEqualTo(DatabaseDriver.UNKNOWN);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:derby:sample")).isEqualTo(DatabaseDriver.DERBY);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:h2:~/sample")).isEqualTo(DatabaseDriver.H2);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:hsqldb:hsql://localhost")).isEqualTo(DatabaseDriver.HSQLDB);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:sqlite:sample.db")).isEqualTo(DatabaseDriver.SQLITE);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:mysql://localhost:3306/sample")).isEqualTo(DatabaseDriver.MYSQL);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:oracle:thin:@localhost:1521:orcl"))
            .isEqualTo(DatabaseDriver.ORACLE);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:postgresql://127.0.0.1:5432/sample"))
            .isEqualTo(DatabaseDriver.POSTGRESQL);
    assertThat(DatabaseDriver.fromJdbcUrl(
            "jdbc:redshift://examplecluster.abc123xyz789.us-west-2.redshift.amazonaws.com:5439/sample"))
            .isEqualTo(DatabaseDriver.REDSHIFT);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:jtds:sqlserver://127.0.0.1:1433/sample"))
            .isEqualTo(DatabaseDriver.JTDS);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:sap:localhost")).isEqualTo(DatabaseDriver.HANA);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:sqlserver://127.0.0.1:1433")).isEqualTo(DatabaseDriver.SQLSERVER);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:firebirdsql://localhost/sample"))
            .isEqualTo(DatabaseDriver.FIREBIRD);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:firebird://localhost/sample")).isEqualTo(DatabaseDriver.FIREBIRD);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:db2://localhost:50000/sample ")).isEqualTo(DatabaseDriver.DB2);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:as400://localhost")).isEqualTo(DatabaseDriver.DB2_AS400);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:teradata://localhost/SAMPLE")).isEqualTo(DatabaseDriver.TERADATA);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:informix-sqli://localhost:1533/sample"))
            .isEqualTo(DatabaseDriver.INFORMIX);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:informix-direct://sample")).isEqualTo(DatabaseDriver.INFORMIX);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:phoenix:localhost")).isEqualTo(DatabaseDriver.PHOENIX);
    assertThat(DatabaseDriver.fromJdbcUrl("jdbc:tc:mysql://localhost:3306/sample"))
            .isEqualTo(DatabaseDriver.TESTCONTAINERS);
  }

}
