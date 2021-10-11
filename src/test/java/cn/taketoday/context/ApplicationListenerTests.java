/**
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.core.Ordered;

/**
 * @author Today <br>
 * 2018-11-08 20:31
 */
class ApplicationListenerTests {

  boolean i = false;

  @Test
  void testAddApplicationListener() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

    try (StandardApplicationContext applicationContext = new StandardApplicationContext()) {
      applicationContext.addApplicationListener(new ApplicationListener<ContextStartedEvent>() {

        @Override
        public void onApplicationEvent(ContextStartedEvent event) {
          i = true;
          System.err.println(i);
        }
      });

      applicationContext.scan("");

      assert i;
    }
  }

  @Test
  void testLoadMetaInfoListeners() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {
    try (StandardApplicationContext applicationContext = new StandardApplicationContext()) {
      applicationContext.scan("");
    }
    // auto close
    assert testLoadedMetaInfoListener;
  }

  private static boolean testLoadedMetaInfoListener = false;

  public static class ContextCloseMetaInfoListener implements ApplicationListener<ContextCloseEvent>, Ordered {

    @Override
    public void onApplicationEvent(ContextCloseEvent event) {
      System.err.println("context is closing");
      testLoadedMetaInfoListener = true;
    }

    @Override
    public int getOrder() {
      return HIGHEST_PRECEDENCE;
    }
  }

}
