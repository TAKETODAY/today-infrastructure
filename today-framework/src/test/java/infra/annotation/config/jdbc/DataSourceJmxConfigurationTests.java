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

package infra.annotation.config.jdbc;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.UUID;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import infra.annotation.config.jmx.JmxAutoConfiguration;
import infra.aop.framework.ProxyFactory;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.jdbc.datasource.DelegatingDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 * @author Tadaya Tsuyukubo
 */
@Execution(ExecutionMode.SAME_THREAD)
class DataSourceJmxConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withPropertyValues("datasource.url=jdbc:hsqldb:mem:test-" + UUID.randomUUID())
          .withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class, DataSourceAutoConfiguration.class));

  @Test
  void hikariAutoConfiguredCanUseRegisterMBeans() {
    String poolName = UUID.randomUUID().toString();
    this.contextRunner
            .withPropertyValues("infra.jmx.enabled=true", "datasource.type=" + HikariDataSource.class.getName(),
                    "datasource.name=" + poolName, "datasource.hikari.register-mbeans=true")
            .run((context) -> {
              assertThat(context).hasSingleBean(HikariDataSource.class);
              HikariDataSource hikariDataSource = context.getBean(HikariDataSource.class);
              assertThat(hikariDataSource.isRegisterMbeans()).isTrue();
              // Ensure that the pool has been initialized, triggering MBean
              // registration
              hikariDataSource.getConnection().close();
              MBeanServer mBeanServer = context.getBean(MBeanServer.class);
              validateHikariMBeansRegistration(mBeanServer, poolName, true);
            });
  }

  @Test
  void hikariAutoConfiguredWithoutDataSourceName() throws MalformedObjectNameException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    Set<ObjectInstance> existingInstances = mBeanServer.queryMBeans(new ObjectName("com.zaxxer.hikari:type=*"),
            null);
    this.contextRunner.withPropertyValues(
            "datasource.type=" + HikariDataSource.class.getName(),
            "datasource.hikari.register-mbeans=true").run((context) -> {
      assertThat(context).hasSingleBean(HikariDataSource.class);
      HikariDataSource hikariDataSource = context.getBean(HikariDataSource.class);
      assertThat(hikariDataSource.isRegisterMbeans()).isTrue();
      // Ensure that the pool has been initialized, triggering MBean
      // registration
      hikariDataSource.getConnection().close();
      // We can't rely on the number of MBeans so we're checking that the
      // pool and pool config MBeans were registered
      assertThat(mBeanServer.queryMBeans(new ObjectName("com.zaxxer.hikari:type=*"), null))
              .hasSize(existingInstances.size() + 2);
    });
  }

  @Test
  void hikariAutoConfiguredUsesJmxFlag() {
    String poolName = UUID.randomUUID().toString();
    this.contextRunner.withPropertyValues("datasource.type=" + HikariDataSource.class.getName(),
            "infra.jmx.enabled=false", "datasource.name=" + poolName,
            "datasource.hikari.register-mbeans=true").run((context) -> {
      assertThat(context).hasSingleBean(HikariDataSource.class);
      HikariDataSource hikariDataSource = context.getBean(HikariDataSource.class);
      assertThat(hikariDataSource.isRegisterMbeans()).isTrue();
      // Ensure that the pool has been initialized, triggering MBean
      // registration
      hikariDataSource.getConnection().close();
      // Hikari can still register mBeans
      validateHikariMBeansRegistration(ManagementFactory.getPlatformMBeanServer(), poolName, true);
    });
  }

  @Test
  void hikariProxiedCanUseRegisterMBeans() {
    String poolName = UUID.randomUUID().toString();
    this.contextRunner.withUserConfiguration(DataSourceProxyConfiguration.class)
            .withPropertyValues("infra.jmx.enabled=true",
                    "datasource.type=" + HikariDataSource.class.getName(),
                    "datasource.name=" + poolName, "datasource.hikari.register-mbeans=true")
            .run((context) -> {
              assertThat(context).hasSingleBean(javax.sql.DataSource.class);
              HikariDataSource hikariDataSource = context.getBean(javax.sql.DataSource.class)
                      .unwrap(HikariDataSource.class);
              assertThat(hikariDataSource.isRegisterMbeans()).isTrue();
              // Ensure that the pool has been initialized, triggering MBean
              // registration
              hikariDataSource.getConnection().close();
              MBeanServer mBeanServer = context.getBean(MBeanServer.class);
              validateHikariMBeansRegistration(mBeanServer, poolName, true);
            });
  }

  private void validateHikariMBeansRegistration(MBeanServer mBeanServer, String poolName, boolean expected)
          throws MalformedObjectNameException {
    assertThat(mBeanServer.isRegistered(new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")")))
            .isEqualTo(expected);
    assertThat(mBeanServer.isRegistered(new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + poolName + ")")))
            .isEqualTo(expected);
  }

  @Configuration(proxyBeanMethods = false)
  static class DataSourceProxyConfiguration {

    @Bean
    static DataSourceBeanPostProcessor dataSourceBeanPostProcessor() {
      return new DataSourceBeanPostProcessor();
    }

  }

  static class DataSourceBeanPostProcessor implements InitializationBeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
      if (bean instanceof javax.sql.DataSource) {
        return new ProxyFactory(bean).getProxy();
      }
      return bean;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DataSourceDelegateConfiguration {

    @Bean
    static DataSourceBeanPostProcessor dataSourceBeanPostProcessor() {
      return new DataSourceBeanPostProcessor() {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
          return (bean instanceof javax.sql.DataSource)
                  ? new DelegatingDataSource((javax.sql.DataSource) bean) : bean;
        }
      };
    }

  }

}
