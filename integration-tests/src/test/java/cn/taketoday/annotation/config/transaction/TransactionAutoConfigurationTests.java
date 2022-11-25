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

package cn.taketoday.annotation.config.transaction;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.jdbc.DataSourceAutoConfiguration;
import cn.taketoday.annotation.config.jdbc.DataSourceTransactionManagerAutoConfiguration;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.jdbc.config.DataSourceBuilder;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.ReactiveTransactionManager;
import cn.taketoday.transaction.annotation.EnableTransactionManagement;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.transaction.reactive.TransactionalOperator;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransactionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class TransactionAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(TransactionAutoConfiguration.class));

  @Test
  void whenThereIsNoPlatformTransactionManagerNoTransactionTemplateIsAutoConfigured() {
    this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(TransactionTemplate.class));
  }

  @Test
  void whenThereIsASinglePlatformTransactionManagerATransactionTemplateIsAutoConfigured() {
    this.contextRunner.withUserConfiguration(SinglePlatformTransactionManagerConfiguration.class).run((context) -> {
      PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
      TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);
      assertThat(transactionTemplate.getTransactionManager()).isSameAs(transactionManager);
    });
  }

  @Test
  void whenThereIsASingleReactiveTransactionManagerATransactionalOperatorIsAutoConfigured() {
    this.contextRunner.withUserConfiguration(SingleReactiveTransactionManagerConfiguration.class).run((context) -> {
      ReactiveTransactionManager transactionManager = context.getBean(ReactiveTransactionManager.class);
      TransactionalOperator transactionalOperator = context.getBean(TransactionalOperator.class);
      assertThat(transactionalOperator).extracting("transactionManager").isSameAs(transactionManager);
    });
  }

  @Test
  void whenThereAreBothReactiveAndPlatformTransactionManagersATemplateAndAnOperatorAreAutoConfigured() {
    contextRunner.withConfiguration(
                    AutoConfigurations.of(DataSourceAutoConfiguration.class,
                            DataSourceTransactionManagerAutoConfiguration.class))
            .withUserConfiguration(SinglePlatformTransactionManagerConfiguration.class,
                    SingleReactiveTransactionManagerConfiguration.class)
            .withPropertyValues("datasource.url:jdbc:h2:mem:" + UUID.randomUUID()).run((context) -> {
              var platformTransactionManager = context.getBean(PlatformTransactionManager.class);
              TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);
              assertThat(transactionTemplate.getTransactionManager()).isSameAs(platformTransactionManager);
              ReactiveTransactionManager reactiveTransactionManager = context
                      .getBean(ReactiveTransactionManager.class);
              TransactionalOperator transactionalOperator = context.getBean(TransactionalOperator.class);
              assertThat(transactionalOperator).extracting("transactionManager")
                      .isSameAs(reactiveTransactionManager);
            });
  }

  @Test
  void whenThereAreSeveralPlatformTransactionManagersNoTransactionTemplateIsAutoConfigured() {
    this.contextRunner.withUserConfiguration(SeveralPlatformTransactionManagersConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean(TransactionTemplate.class));
  }

  @Test
  void whenThereAreSeveralReactiveTransactionManagersNoTransactionOperatorIsAutoConfigured() {
    this.contextRunner.withUserConfiguration(SeveralReactiveTransactionManagersConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean(TransactionalOperator.class));
  }

  @Test
  void whenAUserProvidesATransactionTemplateTheAutoConfiguredTemplateBacksOff() {
    this.contextRunner.withUserConfiguration(CustomPlatformTransactionManagerConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(TransactionTemplate.class);
      assertThat(context.getBean("transactionTemplateFoo")).isInstanceOf(TransactionTemplate.class);
    });
  }

  @Test
  void whenAUserProvidesATransactionalOperatorTheAutoConfiguredOperatorBacksOff() {
    this.contextRunner.withUserConfiguration(SingleReactiveTransactionManagerConfiguration.class,
            CustomTransactionalOperatorConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(TransactionalOperator.class);
      assertThat(context.getBean("customTransactionalOperator"))
              .isInstanceOf(TransactionalOperator.class);
    });
  }

  @Test
  void platformTransactionManagerCustomizers() {
    this.contextRunner.withUserConfiguration(SeveralPlatformTransactionManagersConfiguration.class)
            .run((context) -> {
              TransactionManagerCustomizers customizers = context.getBean(TransactionManagerCustomizers.class);
              assertThat(customizers).extracting("customizers").asList().singleElement()
                      .isInstanceOf(TransactionProperties.class);
            });
  }

  @Test
  void transactionNotManagedWithNoTransactionManager() {
    this.contextRunner.withUserConfiguration(BaseConfiguration.class).run(
            (context) -> assertThat(context.getBean(TransactionalService.class).isTransactionActive()).isFalse());
  }

  @Test
  void transactionManagerUsesCglibByDefault() {
    this.contextRunner.withUserConfiguration(PlatformTransactionManagersConfiguration.class).run((context) -> {
      assertThat(context.getBean(AnotherServiceImpl.class).isTransactionActive()).isTrue();
      assertThat(context.getBeansOfType(TransactionalServiceImpl.class)).hasSize(1);
    });
  }

  @Test
  void transactionManagerCanBeConfiguredToJdkProxy() {
    this.contextRunner.withUserConfiguration(PlatformTransactionManagersConfiguration.class)
            .withPropertyValues("infra.aop.proxy-target-class=false").run((context) -> {
              assertThat(context.getBean(AnotherService.class).isTransactionActive()).isTrue();
              assertThat(context).doesNotHaveBean(AnotherServiceImpl.class);
              assertThat(context).doesNotHaveBean(TransactionalServiceImpl.class);
            });
  }

  @Test
  void customEnableTransactionManagementTakesPrecedence() {
    this.contextRunner
            .withUserConfiguration(CustomTransactionManagementConfiguration.class,
                    PlatformTransactionManagersConfiguration.class)
            .withPropertyValues("infra.aop.proxy-target-class=true").run((context) -> {
              assertThat(context.getBean(AnotherService.class).isTransactionActive()).isTrue();
              assertThat(context).doesNotHaveBean(AnotherServiceImpl.class);
              assertThat(context).doesNotHaveBean(TransactionalServiceImpl.class);
            });
  }

  @Configuration
  static class SinglePlatformTransactionManagerConfiguration {

    @Bean
    PlatformTransactionManager transactionManager() {
      return mock(PlatformTransactionManager.class);
    }

  }

  @Configuration
  static class SingleReactiveTransactionManagerConfiguration {

    @Bean
    ReactiveTransactionManager reactiveTransactionManager() {
      return mock(ReactiveTransactionManager.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SeveralPlatformTransactionManagersConfiguration {

    @Bean
    PlatformTransactionManager transactionManagerOne() {
      return mock(PlatformTransactionManager.class);
    }

    @Bean
    PlatformTransactionManager transactionManagerTwo() {
      return mock(PlatformTransactionManager.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SeveralReactiveTransactionManagersConfiguration {

    @Bean
    ReactiveTransactionManager reactiveTransactionManager1() {
      return mock(ReactiveTransactionManager.class);
    }

    @Bean
    ReactiveTransactionManager reactiveTransactionManager2() {
      return mock(ReactiveTransactionManager.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomPlatformTransactionManagerConfiguration {

    @Bean
    TransactionTemplate transactionTemplateFoo(PlatformTransactionManager transactionManager) {
      return new TransactionTemplate(transactionManager);
    }

    @Bean
    PlatformTransactionManager transactionManagerFoo() {
      return mock(PlatformTransactionManager.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomTransactionalOperatorConfiguration {

    @Bean
    TransactionalOperator customTransactionalOperator() {
      return mock(TransactionalOperator.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class BaseConfiguration {

    @Bean
    TransactionalService transactionalService() {
      return new TransactionalServiceImpl();
    }

    @Bean
    AnotherServiceImpl anotherService() {
      return new AnotherServiceImpl();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(BaseConfiguration.class)
  static class PlatformTransactionManagersConfiguration {

    @Bean
    DataSourceTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    DataSource dataSource() {
      return DataSourceBuilder.create().driverClassName("org.hsqldb.jdbc.JDBCDriver").url("jdbc:hsqldb:mem:tx")
              .username("sa").build();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableTransactionManagement(proxyTargetClass = false)
  static class CustomTransactionManagementConfiguration {

  }

  interface TransactionalService {

    @Transactional
    boolean isTransactionActive();

  }

  static class TransactionalServiceImpl implements TransactionalService {

    @Override
    public boolean isTransactionActive() {
      return TransactionSynchronizationManager.isActualTransactionActive();
    }

  }

  interface AnotherService {

    boolean isTransactionActive();

  }

  static class AnotherServiceImpl implements AnotherService {

    @Override
    @Transactional
    public boolean isTransactionActive() {
      return TransactionSynchronizationManager.isActualTransactionActive();
    }

  }

}
