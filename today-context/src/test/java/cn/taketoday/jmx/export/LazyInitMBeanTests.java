/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jmx.export;

import org.junit.jupiter.api.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class LazyInitMBeanTests {

  @Test
  public void invokeOnLazyInitBean() throws Exception {
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("cn/taketoday/jmx/export/lazyInit.xml");
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
    try {
      MBeanServer server = (MBeanServer) ctx.getBean("server");
      ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean2");
      String name = (String) server.getAttribute(oname, "Name");
      assertThat(name).as("Invalid name returned").isEqualTo("foo");
    }
    finally {
      ctx.close();
    }
  }

}
