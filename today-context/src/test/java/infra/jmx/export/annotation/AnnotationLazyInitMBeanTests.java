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

package infra.jmx.export.annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import infra.context.support.ClassPathXmlApplicationContext;
import infra.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@Execution(ExecutionMode.SAME_THREAD)
class AnnotationLazyInitMBeanTests {

  @Test
  void lazyNaming() throws Exception {
    var ctx = new ClassPathXmlApplicationContext(
            "infra/jmx/export/annotation/lazyNaming.xml");
    MBeanServer server = (MBeanServer) ctx.getBean("server");
    ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean4");
    assertThat(server.getObjectInstance(oname)).isNotNull();
    String name = (String) server.getAttribute(oname, "Name");
    assertThat(name).as("Invalid name returned").isEqualTo("TEST");
  }

  @Test
  void lazyAssembling() throws Exception {
    try {
      System.setProperty("domain", "bean");
      var ctx = new ClassPathXmlApplicationContext(
              "infra/jmx/export/annotation/lazyAssembling.xml");
      MBeanServer server = (MBeanServer) ctx.getBean("server");

      ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean4");
      assertThat(server.getObjectInstance(oname)).isNotNull();
      String name = (String) server.getAttribute(oname, "Name");
      assertThat(name).as("Invalid name returned").isEqualTo("TEST");

      oname = ObjectNameManager.getInstance("bean:name=testBean5");
      assertThat(server.getObjectInstance(oname)).isNotNull();
      name = (String) server.getAttribute(oname, "Name");
      assertThat(name).as("Invalid name returned").isEqualTo("FACTORY");

      oname = ObjectNameManager.getInstance("spring:mbean=true");
      assertThat(server.getObjectInstance(oname)).isNotNull();
      name = (String) server.getAttribute(oname, "Name");
      assertThat(name).as("Invalid name returned").isEqualTo("Rob Harrop");

      oname = ObjectNameManager.getInstance("spring:mbean=another");
      assertThat(server.getObjectInstance(oname)).isNotNull();
      name = (String) server.getAttribute(oname, "Name");
      assertThat(name).as("Invalid name returned").isEqualTo("Juergen Hoeller");
    }
    finally {
      System.clearProperty("domain");
    }
  }

  @Test
  void componentScan() throws Exception {
    var ctx = new ClassPathXmlApplicationContext(
            "infra/jmx/export/annotation/componentScan.xml");
    MBeanServer server = (MBeanServer) ctx.getBean("server");
    ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean4");
    assertThat(server.getObjectInstance(oname)).isNotNull();
    String name = (String) server.getAttribute(oname, "Name");
    assertThat(name).isNull();
  }

}
