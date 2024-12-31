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

package infra.aop.aspectj.autoproxy;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import infra.aop.ClassFilter;
import infra.aop.IntroductionAdvisor;
import infra.aop.IntroductionInterceptor;
import infra.aop.MethodBeforeAdvice;
import infra.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import infra.aop.aspectj.annotation.AspectMetadata;
import infra.aop.config.AopConfigUtils;
import infra.aop.framework.Advised;
import infra.aop.framework.ProxyConfig;
import infra.aop.framework.StandardProxy;
import infra.aop.support.AbstractPointcutAdvisor;
import infra.aop.support.AopUtils;
import infra.aop.support.StaticMethodMatcherPointcutAdvisor;
import infra.beans.PropertyValue;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.config.MethodInvokingFactoryBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.bytecode.proxy.Factory;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.EnableAspectJAutoProxy;
import infra.context.annotation.Scope;
import infra.context.support.ClassPathXmlApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.core.DecoratingProxy;
import infra.core.NestedRuntimeException;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AspectJ auto-proxying. Includes mixing with Framework AOP Advisors
 * to demonstrate that existing autoproxying contract is honoured.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
class AspectJAutoProxyCreatorTests {

  @Test
  void aspectsAreApplied() {
    ClassPathXmlApplicationContext bf = newContext("aspects.xml");

    ITestBean tb = bf.getBean("adrian", ITestBean.class);
    assertThat(tb.getAge()).isEqualTo(68);
    MethodInvokingFactoryBean factoryBean = (MethodInvokingFactoryBean) bf.getBean("&factoryBean");
    assertThat(AopUtils.isAopProxy(factoryBean.getTargetObject())).isTrue();
    assertThat(((ITestBean) factoryBean.getTargetObject()).getAge()).isEqualTo(68);
  }

  @Test
  void multipleAspectsWithParameterApplied() {
    ClassPathXmlApplicationContext bf = newContext("aspects.xml");

    ITestBean tb = bf.getBean("adrian", ITestBean.class);
    tb.setAge(10);
    assertThat(tb.getAge()).isEqualTo(20);
  }

  @Test
  void aspectsAreAppliedInDefinedOrder() {
    ClassPathXmlApplicationContext bf = newContext("aspectsWithOrdering.xml");

    ITestBean tb = bf.getBean("adrian", ITestBean.class);
    assertThat(tb.getAge()).isEqualTo(71);
  }

  @Test
  void aspectsAndAdvisorAreApplied() {
    ClassPathXmlApplicationContext ac = newContext("aspectsPlusAdvisor.xml");

    ITestBean shouldBeWoven = ac.getBean("adrian", ITestBean.class);
    assertAspectsAndAdvisorAreApplied(ac, shouldBeWoven);
  }

  @Test
  void aspectsAndAdvisorAreAppliedEvenIfComingFromParentFactory() {
    ClassPathXmlApplicationContext ac = newContext("aspectsPlusAdvisor.xml");

    GenericApplicationContext childAc = new GenericApplicationContext(ac);
    // Create a child factory with a bean that should be woven
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.getPropertyValues()
            .add(new PropertyValue("name", "Adrian"))
            .add(new PropertyValue("age", 34));
    childAc.registerBeanDefinition("adrian2", bd);
    // Register the advisor auto proxy creator with subclass
    childAc.registerBeanDefinition(AnnotationAwareAspectJAutoProxyCreator.class.getName(),
            new RootBeanDefinition(AnnotationAwareAspectJAutoProxyCreator.class));
    childAc.refresh();

    ITestBean beanFromParentContextThatShouldBeWoven = ac.getBean("adrian", ITestBean.class);
    ITestBean beanFromChildContextThatShouldBeWoven = childAc.getBean("adrian2", ITestBean.class);
    assertAspectsAndAdvisorAreApplied(childAc, beanFromParentContextThatShouldBeWoven);
    assertAspectsAndAdvisorAreApplied(childAc, beanFromChildContextThatShouldBeWoven);
  }

  protected void assertAspectsAndAdvisorAreApplied(ApplicationContext ac, ITestBean shouldBeWoven) {
    TestBeanAdvisor tba = (TestBeanAdvisor) ac.getBean("advisor");

    MultiplyReturnValue mrv = (MultiplyReturnValue) ac.getBean("aspect");
    assertThat(mrv.getMultiple()).isEqualTo(3);

    tba.count = 0;
    mrv.invocations = 0;

    assertThat(AopUtils.isAopProxy(shouldBeWoven)).as("Autoproxying must apply from @AspectJ aspect").isTrue();
    assertThat(shouldBeWoven.getName()).isEqualTo("Adrian");
    assertThat(mrv.invocations).isEqualTo(0);
    assertThat(shouldBeWoven.getAge()).isEqualTo((34 * mrv.getMultiple()));
    assertThat(tba.count).as("Spring advisor must be invoked").isEqualTo(2);
    assertThat(mrv.invocations).as("Must be able to hold state in aspect").isEqualTo(1);
  }

  @Test
  void perThisAspect() {
    ClassPathXmlApplicationContext bf = newContext("perthis.xml");

    ITestBean adrian1 = bf.getBean("adrian", ITestBean.class);
    assertThat(AopUtils.isAopProxy(adrian1)).isTrue();

    assertThat(adrian1.getAge()).isEqualTo(0);
    assertThat(adrian1.getAge()).isEqualTo(1);

    ITestBean adrian2 = bf.getBean("adrian", ITestBean.class);
    assertThat(adrian2).isNotSameAs(adrian1);
    assertThat(AopUtils.isAopProxy(adrian1)).isTrue();
    assertThat(adrian2.getAge()).isEqualTo(0);
    assertThat(adrian2.getAge()).isEqualTo(1);
    assertThat(adrian2.getAge()).isEqualTo(2);
    assertThat(adrian2.getAge()).isEqualTo(3);
    assertThat(adrian1.getAge()).isEqualTo(2);
  }

  @Test
  void perTargetAspect() throws SecurityException, NoSuchMethodException {
    ClassPathXmlApplicationContext bf = newContext("pertarget.xml");

    ITestBean adrian1 = bf.getBean("adrian", ITestBean.class);
    assertThat(AopUtils.isAopProxy(adrian1)).isTrue();

    // Does not trigger advice or count
    int explicitlySetAge = 25;
    adrian1.setAge(explicitlySetAge);

    assertThat(adrian1.getAge()).as("Setter does not initiate advice").isEqualTo(explicitlySetAge);
    // Fire aspect

    AspectMetadata am = new AspectMetadata(PerTargetAspect.class, "someBean");
    assertThat(am.getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();

    adrian1.getSpouse();

    assertThat(adrian1.getAge()).as("Advice has now been instantiated").isEqualTo(0);
    adrian1.setAge(11);
    assertThat(adrian1.getAge()).as("Any int setter increments").isEqualTo(2);
    adrian1.setName("Adrian");
    //assertEquals("Any other setter does not increment", 2, adrian1.getAge());

    ITestBean adrian2 = bf.getBean("adrian", ITestBean.class);
    assertThat(adrian2).isNotSameAs(adrian1);
    assertThat(AopUtils.isAopProxy(adrian1)).isTrue();
    assertThat(adrian2.getAge()).isEqualTo(34);
    adrian2.getSpouse();
    assertThat(adrian2.getAge()).as("Aspect now fired").isEqualTo(0);
    assertThat(adrian2.getAge()).isEqualTo(1);
    assertThat(adrian2.getAge()).isEqualTo(2);
    assertThat(adrian1.getAge()).isEqualTo(3);
  }

  @Test
    // gh-31238
  void cglibProxyClassIsCachedAcrossApplicationContextsForPerTargetAspect() {
    Class<?> configClass = PerTargetProxyTargetClassTrueConfig.class;
    TestBean testBean1;
    TestBean testBean2;

    // Round #1
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configClass)) {
      testBean1 = context.getBean(TestBean.class);
      assertThat(AopUtils.isCglibProxy(testBean1)).as("CGLIB proxy").isTrue();
      assertThat(testBean1.getClass().getInterfaces()).containsExactlyInAnyOrder(
              Factory.class, StandardProxy.class, Advised.class);
    }

    // Round #2
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configClass)) {
      testBean2 = context.getBean(TestBean.class);
      assertThat(AopUtils.isCglibProxy(testBean2)).as("CGLIB proxy").isTrue();
      assertThat(testBean2.getClass().getInterfaces()).containsExactlyInAnyOrder(
              Factory.class, StandardProxy.class, Advised.class);
    }

    assertThat(testBean1.getClass()).isSameAs(testBean2.getClass());
  }

  @Test
  void twoAdviceAspect() {
    ClassPathXmlApplicationContext bf = newContext("twoAdviceAspect.xml");

    ITestBean adrian1 = bf.getBean("adrian", ITestBean.class);
    testAgeAspect(adrian1, 0, 2);
  }

  @Test
  void twoAdviceAspectSingleton() {
    ClassPathXmlApplicationContext bf = newContext("twoAdviceAspectSingleton.xml");

    ITestBean adrian1 = bf.getBean("adrian", ITestBean.class);
    testAgeAspect(adrian1, 0, 1);
    ITestBean adrian2 = bf.getBean("adrian", ITestBean.class);
    assertThat(adrian2).isNotSameAs(adrian1);
    testAgeAspect(adrian2, 2, 1);
  }

  @Test
  void twoAdviceAspectPrototype() {
    ClassPathXmlApplicationContext bf = newContext("twoAdviceAspectPrototype.xml");

    ITestBean adrian1 = bf.getBean("adrian", ITestBean.class);
    testAgeAspect(adrian1, 0, 1);
    ITestBean adrian2 = bf.getBean("adrian", ITestBean.class);
    assertThat(adrian2).isNotSameAs(adrian1);
    testAgeAspect(adrian2, 0, 1);
  }

  private void testAgeAspect(ITestBean adrian, int start, int increment) {
    assertThat(AopUtils.isAopProxy(adrian)).isTrue();
    adrian.setName("");
    assertThat(adrian.age()).isEqualTo(start);
    int newAge = 32;
    adrian.setAge(newAge);
    assertThat(adrian.age()).isEqualTo(start + increment);
    adrian.setAge(0);
    assertThat(adrian.age()).isEqualTo(start + increment * 2);
  }

  @Test
  void adviceUsingJoinPoint() {
    ClassPathXmlApplicationContext bf = newContext("usesJoinPointAspect.xml");

    ITestBean adrian1 = bf.getBean("adrian", ITestBean.class);
    adrian1.getAge();
    AdviceUsingThisJoinPoint aspectInstance = (AdviceUsingThisJoinPoint) bf.getBean("aspect");
    //(AdviceUsingThisJoinPoint) Aspects.aspectOf(AdviceUsingThisJoinPoint.class);
    //assertEquals("method-execution(int TestBean.getAge())",aspectInstance.getLastMethodEntered());
    assertThat(aspectInstance.getLastMethodEntered()).doesNotStartWith("TestBean.getAge())");
  }

  @Test
  void includeMechanism() {
    ClassPathXmlApplicationContext bf = newContext("usesInclude.xml");

    ITestBean adrian = bf.getBean("adrian", ITestBean.class);
    assertThat(AopUtils.isAopProxy(adrian)).isTrue();
    assertThat(adrian.getAge()).isEqualTo(68);
  }

  @Test
  void forceProxyTargetClass() {
    ClassPathXmlApplicationContext bf = newContext("aspectsWithCGLIB.xml");

    ProxyConfig pc = (ProxyConfig) bf.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
    assertThat(pc.isProxyTargetClass()).as("should be proxying classes").isTrue();
    assertThat(pc.isExposeProxy()).as("should expose proxy").isTrue();
  }

  @Test
  void withAbstractFactoryBeanAreApplied() {
    ClassPathXmlApplicationContext bf = newContext("aspectsWithAbstractBean.xml");

    ITestBean adrian = bf.getBean("adrian", ITestBean.class);
    assertThat(AopUtils.isAopProxy(adrian)).isTrue();
    assertThat(adrian.getAge()).isEqualTo(68);
  }

  @Test
  void retryAspect() {
    ClassPathXmlApplicationContext bf = newContext("retryAspect.xml");

    UnreliableBean bean = (UnreliableBean) bf.getBean("unreliableBean");
    RetryAspect aspect = (RetryAspect) bf.getBean("retryAspect");
    assertThat(bean.unreliable()).isEqualTo(2);
    assertThat(aspect.getBeginCalls()).isEqualTo(2);
    assertThat(aspect.getRollbackCalls()).isEqualTo(1);
    assertThat(aspect.getCommitCalls()).isEqualTo(1);
  }

  @Test
  void withBeanNameAutoProxyCreator() {
    ClassPathXmlApplicationContext bf = newContext("withBeanNameAutoProxyCreator.xml");

    ITestBean tb = bf.getBean("adrian", ITestBean.class);
    assertThat(tb.getAge()).isEqualTo(68);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @ValueSource(classes = { ProxyTargetClassFalseConfig.class, ProxyTargetClassTrueConfig.class })
  void lambdaIsAlwaysProxiedWithJdkProxy(Class<?> configClass) {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configClass)) {
      @SuppressWarnings("unchecked")
      Supplier<String> supplier = context.getBean(Supplier.class);
      assertThat(AopUtils.isAopProxy(supplier)).as("AOP proxy").isTrue();
      assertThat(AopUtils.isJdkDynamicProxy(supplier)).as("JDK Dynamic proxy").isTrue();
      assertThat(supplier.getClass().getInterfaces()).containsExactlyInAnyOrder(
              Supplier.class, StandardProxy.class, Advised.class, DecoratingProxy.class);
      assertThat(supplier.get()).isEqualTo("advised: lambda");
    }
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @ValueSource(classes = { MixinProxyTargetClassFalseConfig.class, MixinProxyTargetClassTrueConfig.class })
  void lambdaIsAlwaysProxiedWithJdkProxyWithIntroductions(Class<?> configClass) {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configClass)) {
      MessageGenerator messageGenerator = context.getBean(MessageGenerator.class);
      assertThat(AopUtils.isAopProxy(messageGenerator)).as("AOP proxy").isTrue();
      assertThat(AopUtils.isJdkDynamicProxy(messageGenerator)).as("JDK Dynamic proxy").isTrue();
      assertThat(messageGenerator.getClass().getInterfaces()).containsExactlyInAnyOrder(
              MessageGenerator.class, Mixin.class, StandardProxy.class, Advised.class, DecoratingProxy.class);
      assertThat(messageGenerator.generateMessage()).isEqualTo("mixin: lambda");
    }
  }

  private ClassPathXmlApplicationContext newContext(String fileSuffix) {
    return new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-" + fileSuffix, getClass());
  }

}

@Aspect("pertarget(execution(* *.getSpouse()))")
class PerTargetAspect implements Ordered {

  public int count;

  private int order = Ordered.LOWEST_PRECEDENCE;

  @Around("execution(int *.getAge())")
  public int returnCountAsAge() {
    return count++;
  }

  @Before("execution(void *.set*(int))")
  public void countSetter() {
    ++count;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  public void setOrder(int order) {
    this.order = order;
  }
}

@Aspect
class AdviceUsingThisJoinPoint {

  private String lastEntry = "";

  public String getLastMethodEntered() {
    return this.lastEntry;
  }

  @Pointcut("execution(* *(..))")
  public void methodExecution() {
  }

  @Before("methodExecution()")
  public void entryTrace(JoinPoint jp) {
    this.lastEntry = jp.toString();
  }
}

@Aspect
class DummyAspect {

  @Around("execution(* setAge(int))")
  public Object test(ProceedingJoinPoint pjp) throws Throwable {
    return pjp.proceed();
  }
}

@Aspect
class DummyAspectWithParameter {

  @Around("execution(* setAge(int)) && args(age)")
  public Object test(ProceedingJoinPoint pjp, int age) throws Throwable {
    return pjp.proceed();
  }

}

class DummyFactoryBean implements FactoryBean<Object> {

  @Override
  public Object getObject() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<?> getObjectType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSingleton() {
    throw new UnsupportedOperationException();
  }

}

@Aspect
@Order(10)
class IncreaseReturnValue {

  @Around("execution(int *.getAge())")
  public Object doubleReturnValue(ProceedingJoinPoint pjp) throws Throwable {
    int result = (Integer) pjp.proceed();
    return result + 3;
  }
}

@Aspect
class MultiplyReturnValue {

  private int multiple = 2;

  public int invocations;

  public void setMultiple(int multiple) {
    this.multiple = multiple;
  }

  public int getMultiple() {
    return this.multiple;
  }

  @Around("execution(int *.getAge())")
  public Object doubleReturnValue(ProceedingJoinPoint pjp) throws Throwable {
    ++this.invocations;
    int result = (Integer) pjp.proceed();
    return result * this.multiple;
  }
}

@Retention(RetentionPolicy.RUNTIME)
@interface Marker {
}

@Aspect
class MultiplyReturnValueForMarker {

  private int multiple = 2;

  public int invocations;

  public void setMultiple(int multiple) {
    this.multiple = multiple;
  }

  public int getMultiple() {
    return this.multiple;
  }

  @Around("@annotation(infra.aop.aspectj.autoproxy.Marker)")
  public Object doubleReturnValue(ProceedingJoinPoint pjp) throws Throwable {
    ++this.invocations;
    int result = (Integer) pjp.proceed();
    return result * this.multiple;
  }
}

interface IMarkerTestBean extends ITestBean {

  @Marker
  @Override
  int getAge();
}

class MarkerTestBean extends TestBean implements IMarkerTestBean {

  @Marker
  @Override
  public int getAge() {
    return super.getAge();
  }
}

@Aspect
class RetryAspect {

  private int beginCalls;

  private int commitCalls;

  private int rollbackCalls;

  @Pointcut("execution(public * infra.aop.aspectj.autoproxy.UnreliableBean.*(..))")
  public void execOfPublicMethod() {
  }

  /**
   * Retry Advice
   */
  @Around("execOfPublicMethod()")
  public Object retry(ProceedingJoinPoint jp) throws Throwable {
    boolean retry = true;
    Object o = null;
    while (retry) {
      try {
        retry = false;
        this.beginCalls++;
        try {
          o = jp.proceed();
          this.commitCalls++;
        }
        catch (RetryableException re) {
          this.rollbackCalls++;
          throw re;
        }
      }
      catch (RetryableException re) {
        retry = true;
      }
    }
    return o;
  }

  public int getBeginCalls() {
    return this.beginCalls;
  }

  public int getCommitCalls() {
    return this.commitCalls;
  }

  public int getRollbackCalls() {
    return this.rollbackCalls;
  }
}

@SuppressWarnings("serial")
class RetryableException extends NestedRuntimeException {

  public RetryableException(String msg) {
    super(msg);
  }

  public RetryableException(String msg, Throwable cause) {
    super(msg, cause);
  }
}

class UnreliableBean {

  private int calls;

  public int unreliable() {
    this.calls++;
    if (this.calls % 2 != 0) {
      throw new RetryableException("foo");
    }
    return this.calls;
  }

}

@SuppressWarnings("serial")
class TestBeanAdvisor extends StaticMethodMatcherPointcutAdvisor {

  public int count;

  public TestBeanAdvisor() {
    setAdvice((MethodBeforeAdvice) (method) -> ++count);
  }

  @Override
  public boolean matches(Method method, @Nullable Class<?> targetClass) {
    return ITestBean.class.isAssignableFrom(targetClass);
  }

}

abstract class AbstractProxyTargetClassConfig {

  @Bean
  Supplier<String> stringSupplier() {
    return () -> "lambda";
  }

  @Bean
  SupplierAdvice supplierAdvice() {
    return new SupplierAdvice();
  }

  @Aspect
  static class SupplierAdvice {

    @Around("execution(public * infra.aop.aspectj.autoproxy..*.*(..))")
    Object aroundSupplier(ProceedingJoinPoint joinPoint) throws Throwable {
      return "advised: " + joinPoint.proceed();
    }
  }
}

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = false)
class ProxyTargetClassFalseConfig extends AbstractProxyTargetClassConfig {
}

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = true)
class ProxyTargetClassTrueConfig extends AbstractProxyTargetClassConfig {
}

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
class PerTargetProxyTargetClassTrueConfig {

  @Bean
  @Scope("prototype")
  TestBean testBean() {
    return new TestBean("Jane", 34);
  }

  @Bean
  @Scope("prototype")
  PerTargetAspect perTargetAspect() {
    return new PerTargetAspect();
  }
}

@FunctionalInterface
interface MessageGenerator {
  String generateMessage();
}

interface Mixin {
}

class MixinIntroductionInterceptor implements IntroductionInterceptor {

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    return "mixin: " + invocation.proceed();
  }

  @Override
  public boolean implementsInterface(Class<?> intf) {
    return Mixin.class.isAssignableFrom(intf);
  }

}

@SuppressWarnings("serial")
class MixinAdvisor extends AbstractPointcutAdvisor implements IntroductionAdvisor {

  @Override
  public infra.aop.Pointcut getPointcut() {
    return infra.aop.Pointcut.TRUE;
  }

  @Override
  public Advice getAdvice() {
    return new MixinIntroductionInterceptor();
  }

  @Override
  public Class<?>[] getInterfaces() {
    return new Class[] { Mixin.class };
  }

  @Override
  public ClassFilter getClassFilter() {
    return MessageGenerator.class::isAssignableFrom;
  }

  @Override
  public void validateInterfaces() {
    /* no-op */
  }

}

abstract class AbstractMixinConfig {

  @Bean
  MessageGenerator messageGenerator() {
    return () -> "lambda";
  }

  @Bean
  MixinAdvisor mixinAdvisor() {
    return new MixinAdvisor();
  }

}

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = false)
class MixinProxyTargetClassFalseConfig extends AbstractMixinConfig {
}

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = true)
class MixinProxyTargetClassTrueConfig extends AbstractMixinConfig {
}
