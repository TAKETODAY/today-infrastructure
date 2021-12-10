/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.core.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import cn.taketoday.jdbc.support.lob.LobCreator;
import cn.taketoday.jdbc.support.lob.LobHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Test cases for the SQL LOB value:
 *
 * BLOB:
 * 1. Types.BLOB: setBlobAsBytes (byte[])
 * 2. String: setBlobAsBytes (byte[])
 * 3. else: IllegalArgumentException
 *
 * CLOB:
 * 4. String or NULL: setClobAsString (String)
 * 5. InputStream: setClobAsAsciiStream (InputStream)
 * 6. Reader: setClobAsCharacterStream (Reader)
 * 7. else: IllegalArgumentException
 *
 * @author Alef Arendsen
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SqlLobValueTests {

  @Mock
  private PreparedStatement preparedStatement;

  @Mock
  private LobHandler handler;

  @Mock
  private LobCreator creator;

  @Captor
  private ArgumentCaptor<InputStream> inputStreamCaptor;

  @BeforeEach
  void setUp() {
    given(handler.getLobCreator()).willReturn(creator);
  }

  @Test
  void test1() throws SQLException {
    byte[] testBytes = "Bla".getBytes();
    SqlLobValue lob = new SqlLobValue(testBytes, handler);
    lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test");
    verify(creator).setBlobAsBytes(preparedStatement, 1, testBytes);
  }

  @Test
  void test2() throws SQLException {
    String testString = "Bla";
    SqlLobValue lob = new SqlLobValue(testString, handler);
    lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test");
    verify(creator).setBlobAsBytes(preparedStatement, 1, testString.getBytes());
  }

  @Test
  void test3() throws SQLException {
    SqlLobValue lob = new SqlLobValue(new InputStreamReader(new ByteArrayInputStream("Bla".getBytes())), 12);
    assertThatIllegalArgumentException().isThrownBy(() ->
            lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test"));
  }

  @Test
  void test4() throws SQLException {
    String testContent = "Bla";
    SqlLobValue lob = new SqlLobValue(testContent, handler);
    lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");
    verify(creator).setClobAsString(preparedStatement, 1, testContent);
  }

  @Test
  void test5() throws Exception {
    byte[] testContent = "Bla".getBytes();
    SqlLobValue lob = new SqlLobValue(new ByteArrayInputStream(testContent), 3, handler);
    lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");
    verify(creator).setClobAsAsciiStream(eq(preparedStatement), eq(1), inputStreamCaptor.capture(), eq(3));
    byte[] bytes = new byte[3];
    inputStreamCaptor.getValue().read(bytes);
    assertThat(bytes).isEqualTo(testContent);
  }

  @Test
  void test6() throws SQLException {
    byte[] testContent = "Bla".getBytes();
    ByteArrayInputStream bais = new ByteArrayInputStream(testContent);
    InputStreamReader reader = new InputStreamReader(bais);
    SqlLobValue lob = new SqlLobValue(reader, 3, handler);
    lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");
    verify(creator).setClobAsCharacterStream(eq(preparedStatement), eq(1), eq(reader), eq(3));
  }

  @Test
  void test7() throws SQLException {
    SqlLobValue lob = new SqlLobValue("bla".getBytes());
    assertThatIllegalArgumentException().isThrownBy(() ->
            lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test"));
  }

  @Test
  void testOtherConstructors() throws SQLException {
    // a bit BS, but we need to test them, as long as they don't throw exceptions

    SqlLobValue lob = new SqlLobValue("bla");
    lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");

    SqlLobValue lob2 = new SqlLobValue("bla".getBytes());
    assertThatIllegalArgumentException().isThrownBy(() ->
            lob2.setTypeValue(preparedStatement, 1, Types.CLOB, "test"));

    lob = new SqlLobValue(new ByteArrayInputStream("bla".getBytes()), 3);
    lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");

    lob = new SqlLobValue(new InputStreamReader(new ByteArrayInputStream(
            "bla".getBytes())), 3);
    lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");

    // same for BLOB
    lob = new SqlLobValue("bla");
    lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test");

    lob = new SqlLobValue("bla".getBytes());
    lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test");

    lob = new SqlLobValue(new ByteArrayInputStream("bla".getBytes()), 3);
    lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test");

    SqlLobValue lob3 = new SqlLobValue(new InputStreamReader(new ByteArrayInputStream(
            "bla".getBytes())), 3);
    assertThatIllegalArgumentException().isThrownBy(() ->
            lob3.setTypeValue(preparedStatement, 1, Types.BLOB, "test"));
  }

  @Test
  void testCorrectCleanup() throws SQLException {
    SqlLobValue lob = new SqlLobValue("Bla", handler);
    lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");
    lob.cleanup();
    verify(creator).setClobAsString(preparedStatement, 1, "Bla");
    verify(creator).close();
  }

  @Test
  void testOtherSqlType() throws SQLException {
    SqlLobValue lob = new SqlLobValue("Bla", handler);
    assertThatIllegalArgumentException().isThrownBy(() ->
            lob.setTypeValue(preparedStatement, 1, Types.SMALLINT, "test"));
  }

}
