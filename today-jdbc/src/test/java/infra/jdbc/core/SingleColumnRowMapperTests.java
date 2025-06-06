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

package infra.jdbc.core;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import infra.core.conversion.support.DefaultConversionService;
import infra.dao.TypeMismatchDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SingleColumnRowMapper}.
 *
 * @author Kazuki Shimizu
 */
public class SingleColumnRowMapperTests {

  @Test
  public void useDefaultConversionService() throws SQLException {
    Timestamp timestamp = new Timestamp(0);

    SingleColumnRowMapper<LocalDateTime> rowMapper = SingleColumnRowMapper.newInstance(LocalDateTime.class);

    ResultSet resultSet = mock(ResultSet.class);
    ResultSetMetaData metaData = mock(ResultSetMetaData.class);
    given(metaData.getColumnCount()).willReturn(1);
    given(resultSet.getMetaData()).willReturn(metaData);
    given(resultSet.getObject(1, LocalDateTime.class))
            .willThrow(new SQLFeatureNotSupportedException());
    given(resultSet.getTimestamp(1)).willReturn(timestamp);

    LocalDateTime actualLocalDateTime = rowMapper.mapRow(resultSet, 1);

    assertThat(actualLocalDateTime).isEqualTo(timestamp.toLocalDateTime());
  }

  @Test
  public void useCustomConversionService() throws SQLException {
    Timestamp timestamp = new Timestamp(0);

    DefaultConversionService myConversionService = new DefaultConversionService();
    myConversionService.addConverter(Timestamp.class, MyLocalDateTime.class,
            source -> new MyLocalDateTime(source.toLocalDateTime()));
    SingleColumnRowMapper<MyLocalDateTime> rowMapper =
            SingleColumnRowMapper.newInstance(MyLocalDateTime.class, myConversionService);

    ResultSet resultSet = mock(ResultSet.class);
    ResultSetMetaData metaData = mock(ResultSetMetaData.class);
    given(metaData.getColumnCount()).willReturn(1);
    given(resultSet.getMetaData()).willReturn(metaData);
    given(resultSet.getObject(1, MyLocalDateTime.class))
            .willThrow(new SQLFeatureNotSupportedException());
    given(resultSet.getObject(1)).willReturn(timestamp);

    MyLocalDateTime actualMyLocalDateTime = rowMapper.mapRow(resultSet, 1);

    assertThat(actualMyLocalDateTime).isNotNull();
    assertThat(actualMyLocalDateTime.value).isEqualTo(timestamp.toLocalDateTime());
  }

  @Test
  public void doesNotUseConversionService() throws SQLException {
    SingleColumnRowMapper<LocalDateTime> rowMapper =
            SingleColumnRowMapper.newInstance(LocalDateTime.class, null);

    ResultSet resultSet = mock(ResultSet.class);
    ResultSetMetaData metaData = mock(ResultSetMetaData.class);
    given(metaData.getColumnCount()).willReturn(1);
    given(resultSet.getMetaData()).willReturn(metaData);
    given(resultSet.getObject(1, LocalDateTime.class))
            .willThrow(new SQLFeatureNotSupportedException());
    given(resultSet.getTimestamp(1)).willReturn(new Timestamp(0));
    assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
            rowMapper.mapRow(resultSet, 1));
  }

  private static class MyLocalDateTime {

    private final LocalDateTime value;

    public MyLocalDateTime(LocalDateTime value) {
      this.value = value;
    }
  }

}
