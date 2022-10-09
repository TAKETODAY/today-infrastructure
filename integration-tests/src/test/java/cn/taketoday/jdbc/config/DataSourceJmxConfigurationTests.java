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

package cn.taketoday.jdbc.config;

import com.zaxxer.hikari.HikariDataSource;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.jmx.ConnectionPool;
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

import cn.taketoday.annotation.config.jmx.JmxAutoConfiguration;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.jdbc.datasource.DelegatingDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceJmxConfiguration}.
 *
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
            .withPropertyValues("infra.jmx.enabled=true",
                    "datasource.type=" + HikariDataSource.class.getName(),
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
    this.contextRunner.withPropertyValues("datasource.type=" + HikariDataSource.class.getName(),
            "datasource.hikari.register-mbeans=true").run((context) -> {
      assertThat(context).hasSingleBean(HikariDataSource.class);
      HikariDataSource hikariDataSource = context.getBean(HikariDataSource.class);
      assertThat(hikariDataSource.isRegisterMbeans()).isTrue();
      // Ensure that the pool has been initialized, triggering MBean
      // registration
      hikariDataSource.getConnection().close();
      // We can't rely on the number of MBeans so we're checking that the
      // pool and pool config MBeans were registered
      assertThat(mBeanServer.queryMBeans(new ObjectName("com.zaxxer.hikari:type=*"), null).size())
              .isEqualTo(existingInstances.size() + 2);
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

  @Test
  void tomcatDoesNotExposeMBeanPoolByDefault() {
    this.contextRunner.withPropertyValues("datasource.type=" + DataSource.class.getName())
            .run((context) -> assertThat(context).doesNotHaveBean(ConnectionPool.class));
  }

  @Test
  void tomcatAutoConfiguredCanExposeMBeanPool() {
    this.contextRunner.withPropertyValues(
            "datasource.type=" + DataSource.class.getName(),
            "datasource.tomcat.jmx-enabled=true").run((context) -> {
      assertThat(context).hasBean("dataSourceMBean");
      assertThat(context).hasSingleBean(ConnectionPool.class);
      assertThat(context.getBean(DataSourceProxy.class).createPool().getJmxPool())
              .isSameAs(context.getBean(ConnectionPool.class));
    });
  }

  @Test
  void tomcatProxiedCanExposeMBeanPool() {
    this.contextRunner.withUserConfiguration(DataSourceProxyConfiguration.class)
            .withPropertyValues("datasource.type=" + DataSource.class.getName(),
                    "datasource.tomcat.jmx-enabled=true")
            .run((context) -> {
              assertThat(context).hasBean("dataSourceMBean");
              assertThat(context).getBean("dataSourceMBean").isInstanceOf(ConnectionPool.class);
            });
  }

  @Test
  void tomcatDelegateCanExposeMBeanPool() {
    this.contextRunner.withUserConfiguration(DataSourceDelegateConfiguration.class)
            .withPropertyValues("datasource.type=" + DataSource.class.getName(),
                    "datasource.tomcat.jmx-enabled=true")
            .run((context) -> {
              assertThat(context).hasBean("dataSourceMBean");
              assertThat(context).getBean("dataSourceMBean").isInstanceOf(ConnectionPool.class);
            });
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
