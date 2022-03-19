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

package cn.taketoday.transaction.annotation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.cache.support.SimpleCacheManager;
import cn.taketoday.context.annotation.AdviceMode;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ImportResource;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;
import cn.taketoday.lang.Repository;
import cn.taketoday.testfixture.transaction.CallCountingTransactionManager;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for the @EnableTransactionManagement annotation.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.1
 */
@SuppressWarnings("resource")
class EnableTransactionManagementIntegrationTests {

  @Test
  void repositoryIsNotTxProxy() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);

    assertThat(isTxProxy(ctx.getBean(FooRepository.class))).isFalse();
  }

  @Test
  void repositoryIsTxProxy_withDefaultTxManagerName() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, DefaultTxManagerNameConfig.class);

    assertTxProxying(ctx);
  }

  @Test
  void repositoryIsTxProxy_withCustomTxManagerName() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, CustomTxManagerNameConfig.class);

    assertTxProxying(ctx);
  }

  @Test
  void repositoryIsTxProxy_withNonConventionalTxManagerName_fallsBackToByTypeLookup() {
    assertTxProxying(new AnnotationConfigApplicationContext(Config.class, NonConventionalTxManagerNameConfig.class));
  }

  @Test
  void repositoryIsClassBasedTxProxy() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, ProxyTargetClassTxConfig.class);

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
    assertThatExceptionOfType(Exception.class)
            .isThrownBy(ctx::refresh)
            .withMessageContaining("AspectJJtaTransactionManagementConfiguration");
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
  @ImportResource("cn/taketoday/transaction/annotation/enable-caching.xml")
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
