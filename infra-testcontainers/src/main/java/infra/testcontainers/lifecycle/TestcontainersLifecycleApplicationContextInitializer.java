/*
 * Copyright 2012-present the original author or authors.
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

package infra.testcontainers.lifecycle;

import org.testcontainers.lifecycle.Startable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;

/**
 * {@link ApplicationContextInitializer} to manage the lifecycle of {@link Startable
 * startable containers}.
 *
 * @author Phillip Webb
 * @since 5.0
 */
public class TestcontainersLifecycleApplicationContextInitializer implements ApplicationContextInitializer {

  private static final Set<ConfigurableApplicationContext> applied = Collections.newSetFromMap(new WeakHashMap<>());

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    synchronized(applied) {
      if (!applied.add(applicationContext)) {
        return;
      }
    }
    ConfigurableBeanFactory beanFactory = applicationContext.getBeanFactory();
    applicationContext.addBeanFactoryPostProcessor(new TestcontainersLifecycleBeanFactoryPostProcessor());
    TestcontainersStartup startup = TestcontainersStartup.get(applicationContext.getEnvironment());
    TestcontainersLifecycleBeanPostProcessor beanPostProcessor = new TestcontainersLifecycleBeanPostProcessor(
            beanFactory, startup);
    beanFactory.addBeanPostProcessor(beanPostProcessor);
  }

}
