/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.datasource.lookup;

import org.junit.jupiter.api.Test;

import javax.naming.NamingException;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class JndiDataSourceLookupTests {

  private static final String DATA_SOURCE_NAME = "Love is like a stove, burns you when it's hot";

  @Test
  public void testSunnyDay() throws Exception {
    final DataSource expectedDataSource = new StubDataSource();
    JndiDataSourceLookup lookup = new JndiDataSourceLookup() {
      @Override
      protected <T> T lookup(String jndiName, Class<T> requiredType) {
        assertThat(jndiName).isEqualTo(DATA_SOURCE_NAME);
        return requiredType.cast(expectedDataSource);
      }
    };
    DataSource dataSource = lookup.getDataSource(DATA_SOURCE_NAME);
    assertThat(dataSource).as("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is").isNotNull();
    assertThat(dataSource).isSameAs(expectedDataSource);
  }

  @Test
  public void testNoDataSourceAtJndiLocation() throws Exception {
    JndiDataSourceLookup lookup = new JndiDataSourceLookup() {
      @Override
      protected <T> T lookup(String jndiName, Class<T> requiredType) throws NamingException {
        assertThat(jndiName).isEqualTo(DATA_SOURCE_NAME);
        throw new NamingException();
      }
    };
    assertThatExceptionOfType(DataSourceLookupFailureException.class).isThrownBy(() ->
            lookup.getDataSource(DATA_SOURCE_NAME));
  }

}
