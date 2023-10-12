/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.core.Ordered;

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
