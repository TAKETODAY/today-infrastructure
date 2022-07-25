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

import javax.sql.DataSource;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanNotOfRequiredTypeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class BeanFactoryDataSourceLookupTests {

  private static final String DATASOURCE_BEAN_NAME = "dataSource";

  @Test
  public void testLookupSunnyDay() {
    BeanFactory beanFactory = mock(BeanFactory.class);

    StubDataSource expectedDataSource = new StubDataSource();
    given(beanFactory.getBean(DATASOURCE_BEAN_NAME, DataSource.class)).willReturn(expectedDataSource);

    BeanFactoryDataSourceLookup lookup = new BeanFactoryDataSourceLookup();
    lookup.setBeanFactory(beanFactory);
    DataSource dataSource = lookup.getDataSource(DATASOURCE_BEAN_NAME);
    assertThat(dataSource).as("A DataSourceLookup implementation must *never* return null from " +
            "getDataSource(): this one obviously (and incorrectly) is").isNotNull();
    assertThat(dataSource).isSameAs(expectedDataSource);
  }

  @Test
  public void testLookupWhereBeanFactoryYieldsNonDataSourceType() throws Exception {
    final BeanFactory beanFactory = mock(BeanFactory.class);

    given(beanFactory.getBean(DATASOURCE_BEAN_NAME, DataSource.class)).willThrow(
            new BeanNotOfRequiredTypeException(DATASOURCE_BEAN_NAME,
                    DataSource.class, String.class));

    BeanFactoryDataSourceLookup lookup = new BeanFactoryDataSourceLookup(beanFactory);
    assertThatExceptionOfType(DataSourceLookupFailureException.class)
            .isThrownBy(() -> lookup.getDataSource(DATASOURCE_BEAN_NAME));
  }

  @Test
  public void testLookupWhereBeanFactoryHasNotBeenSupplied() throws Exception {
    BeanFactoryDataSourceLookup lookup = new BeanFactoryDataSourceLookup();
    assertThatIllegalStateException().isThrownBy(() ->
            lookup.getDataSource(DATASOURCE_BEAN_NAME));
  }

}
