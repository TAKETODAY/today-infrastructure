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

import infra.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import infra.aop.target.SingletonTargetSource;
import infra.beans.factory.FactoryBeanNotInitializedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class AbstractSingletonProxyFactoryBeanTests {

  @Test
  void requiresAnObjectOrTargetSourceBeforeInitialization() {
    var factory = new GreetingProxyFactoryBean();
    assertThat(factory.getObjectType()).isNull();
    assertThatExceptionOfType(FactoryBeanNotInitializedException.class).isThrownBy(factory::getObject);
    assertThatIllegalArgumentException().isThrownBy(factory::afterPropertiesSet)
            .withMessageContaining("'target' is required");

    factory.setTarget("greeting");
    assertThatIllegalArgumentException().isThrownBy(factory::afterPropertiesSet)
            .withMessageContaining("needs to be a bean reference");
  }

  @Test
  void appliesPreMainAndPostInterceptorsToASingletonProxy() throws Exception {
    var factory = new GreetingProxyFactoryBean();
    var target = new GreetingTarget();
    var registry = spy(new DefaultAdvisorAdapterRegistry());
    MethodInterceptor pre = invocation -> "pre:" + invocation.proceed();
    MethodInterceptor post = invocation -> "post:" + invocation.proceed();
    factory.setTarget(target);
    assertThat(factory.getObjectType()).isEqualTo(GreetingTarget.class);
    factory.setPreInterceptors(new Object[] { pre });
    factory.setPostInterceptors(new Object[] { post });
    factory.setAdvisorAdapterRegistry(registry);
    factory.setProxyInterfaces(new Class<?>[] { Greeting.class });
    assertThat(factory.getObjectType()).isEqualTo(Greeting.class);

    ClassLoader proxyLoader = new ClassLoader(getClass().getClassLoader()) { };
    factory.setProxyClassLoader(proxyLoader);
    factory.setBeanClassLoader(getClass().getClassLoader());
    factory.afterPropertiesSet();

    Object proxy = factory.getObject();
    assertThat(((Greeting) proxy).greet()).isEqualTo("pre:main:post:hello");
    assertThat(((Advised) proxy).getTargetSource().getTarget()).isSameAs(target);
    assertThat(proxy.getClass().getClassLoader()).isSameAs(proxyLoader);
    assertThat(factory.getObjectType()).isEqualTo(proxy.getClass());
    assertThat(factory.isSingleton()).isTrue();
    assertThat(factory.getObject()).isSameAs(proxy);
    verify(registry).wrap(pre);
    verify(registry).wrap(post);
  }

  @Test
  void detectsInterfacesFromTheSuppliedTargetSource() {
    var factory = new GreetingProxyFactoryBean();
    var source = new SingletonTargetSource(new GreetingTarget());
    factory.setTarget(source);
    assertThat(factory.getObjectType()).isEqualTo(GreetingTarget.class);
    factory.afterPropertiesSet();

    Object proxy = factory.getObject();
    assertThat(((Greeting) proxy).greet()).isEqualTo("main:hello");
    assertThat(((Advised) proxy).getTargetSource()).isSameAs(source);
  }

  @Test
  void usesBeanClassLoaderWhenNoProxyClassLoaderIsConfigured() {
    var factory = new GreetingProxyFactoryBean();
    ClassLoader beanLoader = new ClassLoader(getClass().getClassLoader()) { };
    factory.setBeanClassLoader(beanLoader);
    factory.setTarget(new GreetingTarget());
    factory.afterPropertiesSet();

    assertThat(factory.getObject().getClass().getClassLoader()).isSameAs(beanLoader);
  }

  public interface Greeting {

    String greet();
  }

  private static class GreetingTarget implements Greeting {

    @Override
    public String greet() {
      return "hello";
    }
  }

  private static class GreetingProxyFactoryBean extends AbstractSingletonProxyFactoryBean {

    @Override
    protected Object createMainInterceptor() {
      return (MethodInterceptor) invocation -> "main:" + invocation.proceed();
    }
  }
}
