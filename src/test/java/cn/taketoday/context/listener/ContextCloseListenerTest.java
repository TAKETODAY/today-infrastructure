/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.listener;

import org.junit.Test;

import javax.annotation.PreDestroy;

import cn.taketoday.beans.factory.StandardBeanFactory;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.event.ContextCloseListener;

/**
 * @author TODAY <br>
 * 2020-02-24 22:44
 */
public class ContextCloseListenerTest {

  static class BEAN {
    @PreDestroy
    public void destory() throws Exception {
      throw new Exception("test destory error");
    }
  }

  @Import(BEAN.class)
  static class ContextCloseListenerConfig {

  }

  @Test
  public void testContextCloseListener() {

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      StandardBeanFactory beanFactory = context.getBeanFactory();
      beanFactory.registerSingleton("test", new BEAN());

      context.addApplicationListener(new ContextCloseListener());

      context.importBeans(ContextCloseListenerConfig.class);

      context.refresh();
    }

  }

}
