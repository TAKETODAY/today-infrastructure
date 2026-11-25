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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.beans.factory.DisposableBean;
import infra.beans.testfixture.beans.DerivedTestBean;
import infra.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author TODAY 2021/10/2 15:51
 */
class DefaultSingletonBeanRegistryTests {

  private final DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();

  @Test
  void singletons() {
    AtomicBoolean tbFlag = new AtomicBoolean();
    beanRegistry.addSingletonCallback("tb", instance -> tbFlag.set(true));
    TestBean tb = new TestBean();
    beanRegistry.registerSingleton("tb", tb);
    assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
    assertThat(tbFlag.get()).isTrue();

    AtomicBoolean tb2Flag = new AtomicBoolean();
    beanRegistry.addSingletonCallback("tb2", instance -> tb2Flag.set(true));
    TestBean tb2 = (TestBean) beanRegistry.getSingleton("tb2", TestBean::new);
    assertThat(beanRegistry.getSingleton("tb2")).isSameAs(tb2);
    assertThat(tb2Flag.get()).isTrue();

    TestBean tb3 = (TestBean) beanRegistry.getSingleton("tb3", () -> {
      TestBean newTb = new TestBean();
      beanRegistry.registerSingleton("tb3", newTb);
      return newTb;
    });
    assertThat(beanRegistry.getSingleton("tb3")).isSameAs(tb3);

    assertThat(beanRegistry.getSingletonCount()).isEqualTo(3);
    assertThat(beanRegistry.getSingletonNames()).containsExactly("tb", "tb2", "tb3");

    beanRegistry.destroySingletons();
    assertThat(beanRegistry.getSingletonCount()).isZero();
    assertThat(beanRegistry.getSingletonNames()).isEmpty();
  }

  @Test
  void disposableBean() {
    DerivedTestBean tb = new DerivedTestBean();
    beanRegistry.registerSingleton("tb", tb);
    beanRegistry.registerDisposableBean("tb", tb);
    assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);

    assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
    assertThat(beanRegistry.getSingletonCount()).isEqualTo(1);
    assertThat(beanRegistry.getSingletonNames()).containsExactly("tb");
    assertThat(tb.wasDestroyed()).isFalse();

    beanRegistry.destroySingletons();
    assertThat(beanRegistry.getSingletonCount()).isZero();
    assertThat(beanRegistry.getSingletonNames()).isEmpty();
    assertThat(tb.wasDestroyed()).isTrue();
  }

  @Test
  void dependentRegistration() {
    beanRegistry.registerDependentBean("a", "b");
    beanRegistry.registerDependentBean("b", "c");
    beanRegistry.registerDependentBean("c", "b");
    assertThat(beanRegistry.isDependent("a", "b")).isTrue();
    assertThat(beanRegistry.isDependent("b", "c")).isTrue();
    assertThat(beanRegistry.isDependent("c", "b")).isTrue();
    assertThat(beanRegistry.isDependent("a", "c")).isTrue();
    assertThat(beanRegistry.isDependent("c", "a")).isFalse();
    assertThat(beanRegistry.isDependent("b", "a")).isFalse();
    assertThat(beanRegistry.isDependent("a", "a")).isFalse();
    assertThat(beanRegistry.isDependent("b", "b")).isTrue();
    assertThat(beanRegistry.isDependent("c", "c")).isTrue();
  }

  @Test
  void exposesEarlySingletonsAndProtectsCreationState() {
    TestBean early = new TestBean();
    beanRegistry.addSingletonFactory("early", () -> early);
    beanRegistry.beforeSingletonCreation("early");

    assertThat(beanRegistry.getSingleton("early", false)).isNull();
    assertThat(beanRegistry.getSingleton("early", true)).isSameAs(early);
    assertThat(beanRegistry.getSingleton("early", true)).isSameAs(early);
    assertThat(beanRegistry.isSingletonCurrentlyInCreation("early")).isTrue();
    assertThat(beanRegistry.getSingletonNames()).contains("early");

    assertThatThrownBy(() -> beanRegistry.beforeSingletonCreation("early"))
            .isInstanceOf(BeanCurrentlyInCreationException.class);
    beanRegistry.afterSingletonCreation("early");
    assertThat(beanRegistry.isSingletonCurrentlyInCreation("early")).isFalse();
    assertThatIllegalStateException().isThrownBy(() -> beanRegistry.afterSingletonCreation("early"));
  }

  @Test
  void destroysDependentsAndContainedBeansBeforeRemovingTheSingleton() {
    List<String> destroyed = new ArrayList<>();
    beanRegistry.registerSingleton("parent", new Object());
    beanRegistry.registerSingleton("dependent", new Object());
    beanRegistry.registerSingleton("contained", new Object());
    beanRegistry.registerDisposableBean("parent", disposable("parent", destroyed));
    beanRegistry.registerDisposableBean("dependent", disposable("dependent", destroyed));
    beanRegistry.registerDisposableBean("contained", disposable("contained", destroyed));
    beanRegistry.registerDependentBean("parent", "dependent");
    beanRegistry.registerContainedBean("contained", "parent");

    assertThat(beanRegistry.hasDependentBean("parent")).isTrue();
    assertThat(beanRegistry.getDependentBeans("parent")).containsExactly("dependent");
    assertThat(beanRegistry.getDependenciesForBean("dependent")).containsExactly("parent");

    beanRegistry.destroySingleton("parent");

    assertThat(destroyed).containsExactly("dependent", "parent", "contained");
    assertThat(beanRegistry.getSingleton("parent")).isNull();
    assertThat(beanRegistry.getSingleton("dependent")).isNull();
    assertThat(beanRegistry.getSingleton("contained")).isNull();
    assertThat(beanRegistry.hasDependentBean("parent")).isFalse();
  }

  @Test
  void toleratesDisposableBeanFailuresAndRejectsDuplicateSingletons() {
    Object singleton = new Object();
    beanRegistry.registerSingleton("singleton", singleton);
    assertThatThrownBy(() -> beanRegistry.registerSingleton("singleton", new Object()))
            .isInstanceOf(IllegalStateException.class);

    beanRegistry.registerDisposableBean("singleton", new DisposableBean() {
      @Override
      public void destroy() {
        throw new IllegalStateException("destroy failed");
      }
    });
    beanRegistry.destroySingleton("singleton");

    assertThat(beanRegistry.getSingleton("singleton")).isNull();
    assertThat(beanRegistry.containsSingleton("singleton")).isFalse();
  }

  private DisposableBean disposable(String name, List<String> destroyed) {
    return () -> destroyed.add(name);
  }

}
