/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.transaction.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import javax.sql.DataSource;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.AdviceMode;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.config.AutoConfigurations;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.config.DataSourceBuilder;
import infra.jdbc.config.DataSourceTransactionManagerAutoConfiguration;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.ReactiveTransactionManager;
import infra.transaction.annotation.EnableTransactionManagement;
import infra.transaction.annotation.Transactional;
import infra.transaction.reactive.TransactionalOperator;
import infra.transaction.support.TransactionSynchronizationManager;
import infra.transaction.support.TransactionTemplate;

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
    this.contextRunner.run((context) -> Assertions.assertThat(context).doesNotHaveBean(TransactionTemplate.class));
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
    contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
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
            .run((context) -> Assertions.assertThat(context).doesNotHaveBean(TransactionTemplate.class));
  }

  @Test
  void whenThereAreSeveralReactiveTransactionManagersNoTransactionOperatorIsAutoConfigured() {
    this.contextRunner.withUserConfiguration(SeveralReactiveTransactionManagersConfiguration.class)
            .run((context) -> Assertions.assertThat(context).doesNotHaveBean(TransactionalOperator.class));
  }

  @Test
  void whenAUserProvidesATransactionTemplateTheAutoConfiguredTemplateBacksOff() {
    this.contextRunner.withUserConfiguration(CustomPlatformTransactionManagerConfiguration.class).run((context) -> {
      Assertions.assertThat(context).hasSingleBean(TransactionTemplate.class);
      Assertions.assertThat(context.getBean("transactionTemplateFoo")).isInstanceOf(TransactionTemplate.class);
    });
  }

  @Test
  void whenAUserProvidesATransactionalOperatorTheAutoConfiguredOperatorBacksOff() {
    this.contextRunner.withUserConfiguration(SingleReactiveTransactionManagerConfiguration.class,
            CustomTransactionalOperatorConfiguration.class).run((context) -> {
      Assertions.assertThat(context).hasSingleBean(TransactionalOperator.class);
      Assertions.assertThat(context.getBean("customTransactionalOperator"))
              .isInstanceOf(TransactionalOperator.class);
    });
  }

  @Test
  void transactionNotManagedWithNoTransactionManager() {
    this.contextRunner.withUserConfiguration(BaseConfiguration.class).run(
            (context) -> Assertions.assertThat(context.getBean(TransactionalService.class).isTransactionActive()).isFalse());
  }

  @Test
  void transactionManagerUsesCglibByDefault() {
    this.contextRunner.withUserConfiguration(PlatformTransactionManagersConfiguration.class).run((context) -> {
      Assertions.assertThat(context.getBean(AnotherServiceImpl.class).isTransactionActive()).isTrue();
      Assertions.assertThat(context.getBeansOfType(TransactionalServiceImpl.class)).hasSize(1);
    });
  }

  @Test
  void transactionManagerCanBeConfiguredToJdkProxy() {
    this.contextRunner.withUserConfiguration(PlatformTransactionManagersConfiguration.class)
            .withPropertyValues("infra.aop.proxy-target-class=false").run((context) -> {
              Assertions.assertThat(context.getBean(AnotherService.class).isTransactionActive()).isTrue();
              Assertions.assertThat(context).doesNotHaveBean(AnotherServiceImpl.class);
              Assertions.assertThat(context).doesNotHaveBean(TransactionalServiceImpl.class);
            });
  }

  @Test
  void customEnableTransactionManagementTakesPrecedence() {
    this.contextRunner
            .withUserConfiguration(CustomTransactionManagementConfiguration.class,
                    PlatformTransactionManagersConfiguration.class)
            .withPropertyValues("infra.aop.proxy-target-class=true").run((context) -> {
              Assertions.assertThat(context.getBean(AnotherService.class).isTransactionActive()).isTrue();
              Assertions.assertThat(context).doesNotHaveBean(AnotherServiceImpl.class);
              Assertions.assertThat(context).doesNotHaveBean(TransactionalServiceImpl.class);
            });
  }

  @Test
  void proxyTypeAspectJCausesRegistrationOfAnnotationTransactionAspect() {
    new AnnotationConfigApplicationContext(EnableAspectjTxConfig.class,
            TransactionAutoConfiguration.class);
  }

  @Configuration
  @EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
  static class EnableAspectjTxConfig {
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
