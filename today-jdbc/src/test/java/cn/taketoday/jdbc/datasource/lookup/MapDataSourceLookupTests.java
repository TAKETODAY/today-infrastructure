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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class MapDataSourceLookupTests {

  private static final String DATA_SOURCE_NAME = "dataSource";

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void getDataSourcesReturnsUnmodifiableMap() throws Exception {
    MapDataSourceLookup lookup = new MapDataSourceLookup();
    Map dataSources = lookup.getDataSources();

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            dataSources.put("", ""));
  }

  @Test
  public void lookupSunnyDay() throws Exception {
    Map<String, DataSource> dataSources = new HashMap<>();
    StubDataSource expectedDataSource = new StubDataSource();
    dataSources.put(DATA_SOURCE_NAME, expectedDataSource);
    MapDataSourceLookup lookup = new MapDataSourceLookup();
    lookup.setDataSources(dataSources);
    DataSource dataSource = lookup.getDataSource(DATA_SOURCE_NAME);
    assertThat(dataSource).as("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is").isNotNull();
    assertThat(dataSource).isSameAs(expectedDataSource);
  }

  @Test
  public void setDataSourcesIsAnIdempotentOperation() throws Exception {
    Map<String, DataSource> dataSources = new HashMap<>();
    StubDataSource expectedDataSource = new StubDataSource();
    dataSources.put(DATA_SOURCE_NAME, expectedDataSource);
    MapDataSourceLookup lookup = new MapDataSourceLookup();
    lookup.setDataSources(dataSources);
    lookup.setDataSources(null); // must be idempotent (i.e. the following lookup must still work);
    DataSource dataSource = lookup.getDataSource(DATA_SOURCE_NAME);
    assertThat(dataSource).as("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is").isNotNull();
    assertThat(dataSource).isSameAs(expectedDataSource);
  }

  @Test
  public void addingDataSourcePermitsOverride() throws Exception {
    Map<String, DataSource> dataSources = new HashMap<>();
    StubDataSource overridenDataSource = new StubDataSource();
    StubDataSource expectedDataSource = new StubDataSource();
    dataSources.put(DATA_SOURCE_NAME, overridenDataSource);
    MapDataSourceLookup lookup = new MapDataSourceLookup();
    lookup.setDataSources(dataSources);
    lookup.addDataSource(DATA_SOURCE_NAME, expectedDataSource); // must override existing entry
    DataSource dataSource = lookup.getDataSource(DATA_SOURCE_NAME);
    assertThat(dataSource).as("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is").isNotNull();
    assertThat(dataSource).isSameAs(expectedDataSource);
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void getDataSourceWhereSuppliedMapHasNonDataSourceTypeUnderSpecifiedKey() throws Exception {
    Map dataSources = new HashMap();
    dataSources.put(DATA_SOURCE_NAME, new Object());
    MapDataSourceLookup lookup = new MapDataSourceLookup(dataSources);

    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
            lookup.getDataSource(DATA_SOURCE_NAME));
  }

  @Test
  public void getDataSourceWhereSuppliedMapHasNoEntryForSpecifiedKey() throws Exception {
    MapDataSourceLookup lookup = new MapDataSourceLookup();

    assertThatExceptionOfType(DataSourceLookupFailureException.class).isThrownBy(() ->
            lookup.getDataSource(DATA_SOURCE_NAME));
  }

}
