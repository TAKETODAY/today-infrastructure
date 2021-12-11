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

package cn.taketoday.jdbc.datasource.lookup;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AbstractRoutingDataSource}.
 *
 * @author Kazuki Shimizu
 */
class AbstractRoutingDataSourceTests {

  @Test
  void setTargetDataSources() {
    final ThreadLocal<String> lookupKey = new ThreadLocal<>();
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return lookupKey.get();
      }
    };
    DataSource ds1 = new StubDataSource();
    DataSource ds2 = new StubDataSource();

    MapDataSourceLookup dataSourceLookup = new MapDataSourceLookup();
    dataSourceLookup.addDataSource("dataSource2", ds2);
    routingDataSource.setDataSourceLookup(dataSourceLookup);

    Map<Object, Object> targetDataSources = new HashMap<>();
    targetDataSources.put("ds1", ds1);
    targetDataSources.put("ds2", "dataSource2");
    routingDataSource.setTargetDataSources(targetDataSources);

    routingDataSource.afterPropertiesSet();
    lookupKey.set("ds1");
    assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds1);
    lookupKey.set("ds2");
    assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds2);
  }

  @Test
  void targetDataSourcesIsNull() {
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return null;
      }
    };
    assertThatIllegalArgumentException().isThrownBy(routingDataSource::afterPropertiesSet)
            .withMessage("Property 'targetDataSources' is required");
  }

  @Test
  void dataSourceIsUnSupportedType() {
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return null;
      }
    };
    Map<Object, Object> targetDataSources = new HashMap<>();
    targetDataSources.put("ds1", 1);
    routingDataSource.setTargetDataSources(targetDataSources);
    assertThatIllegalArgumentException().isThrownBy(routingDataSource::afterPropertiesSet)
            .withMessage("Illegal data source value - only [javax.sql.DataSource] and String supported: 1");
  }

  @Test
  void setDefaultTargetDataSource() {
    final ThreadLocal<String> lookupKey = new ThreadLocal<>();
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return lookupKey.get();
      }
    };
    DataSource ds = new StubDataSource();
    routingDataSource.setTargetDataSources(new HashMap<>());
    routingDataSource.setDefaultTargetDataSource(ds);
    routingDataSource.afterPropertiesSet();
    lookupKey.set("foo");
    assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds);
  }

  @Test
  void setDefaultTargetDataSourceFallbackIsFalse() {
    final ThreadLocal<String> lookupKey = new ThreadLocal<>();
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return lookupKey.get();
      }
    };
    DataSource ds = new StubDataSource();
    routingDataSource.setTargetDataSources(new HashMap<>());
    routingDataSource.setDefaultTargetDataSource(ds);
    routingDataSource.setLenientFallback(false);
    routingDataSource.afterPropertiesSet();
    lookupKey.set("foo");
    assertThatIllegalStateException().isThrownBy(routingDataSource::determineTargetDataSource)
            .withMessage("Cannot determine target DataSource for lookup key [foo]");
  }

  @Test
  void setDefaultTargetDataSourceLookupKeyIsNullWhenFallbackIsFalse() {
    final ThreadLocal<String> lookupKey = new ThreadLocal<>();
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return lookupKey.get();
      }
    };
    DataSource ds = new StubDataSource();
    routingDataSource.setTargetDataSources(new HashMap<>());
    routingDataSource.setDefaultTargetDataSource(ds);
    routingDataSource.setLenientFallback(false);
    routingDataSource.afterPropertiesSet();
    lookupKey.set(null);
    assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds);
  }

  @Test
  public void notInitialized() {
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return null;
      }
    };
    assertThatIllegalArgumentException().isThrownBy(routingDataSource::determineTargetDataSource)
            .withMessage("DataSource router not initialized");
  }

}
