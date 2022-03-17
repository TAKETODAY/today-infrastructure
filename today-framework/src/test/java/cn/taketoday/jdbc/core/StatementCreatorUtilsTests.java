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

package cn.taketoday.jdbc.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.GregorianCalendar;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 31.08.2004
 */
public class StatementCreatorUtilsTests {

  private PreparedStatement preparedStatement;

  @BeforeEach
  public void setUp() {
    preparedStatement = mock(PreparedStatement.class);
  }

  @Test
  public void testSetParameterValueWithNullAndType() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, null, null);
    verify(preparedStatement).setNull(1, Types.VARCHAR);
  }

  @Test
  public void testSetParameterValueWithNullAndTypeName() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, "mytype", null);
    verify(preparedStatement).setNull(1, Types.VARCHAR, "mytype");
  }

  @Test
  public void testSetParameterValueWithNullAndUnknownType() throws SQLException {
    StatementCreatorUtils.shouldIgnoreGetParameterType = true;
    Connection con = mock(Connection.class);
    DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
    given(preparedStatement.getConnection()).willReturn(con);
    given(dbmd.getDatabaseProductName()).willReturn("Oracle");
    given(dbmd.getDriverName()).willReturn("Oracle Driver");
    given(con.getMetaData()).willReturn(dbmd);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
    verify(preparedStatement).setNull(1, Types.NULL);
    StatementCreatorUtils.shouldIgnoreGetParameterType = false;
  }

  @Test
  public void testSetParameterValueWithNullAndUnknownTypeOnInformix() throws SQLException {
    StatementCreatorUtils.shouldIgnoreGetParameterType = true;
    Connection con = mock(Connection.class);
    DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
    given(preparedStatement.getConnection()).willReturn(con);
    given(con.getMetaData()).willReturn(dbmd);
    given(dbmd.getDatabaseProductName()).willReturn("Informix Dynamic Server");
    given(dbmd.getDriverName()).willReturn("Informix Driver");
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
    verify(dbmd).getDatabaseProductName();
    verify(dbmd).getDriverName();
    verify(preparedStatement).setObject(1, null);
    StatementCreatorUtils.shouldIgnoreGetParameterType = false;
  }

  @Test
  public void testSetParameterValueWithNullAndUnknownTypeOnDerbyEmbedded() throws SQLException {
    StatementCreatorUtils.shouldIgnoreGetParameterType = true;
    Connection con = mock(Connection.class);
    DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
    given(preparedStatement.getConnection()).willReturn(con);
    given(con.getMetaData()).willReturn(dbmd);
    given(dbmd.getDatabaseProductName()).willReturn("Apache Derby");
    given(dbmd.getDriverName()).willReturn("Apache Derby Embedded Driver");
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
    verify(dbmd).getDatabaseProductName();
    verify(dbmd).getDriverName();
    verify(preparedStatement).setNull(1, Types.VARCHAR);
    StatementCreatorUtils.shouldIgnoreGetParameterType = false;
  }

  @Test
  public void testSetParameterValueWithNullAndGetParameterTypeWorking() throws SQLException {
    ParameterMetaData pmd = mock(ParameterMetaData.class);
    given(preparedStatement.getParameterMetaData()).willReturn(pmd);
    given(pmd.getParameterType(1)).willReturn(Types.SMALLINT);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
    verify(pmd).getParameterType(1);
    verify(preparedStatement, never()).getConnection();
    verify(preparedStatement).setNull(1, Types.SMALLINT);
  }

  @Test
  public void testSetParameterValueWithString() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, null, "test");
    verify(preparedStatement).setString(1, "test");
  }

  @Test
  public void testSetParameterValueWithStringAndSpecialType() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.CHAR, null, "test");
    verify(preparedStatement).setObject(1, "test", Types.CHAR);
  }

  @Test
  public void testSetParameterValueWithStringAndUnknownType() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, "test");
    verify(preparedStatement).setString(1, "test");
  }

  @Test
  public void testSetParameterValueWithSqlDate() throws SQLException {
    java.sql.Date date = new java.sql.Date(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.DATE, null, date);
    verify(preparedStatement).setDate(1, date);
  }

  @Test
  public void testSetParameterValueWithDateAndUtilDate() throws SQLException {
    java.util.Date date = new java.util.Date(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.DATE, null, date);
    verify(preparedStatement).setDate(1, new java.sql.Date(1000));
  }

  @Test
  public void testSetParameterValueWithDateAndCalendar() throws SQLException {
    java.util.Calendar cal = new GregorianCalendar();
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.DATE, null, cal);
    verify(preparedStatement).setDate(1, new java.sql.Date(cal.getTime().getTime()), cal);
  }

  @Test
  public void testSetParameterValueWithSqlTime() throws SQLException {
    java.sql.Time time = new java.sql.Time(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIME, null, time);
    verify(preparedStatement).setTime(1, time);
  }

  @Test
  public void testSetParameterValueWithTimeAndUtilDate() throws SQLException {
    java.util.Date date = new java.util.Date(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIME, null, date);
    verify(preparedStatement).setTime(1, new java.sql.Time(1000));
  }

  @Test
  public void testSetParameterValueWithTimeAndCalendar() throws SQLException {
    java.util.Calendar cal = new GregorianCalendar();
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIME, null, cal);
    verify(preparedStatement).setTime(1, new java.sql.Time(cal.getTime().getTime()), cal);
  }

  @Test
  public void testSetParameterValueWithSqlTimestamp() throws SQLException {
    java.sql.Timestamp timestamp = new java.sql.Timestamp(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP, null, timestamp);
    verify(preparedStatement).setTimestamp(1, timestamp);
  }

  @Test
  public void testSetParameterValueWithTimestampAndUtilDate() throws SQLException {
    java.util.Date date = new java.util.Date(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP, null, date);
    verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(1000));
  }

  @Test
  public void testSetParameterValueWithTimestampAndCalendar() throws SQLException {
    java.util.Calendar cal = new GregorianCalendar();
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP, null, cal);
    verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(cal.getTime().getTime()), cal);
  }

  @Test
  public void testSetParameterValueWithDateAndUnknownType() throws SQLException {
    java.util.Date date = new java.util.Date(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, date);
    verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(1000));
  }

  @Test
  public void testSetParameterValueWithCalendarAndUnknownType() throws SQLException {
    java.util.Calendar cal = new GregorianCalendar();
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, cal);
    verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(cal.getTime().getTime()), cal);
  }

  @Test  // SPR-8571
  public void testSetParameterValueWithStringAndVendorSpecificType() throws SQLException {
    Connection con = mock(Connection.class);
    DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
    given(preparedStatement.getConnection()).willReturn(con);
    given(dbmd.getDatabaseProductName()).willReturn("Oracle");
    given(con.getMetaData()).willReturn(dbmd);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.OTHER, null, "test");
    verify(preparedStatement).setString(1, "test");
  }

  @Test  // SPR-8571
  public void testSetParameterValueWithNullAndVendorSpecificType() throws SQLException {
    StatementCreatorUtils.shouldIgnoreGetParameterType = true;
    Connection con = mock(Connection.class);
    DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
    given(preparedStatement.getConnection()).willReturn(con);
    given(dbmd.getDatabaseProductName()).willReturn("Oracle");
    given(dbmd.getDriverName()).willReturn("Oracle Driver");
    given(con.getMetaData()).willReturn(dbmd);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.OTHER, null, null);
    verify(preparedStatement).setNull(1, Types.NULL);
    StatementCreatorUtils.shouldIgnoreGetParameterType = false;
  }

}
