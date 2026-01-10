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
