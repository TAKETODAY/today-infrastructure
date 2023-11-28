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

package cn.taketoday.annotation.config.jdbc;

import com.zaxxer.hikari.HikariDataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.FilteredClassLoader;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.jdbc.config.DatabaseDriver;
import cn.taketoday.jdbc.datasource.SimpleDriverDataSource;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabase;
import cn.taketoday.util.StringUtils;
import oracle.ucp.jdbc.PoolDataSourceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class DataSourceAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
          .withPropertyValues("datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());

  @Test
  void testDefaultDataSourceExists() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DataSource.class));
  }

  @Test
  void testDataSourceHasEmbeddedDefault() {
    this.contextRunner.run((context) -> {
      HikariDataSource dataSource = context.getBean(HikariDataSource.class);
      assertThat(dataSource.getJdbcUrl()).isNotNull();
      assertThat(dataSource.getDriverClassName()).isNotNull();
    });
  }

  @Test
  void testBadUrl() {
    this.contextRunner.withPropertyValues("datasource.url:jdbc:not-going-to-work")
            .withClassLoader(new DisableEmbeddedDatabaseClassLoader())
            .run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class));
  }

  @Test
  void testBadDriverClass() {
    this.contextRunner.withPropertyValues("datasource.driverClassName:org.none.jdbcDriver")
            .run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class)
                    .hasMessageContaining("org.none.jdbcDriver"));
  }

  @Test
  void hikariValidatesConnectionByDefault() {
    assertDataSource(HikariDataSource.class, Collections.singletonList("org.apache.tomcat"), (dataSource) ->
            // Use Connection#isValid()
            assertThat(dataSource.getConnectionTestQuery()).isNull());
  }

  @Test
  void tomcatIsFallback() {
    assertDataSource(org.apache.tomcat.jdbc.pool.DataSource.class, Collections.singletonList("com.zaxxer.hikari"),
            (dataSource) -> assertThat(dataSource.getUrl()).startsWith("jdbc:hsqldb:mem:testdb"));
  }

  @Test
  void tomcatValidatesConnectionByDefault() {
    assertDataSource(org.apache.tomcat.jdbc.pool.DataSource.class, Collections.singletonList("com.zaxxer.hikari"),
            (dataSource) -> {
              assertThat(dataSource.isTestOnBorrow()).isTrue();
              assertThat(dataSource.getValidationQuery()).isEqualTo(DatabaseDriver.HSQLDB.getValidationQuery());
            });
  }

  @Test
  void commonsDbcp2IsFallback() {
    assertDataSource(BasicDataSource.class, Arrays.asList("com.zaxxer.hikari", "org.apache.tomcat"),
            (dataSource) -> assertThat(dataSource.getUrl()).startsWith("jdbc:hsqldb:mem:testdb"));
  }

  @Test
  void commonsDbcp2ValidatesConnectionByDefault() {
    assertDataSource(org.apache.commons.dbcp2.BasicDataSource.class,
            Arrays.asList("com.zaxxer.hikari", "org.apache.tomcat"), (dataSource) -> {
              assertThat(dataSource.getTestOnBorrow()).isTrue();
              // Use Connection#isValid()
              assertThat(dataSource.getValidationQuery()).isNull();
            });
  }

  @Test
  void oracleUcpIsFallback() {
    assertDataSource(PoolDataSourceImpl.class,
            Arrays.asList("com.zaxxer.hikari", "org.apache.tomcat", "org.apache.commons.dbcp2"),
            (dataSource) -> assertThat(dataSource.getURL()).startsWith("jdbc:hsqldb:mem:testdb"));
  }

  @Test
  void oracleUcpDoesNotValidateConnectionByDefault() {
    assertDataSource(PoolDataSourceImpl.class,
            Arrays.asList("com.zaxxer.hikari", "org.apache.tomcat", "org.apache.commons.dbcp2"), (dataSource) -> {
              assertThat(dataSource.getValidateConnectionOnBorrow()).isFalse();
              // Use an internal ping when using an Oracle JDBC driver
              assertThat(dataSource.getSQLForValidateConnection()).isNull();
            });
  }

  @Test
  @SuppressWarnings("resource")
  void testEmbeddedTypeDefaultsUsername() {
    this.contextRunner.withPropertyValues("datasource.driverClassName:org.hsqldb.jdbcDriver",
            "datasource.url:jdbc:hsqldb:mem:testdb").run((context) -> {
      DataSource bean = context.getBean(DataSource.class);
      HikariDataSource pool = (HikariDataSource) bean;
      assertThat(pool.getDriverClassName()).isEqualTo("org.hsqldb.jdbcDriver");
      assertThat(pool.getUsername()).isEqualTo("sa");
    });
  }

  @Test
  void dataSourceWhenNoConnectionPoolsAreAvailableWithUrlDoesNotCreateDataSource() {
    this.contextRunner.with(hideConnectionPools())
            .withPropertyValues("datasource.url:jdbc:hsqldb:mem:testdb")
            .run((context) -> assertThat(context).doesNotHaveBean(DataSource.class));
  }

  /**
   * This test makes sure that if no supported data source is present, a datasource is
   * still created if "datasource.type" is present.
   */
  @Test
  void dataSourceWhenNoConnectionPoolsAreAvailableWithUrlAndTypeCreatesDataSource() {
    this.contextRunner.with(hideConnectionPools())
            .withPropertyValues("datasource.driverClassName:org.hsqldb.jdbcDriver",
                    "datasource.url:jdbc:hsqldb:mem:testdb",
                    "datasource.type:" + SimpleDriverDataSource.class.getName())
            .run(this::containsOnlySimpleDriverDataSource);
  }

  @Test
  void explicitTypeSupportedDataSource() {
    this.contextRunner
            .withPropertyValues("datasource.driverClassName:org.hsqldb.jdbcDriver",
                    "datasource.url:jdbc:hsqldb:mem:testdb",
                    "datasource.type:" + SimpleDriverDataSource.class.getName())
            .run(this::containsOnlySimpleDriverDataSource);
  }

  private void containsOnlySimpleDriverDataSource(AssertableApplicationContext context) {
    assertThat(context).hasSingleBean(DataSource.class);
    assertThat(context).getBean(DataSource.class).isExactlyInstanceOf(SimpleDriverDataSource.class);
  }

  @Test
  void testExplicitDriverClassClearsUsername() {
    this.contextRunner.withPropertyValues("datasource.driverClassName:" + DatabaseTestDriver.class.getName(),
            "datasource.url:jdbc:foo://localhost").run((context) -> {
      assertThat(context).hasSingleBean(DataSource.class);
      HikariDataSource dataSource = context.getBean(HikariDataSource.class);
      assertThat(dataSource.getDriverClassName()).isEqualTo(DatabaseTestDriver.class.getName());
      assertThat(dataSource.getUsername()).isNull();
    });
  }

  @Test
  void testDefaultDataSourceCanBeOverridden() {
    this.contextRunner.withUserConfiguration(TestDataSourceConfiguration.class)
            .run((context) -> assertThat(context).getBean(DataSource.class).isInstanceOf(BasicDataSource.class));
  }

  @Test
  void whenThereIsAUserProvidedDataSourceAnUnresolvablePlaceholderDoesNotCauseAProblem() {
    this.contextRunner.withUserConfiguration(TestDataSourceConfiguration.class)
            .withPropertyValues("datasource.url:${UNRESOLVABLE_PLACEHOLDER}")
            .run((context) -> assertThat(context).getBean(DataSource.class).isInstanceOf(BasicDataSource.class));
  }

  @Test
  void whenThereIsAnEmptyUserProvidedDataSource() {
    this.contextRunner.with(hideConnectionPools()).withPropertyValues("datasource.url:")
            .run((context) -> assertThat(context).getBean(DataSource.class).isInstanceOf(EmbeddedDatabase.class));
  }

  private static Function<ApplicationContextRunner, ApplicationContextRunner> hideConnectionPools() {
    return (runner) -> runner.withClassLoader(new FilteredClassLoader("org.apache.tomcat", "com.zaxxer.hikari",
            "org.apache.commons.dbcp2", "oracle.ucp.jdbc", "com.mchange"));
  }

  private <T extends DataSource> void assertDataSource(Class<T> expectedType, List<String> hiddenPackages,
          Consumer<T> consumer) {
    FilteredClassLoader classLoader = new FilteredClassLoader(StringUtils.toStringArray(hiddenPackages));
    this.contextRunner.withClassLoader(classLoader).run((context) -> {
      DataSource bean = context.getBean(DataSource.class);
      assertThat(bean).isInstanceOf(expectedType);
      consumer.accept(expectedType.cast(bean));
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class TestDataSourceConfiguration {

    private BasicDataSource pool;

    @Bean
    DataSource dataSource() {
      this.pool = new BasicDataSource();
      this.pool.setDriverClassName("org.hsqldb.jdbcDriver");
      this.pool.setUrl("jdbc:hsqldb:mem:overridedb");
      this.pool.setUsername("sa");
      return this.pool;
    }

  }

  // see testExplicitDriverClassClearsUsername
  public static class DatabaseTestDriver implements Driver {

    @Override
    public Connection connect(String url, Properties info) {
      return mock(Connection.class);
    }

    @Override
    public boolean acceptsURL(String url) {
      return true;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
      return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
      return 1;
    }

    @Override
    public int getMinorVersion() {
      return 0;
    }

    @Override
    public boolean jdbcCompliant() {
      return false;
    }

    @Override
    public Logger getParentLogger() {
      return mock(Logger.class);
    }

  }

  static class DisableEmbeddedDatabaseClassLoader extends URLClassLoader {

    DisableEmbeddedDatabaseClassLoader() {
      super(new URL[0], DisableEmbeddedDatabaseClassLoader.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      for (EmbeddedDatabaseConnection candidate : EmbeddedDatabaseConnection.values()) {
        if (name.equals(candidate.getDriverClassName())) {
          throw new ClassNotFoundException();
        }
      }
      return super.loadClass(name, resolve);
    }

  }

}
