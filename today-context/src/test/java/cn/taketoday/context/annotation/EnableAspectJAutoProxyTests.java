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

package cn.taketoday.context.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.taketoday.aop.framework.AopContext;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import example.scannable.FooDao;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
import example.scannable.ServiceInvocationCounter;
import example.scannable.StubFooDao;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/13 10:38
 */
public class EnableAspectJAutoProxyTests {

  @Test
  public void withJdkProxy() {
    ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithJdkProxy.class);

    aspectIsApplied(ctx);
    assertThat(AopUtils.isJdkDynamicProxy(ctx.getBean(FooService.class))).isTrue();
  }

  @Test
  public void withCglibProxy() {
    ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithCglibProxy.class);

    aspectIsApplied(ctx);
    assertThat(AopUtils.isCglibProxy(ctx.getBean(FooService.class))).isTrue();
  }

  @Test
  public void withExposedProxy() {
    ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithExposedProxy.class);

    aspectIsApplied(ctx);
    assertThat(AopUtils.isJdkDynamicProxy(ctx.getBean(FooService.class))).isTrue();
  }

  private void aspectIsApplied(ApplicationContext ctx) {
    FooService fooService = ctx.getBean(FooService.class);
    ServiceInvocationCounter counter = ctx.getBean(ServiceInvocationCounter.class);

    assertThat(counter.getCount()).isEqualTo(0);

    assertThat(fooService.isInitCalled()).isTrue();
    assertThat(counter.getCount()).isEqualTo(1);

    String value = fooService.foo(1);
    assertThat(value).isEqualTo("bar");
    assertThat(counter.getCount()).isEqualTo(2);

    fooService.foo(1);
    assertThat(counter.getCount()).isEqualTo(3);
  }

  @Test
  public void withAnnotationOnArgumentAndJdkProxy() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
            ConfigWithJdkProxy.class, SampleService.class, LoggingAspect.class);

    SampleService sampleService = ctx.getBean(SampleService.class);
    sampleService.execute(new SampleDto());
    sampleService.execute(new SampleInputBean());
    sampleService.execute((SampleDto) null);
    sampleService.execute((SampleInputBean) null);
  }

  @Test
  public void withAnnotationOnArgumentAndCglibProxy() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
            ConfigWithCglibProxy.class, SampleService.class, LoggingAspect.class);

    SampleService sampleService = ctx.getBean(SampleService.class);
    sampleService.execute(new SampleDto());
    sampleService.execute(new SampleInputBean());
    sampleService.execute((SampleDto) null);
    sampleService.execute((SampleInputBean) null);
  }

  @ComponentScan("example.scannable")
  @EnableAspectJAutoProxy
  static class ConfigWithJdkProxy {
  }

  @ComponentScan("example.scannable")
  @EnableAspectJAutoProxy(proxyTargetClass = true)
  static class ConfigWithCglibProxy {
  }

  @Import({ ServiceInvocationCounter.class, StubFooDao.class })
  @EnableAspectJAutoProxy(exposeProxy = true)
  static class ConfigWithExposedProxy {

    @Bean
    public FooService fooServiceImpl(final ApplicationContext context) {
      return new FooServiceImpl() {
        @Override
        public String foo(int id) {
          assertThat(AopContext.currentProxy()).isNotNull();
          return super.foo(id);
        }

        @Override
        protected FooDao fooDao() {
          return context.getBean(FooDao.class);
        }
      };
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface Loggable {
  }

  @Loggable
  public static class SampleDto {
  }

  public static class SampleInputBean {
  }

  public static class SampleService {

    // Not matched method on {@link LoggingAspect}.
    public void execute(SampleInputBean inputBean) {
    }

    // Matched method on {@link LoggingAspect}
    public void execute(SampleDto dto) {
    }
  }

  @Aspect
  public static class LoggingAspect {

    @Before("@args(cn.taketoday.context.annotation.EnableAspectJAutoProxyTests.Loggable))")
    public void loggingBeginByAtArgs() {
    }
  }

}
