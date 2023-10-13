/*
 * Copyright 2017 - 2023 the original author or authors.
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
    ThreadLocal<String> lookupKey = new ThreadLocal<>();
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
    routingDataSource.setTargetDataSources(Map.of("ds1", ds1, "ds2", "dataSource2"));
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
    routingDataSource.setTargetDataSources(Map.of("ds1", 1));
    assertThatIllegalArgumentException().isThrownBy(routingDataSource::afterPropertiesSet)
            .withMessage("Illegal data source value - only [javax.sql.DataSource] and String supported: 1");
  }

  @Test
  void setDefaultTargetDataSource() {
    ThreadLocal<String> lookupKey = new ThreadLocal<>();
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return lookupKey.get();
      }
    };
    DataSource ds = new StubDataSource();
    routingDataSource.setTargetDataSources(Map.of());
    routingDataSource.setDefaultTargetDataSource(ds);
    routingDataSource.afterPropertiesSet();
    lookupKey.set("foo");
    assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds);
  }

  @Test
  void setDefaultTargetDataSourceFallbackIsFalse() {
    ThreadLocal<String> lookupKey = new ThreadLocal<>();
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return lookupKey.get();
      }
    };
    DataSource ds = new StubDataSource();
    routingDataSource.setTargetDataSources(Map.of());
    routingDataSource.setDefaultTargetDataSource(ds);
    routingDataSource.setLenientFallback(false);
    routingDataSource.afterPropertiesSet();
    lookupKey.set("foo");
    assertThatIllegalStateException().isThrownBy(routingDataSource::determineTargetDataSource)
            .withMessage("Cannot determine target DataSource for lookup key [foo]");
  }

  @Test
  void setDefaultTargetDataSourceLookupKeyIsNullWhenFallbackIsFalse() {
    ThreadLocal<String> lookupKey = new ThreadLocal<>();
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return lookupKey.get();
      }
    };
    DataSource ds = new StubDataSource();
    routingDataSource.setTargetDataSources(Map.of());
    routingDataSource.setDefaultTargetDataSource(ds);
    routingDataSource.setLenientFallback(false);
    routingDataSource.afterPropertiesSet();
    lookupKey.set(null);
    assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds);
  }

  @Test
  void initializeSynchronizesTargetDataSourcesToResolvedDataSources() {
    AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
      @Override
      protected Object determineCurrentLookupKey() {
        return null;
      }
    };

    DataSource ds1 = new StubDataSource();
    DataSource ds2 = new StubDataSource();

    routingDataSource.setTargetDataSources(Map.of("ds1", ds1, "ds2", ds2));
    routingDataSource.initialize();

    Map<Object, DataSource> resolvedDataSources = routingDataSource.getResolvedDataSources();
    assertThat(resolvedDataSources).hasSize(2);
    assertThat(resolvedDataSources.get("ds1")).isSameAs(ds1);
    assertThat(resolvedDataSources.get("ds2")).isSameAs(ds2);
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
