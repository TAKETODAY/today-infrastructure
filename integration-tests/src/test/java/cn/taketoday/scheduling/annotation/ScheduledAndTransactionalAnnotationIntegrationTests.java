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

package cn.taketoday.scheduling.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Repository;
import cn.taketoday.testfixture.transaction.CallCountingTransactionManager;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.annotation.EnableTransactionManagement;
import cn.taketoday.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Integration tests cornering bug SPR-8651, which revealed that @Scheduled methods may
 * not work well with beans that have already been proxied for other reasons such
 * as @Transactional or @Async processing.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("resource")
class ScheduledAndTransactionalAnnotationIntegrationTests {

  @Test
  void failsWhenJdkProxyAndScheduledMethodNotPresentOnInterface() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class, JdkProxyTxConfig.class, RepoConfigA.class);
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(ctx::refresh)
            .withCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void succeedsWhenSubclassProxyAndScheduledMethodNotPresentOnInterface() throws InterruptedException {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class, SubclassProxyTxConfig.class, RepoConfigA.class);
    ctx.refresh();

    Thread.sleep(100);  // allow @Scheduled method to be called several times

    MyRepository repository = ctx.getBean(MyRepository.class);
    CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);
    assertThat(AopUtils.isCglibProxy(repository)).isTrue();
    assertThat(repository.getInvocationCount()).isGreaterThan(0);
    assertThat(txManager.commits).isGreaterThan(0);
  }

  @Test
  void succeedsWhenJdkProxyAndScheduledMethodIsPresentOnInterface() throws InterruptedException {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class, JdkProxyTxConfig.class, RepoConfigB.class);
    ctx.refresh();

    Thread.sleep(100);  // allow @Scheduled method to be called several times

    MyRepositoryWithScheduledMethod repository = ctx.getBean(MyRepositoryWithScheduledMethod.class);
    CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);
    assertThat(AopUtils.isJdkDynamicProxy(repository)).isTrue();
    assertThat(repository.getInvocationCount()).isGreaterThan(0);
    assertThat(txManager.commits).isGreaterThan(0);
  }

  @Test
  void withAspectConfig() throws InterruptedException {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(AspectConfig.class, MyRepositoryWithScheduledMethodImpl.class);
    ctx.refresh();

    Thread.sleep(100);  // allow @Scheduled method to be called several times

    MyRepositoryWithScheduledMethod repository = ctx.getBean(MyRepositoryWithScheduledMethod.class);
    assertThat(AopUtils.isCglibProxy(repository)).isTrue();
    assertThat(repository.getInvocationCount()).isGreaterThan(0);
  }

  @Configuration
  @EnableTransactionManagement
  static class JdkProxyTxConfig {
  }

  @Configuration
  @EnableTransactionManagement(proxyTargetClass = true)
  static class SubclassProxyTxConfig {
  }

  @Configuration
  static class RepoConfigA {

    @Bean
    MyRepository repository() {
      return new MyRepositoryImpl();
    }
  }

  @Configuration
  static class RepoConfigB {

    @Bean
    MyRepositoryWithScheduledMethod repository() {
      return new MyRepositoryWithScheduledMethodImpl();
    }
  }

  @Configuration
  @EnableScheduling
  static class Config {

    @Bean
    PlatformTransactionManager txManager() {
      return new CallCountingTransactionManager();
    }

    @Bean
    PersistenceExceptionTranslator peTranslator() {
      return mock(PersistenceExceptionTranslator.class);
    }

    @Bean
    static PersistenceExceptionTranslationPostProcessor peTranslationPostProcessor() {
      return new PersistenceExceptionTranslationPostProcessor();
    }
  }

  @Configuration
  @EnableScheduling
  static class AspectConfig {

    @Bean
    static AnnotationAwareAspectJAutoProxyCreator autoProxyCreator() {
      AnnotationAwareAspectJAutoProxyCreator apc = new AnnotationAwareAspectJAutoProxyCreator();
      apc.setProxyTargetClass(true);
      return apc;
    }

    @Bean
    static MyAspect myAspect() {
      return new MyAspect();
    }
  }

  @Aspect
  public static class MyAspect {

    private final AtomicInteger count = new AtomicInteger();

    @Before("execution(* scheduled())")
    public void checkTransaction() {
      this.count.incrementAndGet();
    }
  }

  public interface MyRepository {

    int getInvocationCount();
  }

  @Repository
  static class MyRepositoryImpl implements MyRepository {

    private final AtomicInteger count = new AtomicInteger();

    @Transactional
    @Scheduled(fixedDelay = 5)
    public void scheduled() {
      this.count.incrementAndGet();
    }

    @Override
    public int getInvocationCount() {
      return this.count.get();
    }
  }

  public interface MyRepositoryWithScheduledMethod {

    int getInvocationCount();

    void scheduled();
  }

  @Repository
  static class MyRepositoryWithScheduledMethodImpl implements MyRepositoryWithScheduledMethod {

    private final AtomicInteger count = new AtomicInteger();

    @Nullable
    @Autowired(required = false)
    private MyAspect myAspect;

    @Override
    @Transactional
    @Scheduled(fixedDelay = 5)
    public void scheduled() {
      this.count.incrementAndGet();
    }

    @Override
    public int getInvocationCount() {
      if (this.myAspect != null) {
        assertThat(this.myAspect.count.get()).isEqualTo(this.count.get());
      }
      return this.count.get();
    }
  }

}
