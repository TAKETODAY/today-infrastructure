/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jdbc.core;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import infra.dao.InvalidDataAccessApiUsageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:30
 */
class ArgumentTypePreparedStatementSetterTests {

  @Test
  void shouldCreateSetterWithValidArguments() {
    Object[] args = { "test", 123 };
    int[] argTypes = { Types.VARCHAR, Types.INTEGER };

    ArgumentTypePreparedStatementSetter setter = new ArgumentTypePreparedStatementSetter(args, argTypes);

    assertThat(setter).isNotNull();
  }

  @Test
  void shouldCreateSetterWithNullArguments() {
    ArgumentTypePreparedStatementSetter setter = new ArgumentTypePreparedStatementSetter(null, null);

    assertThat(setter).isNotNull();
  }

  @Test
  void shouldThrowExceptionWhenArgsProvidedWithoutTypes() {
    Object[] args = { "test", 123 };

    assertThatThrownBy(() -> new ArgumentTypePreparedStatementSetter(args, null))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessage("args and argTypes parameters must match");
  }

  @Test
  void shouldThrowExceptionWhenTypesProvidedWithoutArgs() {
    int[] argTypes = { Types.VARCHAR, Types.INTEGER };

    assertThatThrownBy(() -> new ArgumentTypePreparedStatementSetter(null, argTypes))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessage("args and argTypes parameters must match");
  }

  @Test
  void shouldThrowExceptionWhenArgsAndTypesLengthMismatch() {
    Object[] args = { "test", 123, true };
    int[] argTypes = { Types.VARCHAR, Types.INTEGER };

    assertThatThrownBy(() -> new ArgumentTypePreparedStatementSetter(args, argTypes))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessage("args and argTypes parameters must match");
  }

  @Test
  void shouldSetValuesWithNullArguments() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);

    ArgumentTypePreparedStatementSetter setter = new ArgumentTypePreparedStatementSetter(null, null);
    setter.setValues(ps);

    // No interactions expected
    verifyNoInteractions(ps);
  }

  @Test
  void shouldHandleCollectionArgument() throws SQLException {
    Object[] args = { java.util.Arrays.asList("value1", "value2") };
    int[] argTypes = { Types.VARCHAR };
    PreparedStatement ps = mock(PreparedStatement.class);

    ArgumentTypePreparedStatementSetter setter = new ArgumentTypePreparedStatementSetter(args, argTypes);
    setter.setValues(ps);

    verify(ps).setString(1, "value1");
    verify(ps).setString(2, "value2");
  }

  @Test
  void shouldHandleCollectionWithArrayElements() throws SQLException {
    Object[] args = { java.util.Arrays.asList(new Object[] { "a", "b" }, "c") };
    int[] argTypes = { Types.VARCHAR };
    PreparedStatement ps = mock(PreparedStatement.class);

    ArgumentTypePreparedStatementSetter setter = new ArgumentTypePreparedStatementSetter(args, argTypes);
    setter.setValues(ps);

    verify(ps).setString(1, "a");
    verify(ps).setString(2, "b");
    verify(ps).setString(3, "c");
  }

  @Test
  void shouldCleanupParameters() {
    Object[] args = { "test", 123 };
    int[] argTypes = { Types.VARCHAR, Types.INTEGER };

    ArgumentTypePreparedStatementSetter setter = new ArgumentTypePreparedStatementSetter(args, argTypes);
    assertThatCode(setter::cleanupParameters).doesNotThrowAnyException();
  }

  @Test
  void shouldCleanupParametersWithNullArguments() {
    ArgumentTypePreparedStatementSetter setter = new ArgumentTypePreparedStatementSetter(null, null);
    assertThatCode(setter::cleanupParameters).doesNotThrowAnyException();
  }

}