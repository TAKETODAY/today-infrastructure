/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import example.scannable.FooDao;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
import example.scannable.ServiceInvocationCounter;
import example.scannable.StubFooDao;
import infra.aop.framework.AopContext;
import infra.aop.support.AopUtils;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/13 10:38
 */
class EnableAspectJAutoProxyTests {

  @Test
  void withJdkProxy() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithJdkProxy.class);

    aspectIsApplied(ctx);
    assertThat(AopUtils.isJdkDynamicProxy(ctx.getBean(FooService.class))).isTrue();
    assertThat(AopUtils.isJdkDynamicProxy(ctx.getBean("otherFooService"))).isTrue();
    ctx.close();
  }

  @Test
  void withCglibProxy() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithCglibProxy.class);

    aspectIsApplied(ctx);
    assertThat(AopUtils.isCglibProxy(ctx.getBean(FooService.class))).isTrue();
    assertThat(AopUtils.isJdkDynamicProxy(ctx.getBean("otherFooService"))).isTrue();
    ctx.close();
  }

  @Test
  void withExposedProxy() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithExposedProxy.class);

    aspectIsApplied(ctx);
    assertThat(AopUtils.isJdkDynamicProxy(ctx.getBean(FooService.class))).isTrue();
    ctx.close();
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
  void withAnnotationOnArgumentAndJdkProxy() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
            ConfigWithJdkProxy.class, SampleService.class, LoggingAspect.class);

    SampleService sampleService = ctx.getBean(SampleService.class);
    sampleService.execute(new SampleDto());
    sampleService.execute(new SampleInputBean());
    sampleService.execute((SampleDto) null);
    sampleService.execute((SampleInputBean) null);
    ctx.close();
  }

  @Test
  void withAnnotationOnArgumentAndCglibProxy() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
            ConfigWithCglibProxy.class, SampleService.class, LoggingAspect.class);

    SampleService sampleService = ctx.getBean(SampleService.class);
    sampleService.execute(new SampleDto());
    sampleService.execute(new SampleInputBean());
    sampleService.execute((SampleDto) null);
    sampleService.execute((SampleInputBean) null);
    ctx.close();
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
    FooService fooServiceImpl(final ApplicationContext context) {
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
  @interface Loggable {
  }

  @Loggable
  static class SampleDto {
  }

  static class SampleInputBean {
  }

  static class SampleService {

    // Not matched method on {@link LoggingAspect}.
    public void execute(SampleInputBean inputBean) {
    }

    // Matched method on {@link LoggingAspect}
    public void execute(SampleDto dto) {
    }
  }

  @Aspect
  public static class LoggingAspect {

    @Before("@args(infra.context.annotation.EnableAspectJAutoProxyTests.Loggable))")
    public void loggingBeginByAtArgs() {
    }
  }

}
