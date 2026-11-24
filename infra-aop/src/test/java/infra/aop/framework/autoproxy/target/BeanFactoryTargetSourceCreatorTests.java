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

package infra.aop.framework.autoproxy.target;

import org.junit.jupiter.api.Test;

import infra.aop.target.CommonsPool2TargetSource;
import infra.aop.target.LazyInitTargetSource;
import infra.aop.target.PrototypeTargetSource;
import infra.aop.target.ThreadLocalTargetSource;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.mock;

class BeanFactoryTargetSourceCreatorTests {

  @Test
  void quickCreatorSelectsTargetSourceAndKeepsParentDefinitionsUnchanged() throws Exception {
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("!service", new RootBeanDefinition(Service.class));
    factory.registerBeanDefinition(":service", new RootBeanDefinition(Service.class));
    factory.registerBeanDefinition("%service", new RootBeanDefinition(Service.class));
    var creator = new QuickTargetSourceCreator();
    creator.setBeanFactory(factory);

    var prototype = (PrototypeTargetSource) creator.getTargetSource(Service.class, "!service");
    var pooled = (CommonsPool2TargetSource) creator.getTargetSource(Service.class, ":service");
    var threadLocal = (ThreadLocalTargetSource) creator.getTargetSource(Service.class, "%service");
    try {
      assertThat(prototype.getTarget()).isNotSameAs(prototype.getTarget());
      assertThat(pooled.getMaxSize()).isEqualTo(25);
      assertThat(threadLocal.getTarget()).isSameAs(threadLocal.getTarget());
      assertThat(factory.getBeanDefinition("!service").isSingleton()).isTrue();
      assertThat(creator.getTargetSource(Service.class, "service")).isNull();
    }
    finally {
      threadLocal.destroy();
      pooled.destroy();
      creator.destroy();
    }
  }

  @Test
  void lazyCreatorPreservesSingletonScopeAndDestroysItsOwnTarget() throws Exception {
    var definition = new RootBeanDefinition(Service.class);
    definition.setLazyInit(true);
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("lazy", definition);
    factory.registerBeanDefinition("eager", new RootBeanDefinition(Service.class));
    var creator = new LazyInitTargetSourceCreator();
    creator.setBeanFactory(factory);

    assertThat(creator.getTargetSource(Service.class, "eager")).isNull();
    var source = (LazyInitTargetSource) creator.getTargetSource(Service.class, "lazy");
    assertThat(factory.containsSingleton("lazy")).isFalse();
    assertThat(creator.getInternalBeanFactoryForBean("lazy").containsSingleton("lazy")).isFalse();

    var target = (Service) source.getTarget();
    assertThat(source.getTarget()).isSameAs(target);
    assertThat(factory.containsSingleton("lazy")).isFalse();
    creator.destroy();
    assertThat(target.destroyed).isTrue();
  }

  @Test
  void creatorRequiresConfigurableFactoryWhenCreatingCustomTarget() {
    var creator = new QuickTargetSourceCreator();
    assertThatIllegalStateException().isThrownBy(() -> creator.setBeanFactory(mock(BeanFactory.class)))
            .withMessageContaining("ConfigurableBeanFactory");
    assertThatIllegalStateException().isThrownBy(() -> creator.getTargetSource(Service.class, "!service"))
            .withMessage("BeanFactory not set");
  }

  static class Service implements DisposableBean {

    boolean destroyed;

    @Override
    public void destroy() {
      this.destroyed = true;
    }
  }

}
