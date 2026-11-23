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

package infra.aop.target;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import infra.aop.framework.ProxyFactory;
import infra.aop.support.AopUtils;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.ITestBean;
import infra.core.io.Resource;

import static infra.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Stephane Nicoll
 */
public class CommonsPool2TargetSourceProxyTests {

  private static final Resource CONTEXT =
          qualifiedResource(CommonsPool2TargetSourceProxyTests.class, "context.xml");

  @Test
  public void testProxy() throws Exception {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
    reader.loadBeanDefinitions(CONTEXT);
    beanFactory.preInstantiateSingletons();
    ITestBean bean = (ITestBean) beanFactory.getBean("testBean");
    assertThat(AopUtils.isAopProxy(bean)).isTrue();
  }

  @Test
  void poolExhaustionReuseAndDestruction() throws Exception {
    var factory = new StandardBeanFactory();
    var definition = new RootBeanDefinition(PooledService.class);
    definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    factory.registerBeanDefinition("service", definition);

    var source = new CommonsPool2TargetSource();
    source.setTargetBeanName("service");
    source.setMaxSize(1);
    source.setMaxIdle(1);
    source.setMinIdle(0);
    source.setMaxWait(0);
    source.setTimeBetweenEvictionRunsMillis(-1);
    source.setMinEvictableIdleTimeMillis(3000);
    source.setBlockWhenExhausted(false);
    source.setBeanFactory(factory);

    var proxyFactory = new ProxyFactory();
    proxyFactory.setTarget(new Object());
    proxyFactory.addAdvisor(source.getPoolingConfigMixin());
    var stats = (PoolingConfig) proxyFactory.getProxy();
    PooledService target;
    try {
      target = (PooledService) source.getTarget();
      assertThat(stats.getActiveCount()).isOne();
      assertThat(stats.getIdleCount()).isZero();
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(source::getTarget);
      source.releaseTarget(target);
      assertThat(stats.getActiveCount()).isZero();
      assertThat(stats.getIdleCount()).isOne();
      assertThat(source.getTarget()).isSameAs(target);
      source.releaseTarget(target);
    }
    finally {
      source.destroy();
    }
    assertThat(target.destroyed).isTrue();
  }

  public static class PooledService implements DisposableBean {

    boolean destroyed;

    @Override
    public void destroy() {
      this.destroyed = true;
    }
  }
}
