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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.jdbc.LobRetrievalFailureException;
import cn.taketoday.jdbc.support.lob.LobCreator;
import cn.taketoday.jdbc.support.lob.LobHandler;

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
