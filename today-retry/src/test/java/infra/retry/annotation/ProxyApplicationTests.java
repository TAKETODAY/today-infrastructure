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

package infra.retry.annotation;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyApplicationTests {

  private final CountClassesClassLoader classLoader = new CountClassesClassLoader();

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
    assertThat(count).describedAs("Class leak").isEqualTo(base);
    runAndClose();
    count = count();
    assertThat(count).describedAs("Class leak").isEqualTo(base);
    runAndClose();
    count = count();
    assertThat(count).describedAs("Class leak").isEqualTo(base);
  }

  @SuppressWarnings("resource")
  private void runAndClose() {
    AnnotationConfigApplicationContext run = new AnnotationConfigApplicationContext();
    run.setClassLoader(this.classLoader);
    run.register(Empty.class);
    run.close();
    while (run.getParent() != null) {
      ((ConfigurableApplicationContext) run.getParent()).close();
      run = (AnnotationConfigApplicationContext) run.getParent();
    }
  }

  private int count() {
    return this.classLoader.classes.size();
  }

  private static class CountClassesClassLoader extends URLClassLoader {

    private final Set<Class<?>> classes = new HashSet<>();

    public CountClassesClassLoader() {
      super(new URL[0], ProxyApplicationTests.class.getClassLoader());
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      Class<?> type = super.loadClass(name);
      classes.add(type);
      return type;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      Class<?> type = super.loadClass(name, resolve);
      classes.add(type);
      return type;
    }

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
