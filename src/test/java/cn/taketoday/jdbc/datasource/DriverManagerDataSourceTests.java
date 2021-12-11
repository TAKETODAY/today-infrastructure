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

import java.sql.Connection;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Rod Johnson
 */
public class DriverManagerDataSourceTests {

  private Connection connection = mock(Connection.class);

  @Test
  public void testStandardUsage() throws Exception {
    final String jdbcUrl = "url";
    final String uname = "uname";
    final String pwd = "pwd";

    class TestDriverManagerDataSource extends DriverManagerDataSource {
      @Override
      protected Connection getConnectionFromDriverManager(String url, Properties props) {
        assertThat(url).isEqualTo(jdbcUrl);
        assertThat(props.getProperty("user")).isEqualTo(uname);
        assertThat(props.getProperty("password")).isEqualTo(pwd);
        return connection;
      }
    }

    DriverManagerDataSource ds = new TestDriverManagerDataSource();
    //ds.setDriverClassName("foobar");
    ds.setUrl(jdbcUrl);
    ds.setUsername(uname);
    ds.setPassword(pwd);

    Connection actualCon = ds.getConnection();
    assertThat(actualCon == connection).isTrue();

    assertThat(ds.getUrl().equals(jdbcUrl)).isTrue();
    assertThat(ds.getPassword().equals(pwd)).isTrue();
    assertThat(ds.getUsername().equals(uname)).isTrue();
  }

  @Test
  public void testUsageWithConnectionProperties() throws Exception {
    final String jdbcUrl = "url";

    final Properties connProps = new Properties();
    connProps.setProperty("myProp", "myValue");
    connProps.setProperty("yourProp", "yourValue");
    connProps.setProperty("user", "uname");
    connProps.setProperty("password", "pwd");

    class TestDriverManagerDataSource extends DriverManagerDataSource {
      @Override
      protected Connection getConnectionFromDriverManager(String url, Properties props) {
        assertThat(url).isEqualTo(jdbcUrl);
        assertThat(props.getProperty("user")).isEqualTo("uname");
        assertThat(props.getProperty("password")).isEqualTo("pwd");
        assertThat(props.getProperty("myProp")).isEqualTo("myValue");
        assertThat(props.getProperty("yourProp")).isEqualTo("yourValue");
        return connection;
      }
    }

    DriverManagerDataSource ds = new TestDriverManagerDataSource();
    //ds.setDriverClassName("foobar");
    ds.setUrl(jdbcUrl);
    ds.setConnectionProperties(connProps);

    Connection actualCon = ds.getConnection();
    assertThat(actualCon == connection).isTrue();

    assertThat(ds.getUrl().equals(jdbcUrl)).isTrue();
  }

  @Test
  public void testUsageWithConnectionPropertiesAndUserCredentials() throws Exception {
    final String jdbcUrl = "url";
    final String uname = "uname";
    final String pwd = "pwd";

    final Properties connProps = new Properties();
    connProps.setProperty("myProp", "myValue");
    connProps.setProperty("yourProp", "yourValue");
    connProps.setProperty("user", "uname2");
    connProps.setProperty("password", "pwd2");

    class TestDriverManagerDataSource extends DriverManagerDataSource {
      @Override
      protected Connection getConnectionFromDriverManager(String url, Properties props) {
        assertThat(url).isEqualTo(jdbcUrl);
        assertThat(props.getProperty("user")).isEqualTo(uname);
        assertThat(props.getProperty("password")).isEqualTo(pwd);
        assertThat(props.getProperty("myProp")).isEqualTo("myValue");
        assertThat(props.getProperty("yourProp")).isEqualTo("yourValue");
        return connection;
      }
    }

    DriverManagerDataSource ds = new TestDriverManagerDataSource();
    //ds.setDriverClassName("foobar");
    ds.setUrl(jdbcUrl);
    ds.setUsername(uname);
    ds.setPassword(pwd);
    ds.setConnectionProperties(connProps);

    Connection actualCon = ds.getConnection();
    assertThat(actualCon == connection).isTrue();

    assertThat(ds.getUrl().equals(jdbcUrl)).isTrue();
    assertThat(ds.getPassword().equals(pwd)).isTrue();
    assertThat(ds.getUsername().equals(uname)).isTrue();
  }

  @Test
  public void testInvalidClassName() throws Exception {
    String bogusClassName = "foobar";
    DriverManagerDataSource ds = new DriverManagerDataSource();
    assertThatIllegalStateException().isThrownBy(() ->
                    ds.setDriverClassName(bogusClassName))
            .withCauseInstanceOf(ClassNotFoundException.class);
  }

}
