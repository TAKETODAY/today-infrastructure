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

package infra.context;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.event.ContextClosedEvent;
import infra.context.event.ContextRefreshedEvent;
import infra.core.Ordered;

/**
 * @author Today <br>
 * 2018-11-08 20:31
 */
class ApplicationListenerTests {

  boolean i = false;

  @Test
  void testAddApplicationListener() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {
    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    applicationContext.addApplicationListener((ApplicationListener<ContextRefreshedEvent>) event -> i = true);
    applicationContext.register(ContextCloseMetaInfoListener.class);
    applicationContext.refresh();
    applicationContext.close();
    assert i;
  }

  @Test
  void testLoadMetaInfoListeners() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {
    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    applicationContext.register(ContextCloseMetaInfoListener.class);
    applicationContext.refresh();
    applicationContext.close();
    // auto close
    assert testLoadedMetaInfoListener;
  }

  private static boolean testLoadedMetaInfoListener = false;

  public static class ContextCloseMetaInfoListener implements ApplicationListener<ContextClosedEvent>, Ordered {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
      System.err.println("context is closing");
      testLoadedMetaInfoListener = true;
    }

    @Override
    public int getOrder() {
      return HIGHEST_PRECEDENCE;
    }
  }

}
