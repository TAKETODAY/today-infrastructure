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

package cn.taketoday.jdbc.core.metadata;

import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/11/27 21:50
 */
class GenericCallMetaDataProviderTests {

  private final DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);

  @Test
  void procedureNameWithPatternIsEscape() throws SQLException {
    given(this.databaseMetaData.getSearchStringEscape()).willReturn("@");
    GenericCallMetaDataProvider provider = new GenericCallMetaDataProvider(this.databaseMetaData);
    given(this.databaseMetaData.getProcedures(null, null, "MY@_PROCEDURE"))
            .willThrow(new IllegalStateException("Expected"));
    assertThatIllegalStateException().isThrownBy(() -> provider.initializeWithProcedureColumnMetaData(
            this.databaseMetaData, null, null, "my_procedure"));
    verify(this.databaseMetaData).getProcedures(null, null, "MY@_PROCEDURE");
  }

  @Test
  void schemaNameWithPatternIsEscape() throws SQLException {
    given(this.databaseMetaData.getSearchStringEscape()).willReturn("@");
    GenericCallMetaDataProvider provider = new GenericCallMetaDataProvider(this.databaseMetaData);
    given(this.databaseMetaData.getProcedures(null, "MY@_SCHEMA", "TEST"))
            .willThrow(new IllegalStateException("Expected"));
    assertThatIllegalStateException().isThrownBy(() -> provider.initializeWithProcedureColumnMetaData(
            this.databaseMetaData, null, "my_schema", "test"));
    verify(this.databaseMetaData).getProcedures(null, "MY@_SCHEMA", "TEST");
  }

  @Test
  void nameIsNotEscapedIfEscapeCharacterIsNotAvailable() throws SQLException {
    given(this.databaseMetaData.getSearchStringEscape()).willReturn(null);
    GenericCallMetaDataProvider provider = new GenericCallMetaDataProvider(this.databaseMetaData);
    given(this.databaseMetaData.getProcedures(null, "MY_SCHEMA", "MY_TEST"))
            .willThrow(new IllegalStateException("Expected"));
    assertThatIllegalStateException().isThrownBy(() -> provider.initializeWithProcedureColumnMetaData(
            this.databaseMetaData, null, "my_schema", "my_test"));
    verify(this.databaseMetaData).getProcedures(null, "MY_SCHEMA", "MY_TEST");
  }

}