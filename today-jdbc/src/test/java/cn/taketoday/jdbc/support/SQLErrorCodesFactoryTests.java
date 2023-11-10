/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.support;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Tests for SQLErrorCodes loading.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
class SQLErrorCodesFactoryTests {

  /**
   * Check that a default instance returns empty error codes for an unknown database.
   */
  @Test
  void defaultInstanceWithNoSuchDatabase() {
    SQLErrorCodes sec = SQLErrorCodesFactory.of().getErrorCodes("xx");
    assertThat(sec.getBadSqlGrammarCodes()).isEmpty();
    assertThat(sec.getDataIntegrityViolationCodes()).isEmpty();
  }

  /**
   * Check that a known database produces recognizable codes.
   */
  @Test
  void defaultInstanceWithOracle() {
    SQLErrorCodes sec = SQLErrorCodesFactory.of().getErrorCodes("Oracle");
    assertIsOracle(sec);
  }

  private void assertIsOracle(SQLErrorCodes sec) {
    assertThat(sec.getBadSqlGrammarCodes()).isNotEmpty();
    assertThat(sec.getDataIntegrityViolationCodes()).isNotEmpty();
    // These had better be a Bad SQL Grammar code
    assertThat(sec.getBadSqlGrammarCodes()).contains("942");
    assertThat(sec.getBadSqlGrammarCodes()).contains("6550");
    // This had better NOT be
    assertThat(sec.getBadSqlGrammarCodes()).doesNotContain("9xx42");
  }

  private void assertIsSQLServer(SQLErrorCodes sec) {
    assertThat(sec.getDatabaseProductName()).isEqualTo("Microsoft SQL Server");

    assertThat(sec.getBadSqlGrammarCodes()).isNotEmpty();

    assertThat(sec.getBadSqlGrammarCodes()).contains("156");
    assertThat(sec.getBadSqlGrammarCodes()).contains("170");
    assertThat(sec.getBadSqlGrammarCodes()).contains("207");
    assertThat(sec.getBadSqlGrammarCodes()).contains("208");
    assertThat(sec.getBadSqlGrammarCodes()).contains("209");
    assertThat(sec.getBadSqlGrammarCodes()).doesNotContain("9xx42");

    assertThat(sec.getPermissionDeniedCodes()).isNotEmpty();
    assertThat(sec.getPermissionDeniedCodes()).contains("229");

    assertThat(sec.getDuplicateKeyCodes()).isNotEmpty();
    assertThat(sec.getDuplicateKeyCodes()).contains("2601");
    assertThat(sec.getDuplicateKeyCodes()).contains("2627");

    assertThat(sec.getDataIntegrityViolationCodes()).isNotEmpty();
    assertThat(sec.getDataIntegrityViolationCodes()).contains("544");
    assertThat(sec.getDataIntegrityViolationCodes()).contains("8114");
    assertThat(sec.getDataIntegrityViolationCodes()).contains("8115");

    assertThat(sec.getDataAccessResourceFailureCodes()).isNotEmpty();
    assertThat(sec.getDataAccessResourceFailureCodes()).contains("4060");

    assertThat(sec.getCannotAcquireLockCodes()).isNotEmpty();
    assertThat(sec.getCannotAcquireLockCodes()).contains("1222");

    assertThat(sec.getDeadlockLoserCodes()).isNotEmpty();
    assertThat(sec.getDeadlockLoserCodes()).contains("1205");
  }

  private void assertIsHsql(SQLErrorCodes sec) {
    assertThat(sec.getBadSqlGrammarCodes()).isNotEmpty();
    assertThat(sec.getDataIntegrityViolationCodes()).isNotEmpty();
    // This had better be a Bad SQL Grammar code
    assertThat(sec.getBadSqlGrammarCodes()).contains("-22");
    // This had better NOT be
    assertThat(sec.getBadSqlGrammarCodes()).doesNotContain("-9");
  }

  private void assertIsDB2(SQLErrorCodes sec) {
    assertThat(sec.getBadSqlGrammarCodes()).isNotEmpty();
    assertThat(sec.getDataIntegrityViolationCodes()).isNotEmpty();

    assertThat(sec.getBadSqlGrammarCodes()).doesNotContain("942");
    // This had better NOT be
    assertThat(sec.getBadSqlGrammarCodes()).contains("-204");
  }

  private void assertIsHana(SQLErrorCodes sec) {
    assertThat(sec.getBadSqlGrammarCodes()).isNotEmpty();
    assertThat(sec.getDataIntegrityViolationCodes()).isNotEmpty();

    assertThat(sec.getBadSqlGrammarCodes()).contains("368");
    assertThat(sec.getPermissionDeniedCodes()).contains("10");
    assertThat(sec.getDuplicateKeyCodes()).contains("301");
    assertThat(sec.getDataIntegrityViolationCodes()).contains("461");
    assertThat(sec.getDataAccessResourceFailureCodes()).contains("-813");
    assertThat(sec.getInvalidResultSetAccessCodes()).contains("582");
    assertThat(sec.getCannotAcquireLockCodes()).contains("131");
    assertThat(sec.getCannotSerializeTransactionCodes()).contains("138");
    assertThat(sec.getDeadlockLoserCodes()).contains("133");
  }

  @Test
  void lookupOrder() {
    class TestSQLErrorCodesFactory extends SQLErrorCodesFactory {
      private int lookups = 0;

      @Override
      protected Resource loadResource(String path) {
        ++lookups;
        if (lookups == 1) {
          assertThat(path).isEqualTo(SQLErrorCodesFactory.SQL_ERROR_CODE_DEFAULT_PATH);
          return null;
        }
        else {
          // Should have only one more lookup
          assertThat(lookups).isEqualTo(2);
          assertThat(path).isEqualTo(SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH);
          return null;
        }
      }
    }

    // Should have failed to load without error
    TestSQLErrorCodesFactory sf = new TestSQLErrorCodesFactory();
    assertThat(sf.getErrorCodes("XX").getBadSqlGrammarCodes()).isEmpty();
    assertThat(sf.getErrorCodes("Oracle").getDataIntegrityViolationCodes()).isEmpty();
  }

  /**
   * Check that user defined error codes take precedence.
   */
  @Test
  void findUserDefinedCodes() {
    class TestSQLErrorCodesFactory extends SQLErrorCodesFactory {
      @Override
      protected Resource loadResource(String path) {
        if (SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH.equals(path)) {
          return new ClassPathResource("test-error-codes.xml", SQLErrorCodesFactoryTests.class);
        }
        return null;
      }
    }

    // Should have loaded without error
    TestSQLErrorCodesFactory sf = new TestSQLErrorCodesFactory();
    assertThat(sf.getErrorCodes("XX").getBadSqlGrammarCodes()).isEmpty();
    assertThat(sf.getErrorCodes("Oracle").getBadSqlGrammarCodes()).containsExactly("1", "2");
  }

  @Test
  void invalidUserDefinedCodeFormat() {
    class TestSQLErrorCodesFactory extends SQLErrorCodesFactory {
      @Override
      protected Resource loadResource(String path) {
        if (SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH.equals(path)) {
          // Guaranteed to be on the classpath, but most certainly NOT XML
          return new ClassPathResource("SQLExceptionTranslator.class", SQLErrorCodesFactoryTests.class);
        }
        return null;
      }
    }

    // Should have failed to load without error
    TestSQLErrorCodesFactory sf = new TestSQLErrorCodesFactory();
    assertThat(sf.getErrorCodes("XX").getBadSqlGrammarCodes()).isEmpty();
    assertThat(sf.getErrorCodes("Oracle").getBadSqlGrammarCodes()).isEmpty();
  }

  /**
   * Check that custom error codes take precedence.
   */
  @Test
  void findCustomCodes() {
    class TestSQLErrorCodesFactory extends SQLErrorCodesFactory {
      @Override
      protected Resource loadResource(String path) {
        if (SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH.equals(path)) {
          return new ClassPathResource("custom-error-codes.xml", SQLErrorCodesFactoryTests.class);
        }
        return null;
      }
    }

    // Should have loaded without error
    TestSQLErrorCodesFactory sf = new TestSQLErrorCodesFactory();
    assertThat(sf.getErrorCodes("Oracle").getCustomTranslations()).hasSize(1);
    CustomSQLErrorCodesTranslation translation = sf.getErrorCodes("Oracle").getCustomTranslations()[0];
    assertThat(translation.getExceptionClass()).isEqualTo(CustomErrorCodeException.class);
    assertThat(translation.getErrorCodes()).hasSize(1);
  }

  @Test
  void dataSourceWithNullMetadata() throws Exception {
    Connection connection = mock();
    DataSource dataSource = mock();
    given(dataSource.getConnection()).willReturn(connection);

    SQLErrorCodes sec = SQLErrorCodesFactory.of().getErrorCodes(dataSource);
    assertIsEmpty(sec);
    verify(connection).close();

    reset(connection);
    sec = SQLErrorCodesFactory.of().resolveErrorCodes(dataSource);
    assertThat(sec).isNull();
    verify(connection).close();
  }

  @Test
  void getFromDataSourceWithSQLException() throws Exception {
    SQLException expectedSQLException = new SQLException();

    DataSource dataSource = mock();
    given(dataSource.getConnection()).willThrow(expectedSQLException);

    SQLErrorCodes sec = SQLErrorCodesFactory.of().getErrorCodes(dataSource);
    assertIsEmpty(sec);

    sec = SQLErrorCodesFactory.of().resolveErrorCodes(dataSource);
    assertThat(sec).isNull();
  }

  private SQLErrorCodes getErrorCodesFromDataSource(String productName, SQLErrorCodesFactory factory) throws Exception {
    DatabaseMetaData databaseMetaData = mock();
    given(databaseMetaData.getDatabaseProductName()).willReturn(productName);

    Connection connection = mock();
    given(connection.getMetaData()).willReturn(databaseMetaData);

    DataSource dataSource = mock();
    given(dataSource.getConnection()).willReturn(connection);

    SQLErrorCodesFactory secf = (factory != null ? factory : SQLErrorCodesFactory.of());
    SQLErrorCodes sec = secf.getErrorCodes(dataSource);

    SQLErrorCodes sec2 = secf.getErrorCodes(dataSource);
    assertThat(sec).as("Cached per DataSource").isSameAs(sec2);

    verify(connection).close();
    return sec;
  }

  @Test
  void sqlServerRecognizedFromMetadata() throws Exception {
    SQLErrorCodes sec = getErrorCodesFromDataSource("MS-SQL", null);
    assertIsSQLServer(sec);
  }

  @Test
  void oracleRecognizedFromMetadata() throws Exception {
    SQLErrorCodes sec = getErrorCodesFromDataSource("Oracle", null);
    assertIsOracle(sec);
  }

  @Test
  void hsqlRecognizedFromMetadata() throws Exception {
    SQLErrorCodes sec = getErrorCodesFromDataSource("HSQL Database Engine", null);
    assertIsHsql(sec);
  }

  @Test
  void dB2RecognizedFromMetadata() throws Exception {
    SQLErrorCodes sec = getErrorCodesFromDataSource("DB2", null);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("DB2/", null);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("DB-2", null);
    assertIsEmpty(sec);
  }

  @Test
  void hanaIsRecognizedFromMetadata() throws Exception {
    SQLErrorCodes sec = getErrorCodesFromDataSource("SAP DB", null);
    assertIsHana(sec);
  }

  /**
   * Check that wild card database name works.
   */
  @Test
  void wildCardNameRecognized() throws Exception {
    class WildcardSQLErrorCodesFactory extends SQLErrorCodesFactory {
      @Override
      protected Resource loadResource(String path) {
        if (SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH.equals(path)) {
          return new ClassPathResource("wildcard-error-codes.xml", SQLErrorCodesFactoryTests.class);
        }
        return null;
      }
    }

    WildcardSQLErrorCodesFactory factory = new WildcardSQLErrorCodesFactory();
    SQLErrorCodes sec = getErrorCodesFromDataSource("DB2", factory);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("DB2 UDB for Xxxxx", factory);
    assertIsDB2(sec);

    sec = getErrorCodesFromDataSource("DB3", factory);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("DB3/", factory);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("/DB3", factory);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("/DB3", factory);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("/DB3/", factory);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("DB-3", factory);
    assertIsEmpty(sec);

    sec = getErrorCodesFromDataSource("DB1", factory);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("DB1/", factory);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("/DB1", factory);
    assertIsEmpty(sec);
    sec = getErrorCodesFromDataSource("/DB1/", factory);
    assertIsEmpty(sec);

    sec = getErrorCodesFromDataSource("DB0", factory);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("/DB0", factory);
    assertIsDB2(sec);
    sec = getErrorCodesFromDataSource("DB0/", factory);
    assertIsEmpty(sec);
    sec = getErrorCodesFromDataSource("/DB0/", factory);
    assertIsEmpty(sec);
  }

  private void assertIsEmpty(SQLErrorCodes sec) {
    assertThat(sec.getBadSqlGrammarCodes()).isEmpty();
    assertThat(sec.getDataIntegrityViolationCodes()).isEmpty();
  }

}
