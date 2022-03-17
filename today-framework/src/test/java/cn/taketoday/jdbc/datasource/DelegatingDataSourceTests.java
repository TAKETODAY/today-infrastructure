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

package cn.taketoday.jdbc.datasource;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DelegatingDataSource}.
 *
 * @author Phillip Webb
 */
public class DelegatingDataSourceTests {

  private final DataSource delegate = mock(DataSource.class);

  private DelegatingDataSource dataSource = new DelegatingDataSource(delegate);

  @Test
  public void shouldDelegateGetConnection() throws Exception {
    Connection connection = mock(Connection.class);
    given(delegate.getConnection()).willReturn(connection);
    assertThat(dataSource.getConnection()).isEqualTo(connection);
  }

  @Test
  public void shouldDelegateGetConnectionWithUsernameAndPassword() throws Exception {
    Connection connection = mock(Connection.class);
    String username = "username";
    String password = "password";
    given(delegate.getConnection(username, password)).willReturn(connection);
    assertThat(dataSource.getConnection(username, password)).isEqualTo(connection);
  }

  @Test
  public void shouldDelegateGetLogWriter() throws Exception {
    PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());
    given(delegate.getLogWriter()).willReturn(writer);
    assertThat(dataSource.getLogWriter()).isEqualTo(writer);
  }

  @Test
  public void shouldDelegateSetLogWriter() throws Exception {
    PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());
    dataSource.setLogWriter(writer);
    verify(delegate).setLogWriter(writer);
  }

  @Test
  public void shouldDelegateGetLoginTimeout() throws Exception {
    int timeout = 123;
    given(delegate.getLoginTimeout()).willReturn(timeout);
    assertThat(dataSource.getLoginTimeout()).isEqualTo(timeout);
  }

  @Test
  public void shouldDelegateSetLoginTimeoutWithSeconds() throws Exception {
    int timeout = 123;
    dataSource.setLoginTimeout(timeout);
    verify(delegate).setLoginTimeout(timeout);
  }

  @Test
  public void shouldDelegateUnwrapWithoutImplementing() throws Exception {
    ExampleWrapper wrapper = mock(ExampleWrapper.class);
    given(delegate.unwrap(ExampleWrapper.class)).willReturn(wrapper);
    assertThat(dataSource.unwrap(ExampleWrapper.class)).isEqualTo(wrapper);
  }

  @Test
  public void shouldDelegateUnwrapImplementing() throws Exception {
    dataSource = new DelegatingDataSourceWithWrapper();
    assertThat(dataSource.unwrap(ExampleWrapper.class)).isSameAs(dataSource);
  }

  @Test
  public void shouldDelegateIsWrapperForWithoutImplementing() throws Exception {
    given(delegate.isWrapperFor(ExampleWrapper.class)).willReturn(true);
    assertThat(dataSource.isWrapperFor(ExampleWrapper.class)).isTrue();
  }

  @Test
  public void shouldDelegateIsWrapperForImplementing() throws Exception {
    dataSource = new DelegatingDataSourceWithWrapper();
    assertThat(dataSource.isWrapperFor(ExampleWrapper.class)).isTrue();
  }

  public static interface ExampleWrapper {
  }

  private static class DelegatingDataSourceWithWrapper extends DelegatingDataSource
          implements ExampleWrapper {
  }
}
