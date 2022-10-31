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

package cn.taketoday.annotation.config.jdbc;

import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.interceptor.SlowQueryReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.EnableMBeanExport;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.jdbc.config.DataSourceBuilder;
import cn.taketoday.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link TomcatDataSourceConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class TomcatDataSourceConfigurationTests {

  private static final String PREFIX = "datasource.tomcat.";

  private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @BeforeEach
  void init() {
    TestPropertyValues.of(PREFIX + "initialize:false").applyTo(this.context);
  }

  @Test
  void testDataSourceExists() {
    this.context.register(TomcatDataSourceConfiguration.class);
    TestPropertyValues.of(PREFIX + "url:jdbc:h2:mem:testdb").applyTo(this.context);
    this.context.refresh();
    assertThat(this.context.getBean(DataSource.class)).isNotNull();
    assertThat(this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class)).isNotNull();
  }

  @Test
  void testDataSourcePropertiesOverridden() throws Exception {
    this.context.register(TomcatDataSourceConfiguration.class);
    TestPropertyValues
            .of(PREFIX + "url:jdbc:h2:mem:testdb", PREFIX + "testWhileIdle:true", PREFIX + "testOnBorrow:true",
                    PREFIX + "testOnReturn:true", PREFIX + "timeBetweenEvictionRunsMillis:10000",
                    PREFIX + "minEvictableIdleTimeMillis:12345", PREFIX + "maxWait:1234",
                    PREFIX + "jdbcInterceptors:SlowQueryReport", PREFIX + "validationInterval:9999")
            .applyTo(this.context);
    this.context.refresh();
    org.apache.tomcat.jdbc.pool.DataSource ds = this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
    assertThat(ds.getUrl()).isEqualTo("jdbc:h2:mem:testdb");
    assertThat(ds.isTestWhileIdle()).isTrue();
    assertThat(ds.isTestOnBorrow()).isTrue();
    assertThat(ds.isTestOnReturn()).isTrue();
    assertThat(ds.getTimeBetweenEvictionRunsMillis()).isEqualTo(10000);
    assertThat(ds.getMinEvictableIdleTimeMillis()).isEqualTo(12345);
    assertThat(ds.getMaxWait()).isEqualTo(1234);
    assertThat(ds.getValidationInterval()).isEqualTo(9999L);
    assertDataSourceHasInterceptors(ds);
  }

  private void assertDataSourceHasInterceptors(DataSourceProxy ds) throws ClassNotFoundException {
    PoolProperties.InterceptorDefinition[] interceptors = ds.getJdbcInterceptorsAsArray();
    for (PoolProperties.InterceptorDefinition interceptor : interceptors) {
      if (SlowQueryReport.class == interceptor.getInterceptorClass()) {
        return;
      }
    }
    fail("SlowQueryReport interceptor should have been set.");
  }

  @Test
  void testDataSourceDefaultsPreserved() {
    this.context.register(TomcatDataSourceConfiguration.class);
    TestPropertyValues.of(PREFIX + "url:jdbc:h2:mem:testdb").applyTo(this.context);
    this.context.refresh();
    org.apache.tomcat.jdbc.pool.DataSource ds = this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
    assertThat(ds.getTimeBetweenEvictionRunsMillis()).isEqualTo(5000);
    assertThat(ds.getMinEvictableIdleTimeMillis()).isEqualTo(60000);
    assertThat(ds.getMaxWait()).isEqualTo(30000);
    assertThat(ds.getValidationInterval()).isEqualTo(3000L);
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties
  @EnableMBeanExport
  static class TomcatDataSourceConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "datasource.tomcat")
    DataSource dataSource() {
      return DataSourceBuilder.create().type(org.apache.tomcat.jdbc.pool.DataSource.class).build();
    }

  }

}
