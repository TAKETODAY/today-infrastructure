/*
 * Copyright 2002-present the original author or authors.
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

package infra.aop.framework;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.Set;

import infra.aop.TargetSource;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.bytecode.core.CodeGenerationException;
import infra.bytecode.proxy.MethodProxy;
import infra.util.ClassUtils;

import static infra.aop.InterceptorChainFactory.EMPTY_INTERCEPTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Regression tests for final methods declared on container callback interfaces. */
class CglibAopProxyConfigurationCallbackTests {

  @Test
  void finalAfterPropertiesSetIsRecognisedAsCallback() throws NoSuchMethodException {
    assertCallback(WithFinalAfterPropertiesSet.class, "afterPropertiesSet", true);
  }

  @Test
  void finalDestroyIsRecognisedAsCallback() throws NoSuchMethodException {
    assertCallback(WithFinalDestroy.class, "destroy", true);
  }

  @Test
  void finalAwareCallbackIsRecognisedAsCallback() throws NoSuchMethodException {
    assertCallback(WithFinalBeanFactoryAware.class, "setBeanFactory", true, BeanFactory.class);
  }

  @Test
  void finalCloseableCloseIsRecognisedAsCallback() throws NoSuchMethodException {
    assertCallback(WithFinalCloseableClose.class, "close", true);
  }

  @Test
  void finalAutoCloseableCloseIsRecognisedAsCallback() throws NoSuchMethodException {
    assertCallback(WithFinalAutoCloseableClose.class, "close", true);
  }

  @Test
  void finalUserInterfaceMethodIsNotSuppressed() throws NoSuchMethodException {
    assertCallback(WithFinalUserApi.class, "execute", false);
  }

  @Test
  void methodSharedBetweenCallbackAndUserInterfaceIsNotSuppressed() throws NoSuchMethodException {
    assertCallback(WithSharedSignature.class, "afterPropertiesSet", false);
  }

  @Test
  void constructorArgumentsMustBeSpecifiedTogetherAndHaveMatchingSizes() {
    AdvisedSupport config = new AdvisedSupport();
    config.setTargetClass(ClassWithConstructor.class);
    CglibAopProxy proxy = new CglibAopProxy(config);

    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setConstructorArguments(null,
            new Class<?>[] { Object.class }));
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setConstructorArguments(new Object[0],
            new Class<?>[] { Object.class }));
  }

  @Test
  void constructorArgumentsAreUsedWhenCreatingClassProxy() {
    AdvisedSupport config = new AdvisedSupport();
    config.setTargetClass(ClassWithConstructor.class);
    CglibAopProxy proxy = new CglibAopProxy(config);
    proxy.setConstructorArguments(new Object[] { "value" }, new Class<?>[] { Object.class });

    assertThat(proxy.getProxy()).isInstanceOf(ClassWithConstructor.class);
  }

  @Test
  void proxyTypeCanBeGeneratedWithoutRunningTheTargetConstructor() {
    var config = new AdvisedSupport();
    config.setTargetClass(ClassWithConstructor.class);
    var proxy = new CglibAopProxy(config);

    assertThat(proxy.getProxyClass(getClass().getClassLoader()).getSuperclass()).isEqualTo(ClassWithConstructor.class);
  }

  @Test
  void instantiationAwareProxyAlsoAcceptsExplicitConstructorArguments() {
    var config = new AdvisedSupport();
    config.setTargetClass(ClassWithConstructor.class);
    var proxy = new InstantiationAwareCglibAopProxy(config);
    proxy.setConstructorArguments(new Object[] { "value" }, new Class<?>[] { Object.class });

    assertThat(proxy.getProxy()).isInstanceOf(ClassWithConstructor.class);
  }

  @Test
  void equivalentClassProxyConfigurationsHaveEqualFactoriesAndProxies() {
    var target = new FluentTarget();
    var config = new AdvisedSupport();
    config.setTarget(target);
    var otherConfig = new AdvisedSupport();
    otherConfig.setTarget(target);
    var first = new CglibAopProxy(config);
    var second = new CglibAopProxy(otherConfig);

    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    assertThat(first).isNotEqualTo(new Object());
    assertThat(first.getProxy()).isEqualTo(second.getProxy()).isNotEqualTo(target);
  }

  @Test
  void fallsBackToReflectionWhenFastClassGenerationFailsForAnAdvisedCall() throws Throwable {
    var target = new FluentTarget();
    Method method = FluentTarget.class.getMethod("self");
    MethodProxy fastProxy = mock();
    given(fastProxy.invoke(target, new Object[0])).willThrow(new CodeGenerationException(new IllegalStateException("fast class")));
    var invocation = new CglibAopProxy.CglibMethodInvocation(new Object(), target, method,
            FluentTarget.class, fastProxy, new Object[0], EMPTY_INTERCEPTOR);

    assertThat(invocation.proceed()).isSameAs(target);
    verify(fastProxy).invoke(target, new Object[0]);
  }

  @Test
  void targetlessInvocationRetainsItsMethodAndSharesAttributesWithTheClone() throws Exception {
    Method method = FluentTarget.class.getMethod("self");
    var invocation = new DefaultMethodInvocation(new Object(), null, method, null, new Object[0], EMPTY_INTERCEPTOR);
    var clone = (DefaultMethodInvocation) invocation.invocableClone();

    assertThat(invocation.getStaticPart()).isSameAs(method);
    assertThat(invocation.getTargetClass()).isEqualTo(FluentTarget.class);
    assertThat(invocation.toString()).contains("self", "target is null");
    assertThat(invocation).isEqualTo(clone).hasSameHashCodeAs(clone).isNotEqualTo(method);
    clone.setAttribute("marker", "shared");
    assertThat(invocation.getAttribute("marker")).isEqualTo("shared");
    clone.setArguments(new Object[] { "different" });
    assertThat(invocation).isNotEqualTo(clone);
  }

  @Test
  void invocationCloneReportsSubclassThatDisablesCloning() throws Exception {
    var invocation = new DefaultMethodInvocation(new Object(), null, FluentTarget.class.getMethod("self"),
            null, new Object[0], EMPTY_INTERCEPTOR) {
      @Override
      protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("disabled");
      }
    };

    assertThatIllegalStateException().isThrownBy(invocation::invocableClone).withMessageContaining("disabled");
  }

  @Test
  void frozenUnadvisedSingletonPreservesFluentProxyIdentity() {
    ProxyFactory factory = new ProxyFactory(new FluentTarget());
    factory.setProxyTargetClass(true);
    factory.setFrozen(true);
    FluentTarget proxy = (FluentTarget) factory.getProxy();

    assertThat(proxy.self()).isSameAs(proxy);
  }

  @Test
  void frozenUnadvisedSingletonExposesAndRestoresTheCurrentProxy() {
    ProxyFactory factory = new ProxyFactory(new FluentTarget());
    factory.setProxyTargetClass(true);
    factory.setFrozen(true);
    factory.setExposeProxy(true);
    FluentTarget proxy = (FluentTarget) factory.getProxy();

    assertThat(proxy.currentProxy()).isSameAs(proxy);
    assertThatIllegalStateException().isThrownBy(AopContext::currentProxy);
  }

  @Test
  void frozenUnadvisedDynamicSourceReleasesTargetEvenOnFailure() throws Exception {
    FluentTarget target = new FluentTarget();
    TargetSource source = mock();
    willReturn(FluentTarget.class).given(source).getTargetClass();
    given(source.getTarget()).willReturn(target);
    ProxyFactory factory = new ProxyFactory();
    factory.setTargetSource(source);
    factory.setProxyTargetClass(true);
    factory.setFrozen(true);
    FluentTarget proxy = (FluentTarget) factory.getProxy();

    assertThat(proxy.self()).isSameAs(proxy);
    assertThatIllegalStateException().isThrownBy(proxy::currentProxy);
    verify(source, times(2)).releaseTarget(target);
  }

  @Test
  void frozenUnadvisedDynamicSourceExposesProxyAndReleasesTarget() throws Exception {
    FluentTarget target = new FluentTarget();
    TargetSource source = mock();
    willReturn(FluentTarget.class).given(source).getTargetClass();
    given(source.getTarget()).willReturn(target);
    ProxyFactory factory = new ProxyFactory();
    factory.setTargetSource(source);
    factory.setProxyTargetClass(true);
    factory.setFrozen(true);
    factory.setExposeProxy(true);
    FluentTarget proxy = (FluentTarget) factory.getProxy();

    assertThat(proxy.currentProxy()).isSameAs(proxy);
    verify(source).releaseTarget(target);
    assertThatIllegalStateException().isThrownBy(AopContext::currentProxy);
  }

  @Test
  void finalMethodWithoutInterfaceMatchIsNotSuppressed() throws NoSuchMethodException {
    assertCallback(WithStandaloneFinal.class, "doSomething", false);
  }

  private static void assertCallback(Class<?> type, String name, boolean expected, Class<?>... parameterTypes)
          throws NoSuchMethodException {
    Method method = type.getDeclaredMethod(name, parameterTypes);
    Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(type);
    assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isEqualTo(expected);
  }

  static class WithFinalAfterPropertiesSet implements InitializingBean {
    @Override
    public final void afterPropertiesSet() { }
  }

  static class FluentTarget {

    public FluentTarget self() {
      return this;
    }

    public Object currentProxy() {
      return AopContext.currentProxy();
    }
  }

  static class WithFinalDestroy implements DisposableBean {
    @Override
    public final void destroy() { }
  }

  static class WithFinalBeanFactoryAware implements BeanFactoryAware {
    @Override
    public final void setBeanFactory(BeanFactory beanFactory) { }
  }

  static class WithFinalCloseableClose implements Closeable {
    @Override
    public final void close() { }
  }

  static class WithFinalAutoCloseableClose implements AutoCloseable {
    @Override
    public final void close() { }
  }

  interface UserApi {
    void execute();
  }

  static class WithFinalUserApi implements UserApi {
    @Override
    public final void execute() { }
  }

  interface CustomLifecycle {
    void afterPropertiesSet();
  }

  static class WithSharedSignature implements InitializingBean, CustomLifecycle {
    @Override
    public final void afterPropertiesSet() { }
  }

  static class WithStandaloneFinal {
    public final void doSomething() { }
  }
}
