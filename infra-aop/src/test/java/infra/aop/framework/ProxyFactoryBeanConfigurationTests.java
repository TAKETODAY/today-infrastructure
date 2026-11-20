/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import infra.aop.framework.adapter.AdvisorAdapterRegistry;
import infra.aop.support.DefaultPointcutAdvisor;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.FactoryBeanNotInitializedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProxyFactoryBeanConfigurationTests {

  @Test
  void explicitProxyClassLoaderOverridesTheBeanClassLoader() {
    var parent = getClass().getClassLoader();
    var explicit = new ClassLoader(parent) { };
    var factory = new ProxyFactoryBean();
    factory.setProxyClassLoader(explicit);
    factory.setBeanClassLoader(parent);
    factory.setProxyInterfaces(new Class<?>[] { Runnable.class });
    factory.addAdvice((MethodInterceptor) invocation -> null);

    assertThat(factory.getObjectType().getClassLoader()).isSameAs(explicit);
    assertThat(factory.getObject()).isExactlyInstanceOf(factory.getObjectType());
    assertThat(factory.createCompositeInterface(new Class<?>[] { Runnable.class, AutoCloseable.class }).getClassLoader())
            .isSameAs(explicit);
  }

  @Test
  void unknownFinalBeanTypeIsResolvedAsTargetAndRegistryAdaptsNamedAdvice() {
    BeanFactory beans = mock();
    AdvisorAdapterRegistry registry = mock();
    MethodInterceptor interceptor = invocation -> "advised:" + invocation.proceed();
    given(registry.wrap(interceptor)).willReturn(new DefaultPointcutAdvisor(interceptor));
    given(beans.getBean("advice")).willReturn(interceptor);
    given(beans.getBean("target")).willReturn(new Greeting());
    var factory = new ProxyFactoryBean();
    factory.setAdvisorAdapterRegistry(registry);
    factory.setInterceptorNames("advice", "target");
    factory.setBeanFactory(beans);

    assertThat(((Greeting) factory.getObject()).message()).isEqualTo("advised:hello");
    verify(registry).wrap(interceptor);
  }

  @Test
  void missingFactoryReportsUnresolvedAdviceOrTargetInsteadOfReturningAProxy() {
    var factory = new ProxyFactoryBean();
    factory.setInterceptorNames("audit");
    assertThatIllegalStateException().isThrownBy(factory::getObject).withMessageContaining("interceptor names [audit]");

    var namedTargetFactory = new ProxyFactoryBean();
    namedTargetFactory.setTargetName("service");
    assertThatIllegalStateException().isThrownBy(namedTargetFactory::getObject).withMessageContaining("target with name 'service'");
  }

  @Test
  void proxyTypePredictionPropagatesSubclassGenerationErrorsForKnownTargets() {
    var factory = new ProxyFactoryBean();
    factory.setTarget("final target");
    factory.setProxyTargetClass(true);

    assertThatExceptionOfType(AopConfigException.class).isThrownBy(factory::getObjectType)
            .withMessageContaining("Could not generate CGLIB subclass");
    assertThatExceptionOfType(FactoryBeanNotInitializedException.class).isThrownBy(new ProxyFactoryBean()::getObject)
            .withMessageContaining("Cannot determine target class");
  }

  static class Greeting {

    public String message() {
      return "hello";
    }
  }
}
