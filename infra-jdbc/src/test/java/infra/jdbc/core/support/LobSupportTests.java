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

package infra.jdbc.core.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.dao.DataAccessException;
import infra.dao.IncorrectResultSizeDataAccessException;
import infra.jdbc.LobRetrievalFailureException;
import infra.jdbc.support.lob.LobCreator;
import infra.jdbc.support.lob.LobHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Alef Arendsen
 */
public class LobSupportTests {

  @Test
  public void testCreatingPreparedStatementCallback() throws SQLException {
    LobHandler handler = mock(LobHandler.class);
    LobCreator creator = mock(LobCreator.class);
    PreparedStatement ps = mock(PreparedStatement.class);

    given(handler.getLobCreator()).willReturn(creator);
    given(ps.executeUpdate()).willReturn(3);

    class SetValuesCalled {
      boolean b = false;
    }

    final SetValuesCalled svc = new SetValuesCalled();

    AbstractLobCreatingPreparedStatementCallback psc = new AbstractLobCreatingPreparedStatementCallback(
            handler) {
      @Override
      protected void setValues(PreparedStatement ps, LobCreator lobCreator)
              throws SQLException, DataAccessException {
        svc.b = true;
      }
    };

    assertThat(psc.doInPreparedStatement(ps)).isEqualTo(Integer.valueOf(3));
    assertThat(svc.b).isTrue();
    verify(creator).close();
    verify(handler).getLobCreator();
    verify(ps).executeUpdate();
  }

  @Test
  public void testAbstractLobStreamingResultSetExtractorNoRows() throws SQLException {
    ResultSet rset = mock(ResultSet.class);
    AbstractLobStreamingResultSetExtractor<Void> lobRse = getResultSetExtractor(false);
    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
            lobRse.extractData(rset));
    verify(rset).next();
  }

  @Test
  public void testAbstractLobStreamingResultSetExtractorOneRow() throws SQLException {
    ResultSet rset = mock(ResultSet.class);
    given(rset.next()).willReturn(true, false);
    AbstractLobStreamingResultSetExtractor<Void> lobRse = getResultSetExtractor(false);
    lobRse.extractData(rset);
    verify(rset).clearWarnings();
  }

  @Test
  public void testAbstractLobStreamingResultSetExtractorMultipleRows()
          throws SQLException {
    ResultSet rset = mock(ResultSet.class);
    given(rset.next()).willReturn(true, true, false);
    AbstractLobStreamingResultSetExtractor<Void> lobRse = getResultSetExtractor(false);
    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
            lobRse.extractData(rset));
    verify(rset).clearWarnings();
  }

  @Test
  public void testAbstractLobStreamingResultSetExtractorCorrectException()
          throws SQLException {
    ResultSet rset = mock(ResultSet.class);
    given(rset.next()).willReturn(true);
    AbstractLobStreamingResultSetExtractor<Void> lobRse = getResultSetExtractor(true);
    assertThatExceptionOfType(LobRetrievalFailureException.class).isThrownBy(() ->
            lobRse.extractData(rset));
  }

  private AbstractLobStreamingResultSetExtractor<Void> getResultSetExtractor(final boolean ex) {
    AbstractLobStreamingResultSetExtractor<Void> lobRse = new AbstractLobStreamingResultSetExtractor<Void>() {

      @Override
      protected void streamData(ResultSet rs) throws SQLException, IOException {
        if (ex) {
          throw new IOException();
        }
        else {
          rs.clearWarnings();
        }
      }
    };
    return lobRse;
  }
}
