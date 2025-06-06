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

package infra.cache.annotation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import infra.aop.framework.Advised;
import infra.aop.support.AopUtils;
import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.cache.concurrent.ConcurrentMapCache;
import infra.cache.support.SimpleCacheManager;
import infra.context.annotation.AdviceMode;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.ImportResource;
import infra.core.NestedRuntimeException;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseType;
import infra.stereotype.Repository;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.annotation.EnableTransactionManagement;
import infra.transaction.annotation.TransactionManagementConfigurer;
import infra.transaction.annotation.Transactional;
import infra.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import infra.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for the @EnableTransactionManagement annotation.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("resource")
class EnableTransactionManagementIntegrationTests {

  @Test
  void repositoryIsNotTxProxy() {
    var ctx = new AnnotationConfigApplicationContext(Config.class);

    assertThat(isTxProxy(ctx.getBean(FooRepository.class))).isFalse();
  }

  @Test
  void repositoryIsTxProxy_withDefaultTxManagerName() {
    var ctx = new AnnotationConfigApplicationContext(Config.class, DefaultTxManagerNameConfig.class);

    assertTxProxying(ctx);
  }

  @Test
  void repositoryIsTxProxy_withCustomTxManagerName() {
    var ctx = new AnnotationConfigApplicationContext(Config.class, CustomTxManagerNameConfig.class);

    assertTxProxying(ctx);
  }

  @Test
  void repositoryIsTxProxy_withNonConventionalTxManagerName_fallsBackToByTypeLookup() {
    assertTxProxying(new AnnotationConfigApplicationContext(Config.class, NonConventionalTxManagerNameConfig.class));
  }

  @Test
  void repositoryIsClassBasedTxProxy() {
    var ctx = new AnnotationConfigApplicationContext(Config.class, ProxyTargetClassTxConfig.class);

    assertTxProxying(ctx);
    assertThat(AopUtils.isCglibProxy(ctx.getBean(FooRepository.class))).isTrue();
  }

  @Test
  void repositoryUsesAspectJAdviceMode() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class, AspectJTxConfig.class);
    // this test is a bit fragile, but gets the job done, proving that an
    // attempt was made to look up the AJ aspect. It's due to classpath issues
    // in .integration-tests that it's not found.

    assertThatExceptionOfType(NestedRuntimeException.class)
            .isThrownBy(ctx::refresh)
            .satisfies(ex -> {
              assertThat(ex.getNestedMessage()).contains("AspectJJtaTransactionManagementConfiguration");
            });
  }

  @Test
  void implicitTxManager() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImplicitTxManagerConfig.class);

    FooRepository fooRepository = ctx.getBean(FooRepository.class);
    fooRepository.findAll();

    CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);
    assertThat(txManager.begun).isEqualTo(1);
    assertThat(txManager.commits).isEqualTo(1);
    assertThat(txManager.rollbacks).isEqualTo(0);
  }

  @Test
  void explicitTxManager() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ExplicitTxManagerConfig.class);

    FooRepository fooRepository = ctx.getBean(FooRepository.class);
    fooRepository.findAll();

    CallCountingTransactionManager txManager1 = ctx.getBean("txManager1", CallCountingTransactionManager.class);
    assertThat(txManager1.begun).isEqualTo(1);
    assertThat(txManager1.commits).isEqualTo(1);
    assertThat(txManager1.rollbacks).isEqualTo(0);

    CallCountingTransactionManager txManager2 = ctx.getBean("txManager2", CallCountingTransactionManager.class);
    assertThat(txManager2.begun).isEqualTo(0);
    assertThat(txManager2.commits).isEqualTo(0);
    assertThat(txManager2.rollbacks).isEqualTo(0);
  }

  @Test
  void apcEscalation() {
    new AnnotationConfigApplicationContext(EnableTxAndCachingConfig.class);
  }

  private void assertTxProxying(AnnotationConfigApplicationContext ctx) {
    FooRepository repo = ctx.getBean(FooRepository.class);
    assertThat(isTxProxy(repo)).isTrue();
    // trigger a transaction
    repo.findAll();
  }

  private boolean isTxProxy(FooRepository repo) {
    if (!AopUtils.isAopProxy(repo)) {
      return false;
    }
    return Arrays.stream(((Advised) repo).getAdvisors())
            .anyMatch(BeanFactoryTransactionAttributeSourceAdvisor.class::isInstance);
  }

  @Configuration
  @EnableTransactionManagement
  @ImportResource("infra/transaction/annotation/enable-caching.xml")
  static class EnableTxAndCachingConfig {

    @Bean
    public PlatformTransactionManager txManager() {
      return new CallCountingTransactionManager();
    }

    @Bean
    public FooRepository fooRepository() {
      return new DummyFooRepository();
    }

    @Bean
    public CacheManager cacheManager() {
      SimpleCacheManager mgr = new SimpleCacheManager();
      ArrayList<Cache> caches = new ArrayList<>();
      caches.add(new ConcurrentMapCache(""));
      mgr.setCaches(caches);
      return mgr;
    }
  }

  @Configuration
  @EnableTransactionManagement
  static class ImplicitTxManagerConfig {

    @Bean
    public PlatformTransactionManager txManager() {
      return new CallCountingTransactionManager();
    }

    @Bean
    public FooRepository fooRepository() {
      return new DummyFooRepository();
    }
  }

  @Configuration
  @EnableTransactionManagement
  static class ExplicitTxManagerConfig implements TransactionManagementConfigurer {

    @Bean
    public PlatformTransactionManager txManager1() {
      return new CallCountingTransactionManager();
    }

    @Bean
    public PlatformTransactionManager txManager2() {
      return new CallCountingTransactionManager();
    }

    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
      return txManager1();
    }

    @Bean
    public FooRepository fooRepository() {
      return new DummyFooRepository();
    }
  }

  @Configuration
  @EnableTransactionManagement
  static class DefaultTxManagerNameConfig {

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }
  }

  @Configuration
  @EnableTransactionManagement
  static class CustomTxManagerNameConfig {

    @Bean
    PlatformTransactionManager txManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }
  }

  @Configuration
  @EnableTransactionManagement
  static class NonConventionalTxManagerNameConfig {

    @Bean
    PlatformTransactionManager txManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }
  }

  @Configuration
  @EnableTransactionManagement(proxyTargetClass = true)
  static class ProxyTargetClassTxConfig {

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }
  }

  @Configuration
  @EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
  static class AspectJTxConfig {

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }
  }

  @Configuration
  static class Config {

    @Bean
    FooRepository fooRepository() {
      JdbcFooRepository repos = new JdbcFooRepository();
      repos.setDataSource(dataSource());
      return repos;
    }

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()
              .setType(EmbeddedDatabaseType.HSQL)
              .build();
    }
  }

  interface FooRepository {

    List<Object> findAll();
  }

  @Repository
  static class JdbcFooRepository implements FooRepository {

    public void setDataSource(DataSource dataSource) {
    }

    @Override
    @Transactional
    public List<Object> findAll() {
      return Collections.emptyList();
    }
  }

  @Repository
  static class DummyFooRepository implements FooRepository {

    @Override
    @Transactional
    public List<Object> findAll() {
      return Collections.emptyList();
    }
  }

}
