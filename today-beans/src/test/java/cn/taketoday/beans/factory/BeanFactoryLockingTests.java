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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 */
class BeanFactoryLockingTests {

  @Test
  void fallbackForThreadDuringInitialization() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("bean1",
            new RootBeanDefinition(ThreadDuringInitialization.class));
    beanFactory.registerBeanDefinition("bean2",
            new RootBeanDefinition(TestBean.class, () -> new TestBean("tb")));
    beanFactory.getBean(ThreadDuringInitialization.class);
  }

  static class ThreadDuringInitialization implements BeanFactoryAware, InitializingBean {

    private BeanFactory beanFactory;

    private volatile boolean initialized;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
      Thread thread = new Thread(() -> {
        // Fail for circular reference from other thread
        assertThatExceptionOfType(BeanCurrentlyInCreationException.class).isThrownBy(() ->
                beanFactory.getBean(ThreadDuringInitialization.class));
        // Leniently create unrelated other bean outside of singleton lock
        assertThat(beanFactory.getBean(TestBean.class).getName()).isEqualTo("tb");
        // Creation attempt in other thread was successful
        initialized = true;
      });
      thread.start();
      thread.join();
      if (!initialized) {
        throw new IllegalStateException("Thread not executed");
      }
    }
  }

}
