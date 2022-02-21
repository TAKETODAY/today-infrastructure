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

package cn.taketoday.web.context;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.Resource;
import cn.taketoday.web.context.support.ServletContextAttributeExporter;
import cn.taketoday.web.context.support.ServletContextAttributeFactoryBean;
import cn.taketoday.web.context.support.ServletContextParameterFactoryBean;
import cn.taketoday.web.context.support.ServletContextResource;
import cn.taketoday.web.context.support.ServletContextResourceLoader;
import cn.taketoday.web.context.support.ServletContextResourcePatternLoader;
import cn.taketoday.web.context.support.StaticWebApplicationContext;
import cn.taketoday.web.mock.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/21 16:55
 */
public class ServletContextSupportTests {

  @Test
  @SuppressWarnings("resource")
  public void testServletContextAttributeFactoryBean() {
    MockServletContext sc = new MockServletContext();
    sc.setAttribute("myAttr", "myValue");

    StaticWebApplicationContext wac = new StaticWebApplicationContext();
    wac.setServletContext(sc);
    PropertyValues pvs = new PropertyValues();
    pvs.add("attributeName", "myAttr");
    wac.registerSingleton("importedAttr", ServletContextAttributeFactoryBean.class, pvs);
    wac.refresh();

    Object value = wac.getBean("importedAttr");
    assertThat(value).isEqualTo("myValue");
  }

  @Test
  @SuppressWarnings("resource")
  public void testServletContextAttributeFactoryBeanWithAttributeNotFound() {
    MockServletContext sc = new MockServletContext();

    StaticWebApplicationContext wac = new StaticWebApplicationContext();
    wac.setServletContext(sc);
    PropertyValues pvs = new PropertyValues();
    pvs.add("attributeName", "myAttr");
    wac.registerSingleton("importedAttr", ServletContextAttributeFactoryBean.class, pvs);

    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    wac::refresh)
            .withCauseInstanceOf(IllegalStateException.class)
            .withMessageContaining("myAttr");
  }

  @Test
  @SuppressWarnings("resource")
  public void testServletContextParameterFactoryBean() {
    MockServletContext sc = new MockServletContext();
    sc.addInitParameter("myParam", "myValue");

    StaticWebApplicationContext wac = new StaticWebApplicationContext();
    wac.setServletContext(sc);
    PropertyValues pvs = new PropertyValues();
    pvs.add("initParamName", "myParam");
    wac.registerSingleton("importedParam", ServletContextParameterFactoryBean.class, pvs);
    wac.refresh();

    Object value = wac.getBean("importedParam");
    assertThat(value).isEqualTo("myValue");
  }

  @Test
  @SuppressWarnings("resource")
  public void testServletContextParameterFactoryBeanWithAttributeNotFound() {
    MockServletContext sc = new MockServletContext();

    StaticWebApplicationContext wac = new StaticWebApplicationContext();
    wac.setServletContext(sc);
    PropertyValues pvs = new PropertyValues();
    pvs.add("initParamName", "myParam");
    wac.registerSingleton("importedParam", ServletContextParameterFactoryBean.class, pvs);

    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    wac::refresh)
            .withCauseInstanceOf(IllegalStateException.class)
            .withMessageContaining("myParam");
  }

  @Test
  public void testServletContextAttributeExporter() {
    TestBean tb = new TestBean();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("attr1", "value1");
    attributes.put("attr2", tb);

    MockServletContext sc = new MockServletContext();
    ServletContextAttributeExporter exporter = new ServletContextAttributeExporter();
    exporter.setAttributes(attributes);
    exporter.setServletContext(sc);

    assertThat(sc.getAttribute("attr1")).isEqualTo("value1");
    assertThat(sc.getAttribute("attr2")).isSameAs(tb);
  }

  @Test
  public void testServletContextResourceLoader() {
    MockServletContext sc = new MockServletContext("classpath:cn/taketoday/web/context");
    ServletContextResourceLoader rl = new ServletContextResourceLoader(sc);
    assertThat(rl.getResource("/WEB-INF/web.xml").exists()).isTrue();
    assertThat(rl.getResource("WEB-INF/web.xml").exists()).isTrue();
    assertThat(rl.getResource("../context/WEB-INF/web.xml").exists()).isTrue();
    assertThat(rl.getResource("/../context/WEB-INF/web.xml").exists()).isTrue();
  }

  @Test
  public void testServletContextResourcePatternLoader() throws IOException {
    final Set<String> paths = new HashSet<>();
    paths.add("/WEB-INF/context1.xml");
    paths.add("/WEB-INF/context2.xml");

    MockServletContext sc = new MockServletContext("classpath:cn/taketoday/web/context") {
      @Override
      public Set<String> getResourcePaths(String path) {
        if ("/WEB-INF/".equals(path)) {
          return paths;
        }
        return null;
      }
    };

    ServletContextResourcePatternLoader rpr = new ServletContextResourcePatternLoader(sc);
    Resource[] found = rpr.getResourcesArray("/WEB-INF/*.xml");
    Set<String> foundPaths = new HashSet<>();
    for (Resource resource : found) {
      foundPaths.add(((ServletContextResource) resource).getPath());
    }
    assertThat(foundPaths.size()).isEqualTo(2);
    assertThat(foundPaths.contains("/WEB-INF/context1.xml")).isTrue();
    assertThat(foundPaths.contains("/WEB-INF/context2.xml")).isTrue();
  }

  @Test
  public void testServletContextResourcePatternLoaderWithPatternPath() throws IOException {
    final Set<String> dirs = new HashSet<>();
    dirs.add("/WEB-INF/mydir1/");
    dirs.add("/WEB-INF/mydir2/");

    MockServletContext sc = new MockServletContext("classpath:cn/taketoday/web/context") {
      @Override
      public Set<String> getResourcePaths(String path) {
        if ("/WEB-INF/".equals(path)) {
          return dirs;
        }
        if ("/WEB-INF/mydir1/".equals(path)) {
          return Collections.singleton("/WEB-INF/mydir1/context1.xml");
        }
        if ("/WEB-INF/mydir2/".equals(path)) {
          return Collections.singleton("/WEB-INF/mydir2/context2.xml");
        }
        return null;
      }
    };

    ServletContextResourcePatternLoader rpr = new ServletContextResourcePatternLoader(sc);
    Resource[] found = rpr.getResourcesArray("/WEB-INF/*/*.xml");
    Set<String> foundPaths = new HashSet<>();
    for (Resource resource : found) {
      foundPaths.add(((ServletContextResource) resource).getPath());
    }
    assertThat(foundPaths.size()).isEqualTo(2);
    assertThat(foundPaths.contains("/WEB-INF/mydir1/context1.xml")).isTrue();
    assertThat(foundPaths.contains("/WEB-INF/mydir2/context2.xml")).isTrue();
  }

  @Test
  public void testServletContextResourcePatternLoaderWithUnboundedPatternPath() throws IOException {
    final Set<String> dirs = new HashSet<>();
    dirs.add("/WEB-INF/mydir1/");
    dirs.add("/WEB-INF/mydir2/");

    final Set<String> paths = new HashSet<>();
    paths.add("/WEB-INF/mydir2/context2.xml");
    paths.add("/WEB-INF/mydir2/mydir3/");

    MockServletContext sc = new MockServletContext("classpath:cn/taketoday/web/context") {
      @Override
      public Set<String> getResourcePaths(String path) {
        if ("/WEB-INF/".equals(path)) {
          return dirs;
        }
        if ("/WEB-INF/mydir1/".equals(path)) {
          return Collections.singleton("/WEB-INF/mydir1/context1.xml");
        }
        if ("/WEB-INF/mydir2/".equals(path)) {
          return paths;
        }
        if ("/WEB-INF/mydir2/mydir3/".equals(path)) {
          return Collections.singleton("/WEB-INF/mydir2/mydir3/context3.xml");
        }
        return null;
      }
    };

    ServletContextResourcePatternLoader rpr = new ServletContextResourcePatternLoader(sc);
    Resource[] found = rpr.getResourcesArray("/WEB-INF/**/*.xml");
    Set<String> foundPaths = new HashSet<>();
    for (Resource resource : found) {
      foundPaths.add(((ServletContextResource) resource).getPath());
    }
    assertThat(foundPaths.size()).isEqualTo(3);
    assertThat(foundPaths.contains("/WEB-INF/mydir1/context1.xml")).isTrue();
    assertThat(foundPaths.contains("/WEB-INF/mydir2/context2.xml")).isTrue();
    assertThat(foundPaths.contains("/WEB-INF/mydir2/mydir3/context3.xml")).isTrue();
  }

  @Test
  public void testServletContextResourcePatternLoaderWithAbsolutePaths() throws IOException {
    final Set<String> paths = new HashSet<>();
    paths.add("C:/webroot/WEB-INF/context1.xml");
    paths.add("C:/webroot/WEB-INF/context2.xml");
    paths.add("C:/webroot/someOtherDirThatDoesntContainPath");

    MockServletContext sc = new MockServletContext("classpath:cn/taketoday/web/context") {
      @Override
      public Set<String> getResourcePaths(String path) {
        if ("/WEB-INF/".equals(path)) {
          return paths;
        }
        return null;
      }
    };

    ServletContextResourcePatternLoader rpr = new ServletContextResourcePatternLoader(sc);
    Resource[] found = rpr.getResourcesArray("/WEB-INF/*.xml");
    Set<String> foundPaths = new HashSet<>();
    for (Resource resource : found) {
      foundPaths.add(((ServletContextResource) resource).getPath());
    }
    assertThat(foundPaths.size()).isEqualTo(2);
    assertThat(foundPaths.contains("/WEB-INF/context1.xml")).isTrue();
    assertThat(foundPaths.contains("/WEB-INF/context2.xml")).isTrue();
  }

}
