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

package cn.taketoday.retry.annotation;

import org.junit.Test;

import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.lang.Component;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

public class ProxyApplicationTests {

  private Set<Class<?>> classes = new HashSet<Class<?>>();

  @Test
  // See gh-53
  public void contextLoads() throws Exception {
    int count = count();
    runAndClose();
    runAndClose();
    // Let the JVM catch up
    Thread.sleep(500L);
    runAndClose();
    int base = count();
    runAndClose();
    count = count();
    assertEquals("Class leak", base, count);
    runAndClose();
    count = count();
    assertEquals("Class leak", base, count);
    runAndClose();
    count = count();
    assertEquals("Class leak", base, count);
  }

  @SuppressWarnings("resource")
  private void runAndClose() {
    ConfigurableApplicationContext run = new AnnotationConfigApplicationContext(Empty.class);
    run.close();
    while (run.getParent() != null) {
      ((ConfigurableApplicationContext) run.getParent()).close();
      run = (ConfigurableApplicationContext) run.getParent();
    }
  }

  private int count() {
    URLClassLoader classLoader = (URLClassLoader) getClass().getClassLoader();
    @SuppressWarnings("unchecked")
    Vector<Class<?>> classes = (Vector<Class<?>>) ReflectionTestUtils.getField(classLoader, "classes");
    Set<Class<?>> news = new HashSet<Class<?>>();
    for (Iterator<Class<?>> iterator = classes.iterator(); iterator.hasNext(); ) {
      Class<?> cls = iterator.next();
      if (!this.classes.contains(cls)) {
        news.add(cls);
      }
    }
    this.classes.addAll(classes);
    return classes.size();
  }

  @Configuration
  @EnableRetry(proxyTargetClass = true)
  protected static class Empty {

    @Bean
    public Service service() {
      return new Service();
    }

  }

  @Component
  static class Service {

    @Retryable
    public void handle() {
      System.err.println("Handling");
    }

  }

}
