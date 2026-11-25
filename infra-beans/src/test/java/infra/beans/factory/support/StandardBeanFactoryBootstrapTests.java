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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.SmartInitializingSingleton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD;

@Timeout(value = 10, threadMode = SEPARATE_THREAD)
class StandardBeanFactoryBootstrapTests {

  @Test
  void initializesPrerequisitesOnMainThreadBeforeRunningFactoryMethodInBackground() {
    var factory = new StandardBeanFactory();
    Thread main = Thread.currentThread();
    List<Thread> prerequisiteThreads = new ArrayList<>();
    List<Thread> callbackThreads = new ArrayList<>();
    String workerName = "bean-bootstrap-test";
    var executor = Executors.newSingleThreadExecutor(task -> {
      var thread = new Thread(task, workerName);
      thread.setDaemon(true);
      return thread;
    });
    factory.setBootstrapExecutor(executor);
    try {
      var productDefinition = new RootBeanDefinition();
      productDefinition.setFactoryBeanName("creator");
      productDefinition.setFactoryMethodName("create");
      productDefinition.setBackgroundInit(true);
      productDefinition.setDependsOn("prerequisite");
      factory.registerBeanDefinition("product", productDefinition);
      factory.registerBeanDefinition("prerequisite", new RootBeanDefinition(Object.class, () -> {
        prerequisiteThreads.add(Thread.currentThread());
        return new Object();
      }));
      factory.registerBeanDefinition("creator", new RootBeanDefinition(ProductFactory.class, () -> {
        prerequisiteThreads.add(Thread.currentThread());
        return new ProductFactory(callbackThreads);
      }));

      factory.preInstantiateSingletons();

      assertThat(prerequisiteThreads).containsExactly(main, main);
      assertThat(factory.containsSingleton("product")).isTrue();
      Product product = factory.getBean("product", Product.class);
      assertThat(product.creationThread()).isNotSameAs(main);
      assertThat(product.creationThread().getName()).isEqualTo(workerName);
      assertThat(callbackThreads).containsExactly(main);
    }
    finally {
      executor.shutdownNow();
    }
  }

  @Test
  void fallsBackToMainThreadWithoutBootstrapExecutor() {
    var factory = new StandardBeanFactory();
    var definition = new RootBeanDefinition(Thread.class, Thread::currentThread);
    definition.setBackgroundInit(true);
    factory.registerBeanDefinition("background", definition);

    factory.preInstantiateSingletons();

    assertThat(factory.containsSingleton("background")).isTrue();
    assertThat(factory.getBean("background")).isSameAs(Thread.currentThread());
  }

  @Test
  void propagatesBackgroundCreationFailureAndLeavesFailedSingletonUnregistered() {
    var factory = new StandardBeanFactory();
    var executor = Executors.newSingleThreadExecutor();
    factory.setBootstrapExecutor(executor);
    try {
      var failure = new IllegalStateException("cannot initialize");
      var definition = new RootBeanDefinition(Object.class, () -> { throw failure; });
      definition.setBackgroundInit(true);
      factory.registerBeanDefinition("broken", definition);

      assertThatExceptionOfType(BeanCreationException.class).isThrownBy(factory::preInstantiateSingletons)
              .withMessageContaining("broken").satisfies(ex -> assertThat(ex).hasRootCause(failure));
      assertThat(factory.containsSingleton("broken")).isFalse();
      assertThat(factory.isCurrentlyInCreation("broken")).isFalse();
    }
    finally {
      executor.shutdownNow();
    }
  }

  record ProductFactory(List<Thread> callbackThreads) {

    public Product create() {
      return new Product(Thread.currentThread(), this.callbackThreads);
    }

  }

  record Product(Thread creationThread, List<Thread> callbackThreads) implements SmartInitializingSingleton {

    @Override
    public void afterSingletonsInstantiated() {
      this.callbackThreads.add(Thread.currentThread());
    }

  }

}
