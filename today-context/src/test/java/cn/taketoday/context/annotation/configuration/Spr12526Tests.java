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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import jakarta.annotation.Resource;

import static cn.taketoday.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;
import static cn.taketoday.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marcin Piela
 * @author Juergen Hoeller
 */
class Spr12526Tests {

  @Test
  void testInjection() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(TestContext.class);
    CustomCondition condition = ctx.getBean(CustomCondition.class);

    condition.setCondition(true);
    FirstService firstService = (FirstService) ctx.getBean(Service.class);
    assertThat(firstService.getDependency()).as("FirstService.dependency is null").isNotNull();

    condition.setCondition(false);
    SecondService secondService = (SecondService) ctx.getBean(Service.class);
    assertThat(secondService.getDependency()).as("SecondService.dependency is null").isNotNull();

    ctx.close();
  }

  @Configuration
  static class TestContext {

    @Bean
    @Scope(SCOPE_SINGLETON)
    CustomCondition condition() {
      return new CustomCondition();
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    Service service(CustomCondition condition) {
      return (condition.check() ? new FirstService() : new SecondService());
    }

    @Bean
    DependencyOne dependencyOne() {
      return new DependencyOne();
    }

    @Bean
    DependencyTwo dependencyTwo() {
      return new DependencyTwo();
    }
  }

  public static class CustomCondition {

    private boolean condition;

    public boolean check() {
      return condition;
    }

    public void setCondition(boolean value) {
      this.condition = value;
    }
  }

  public interface Service {

    void doStuff();
  }

  public static class FirstService implements Service {

    private DependencyOne dependency;

    @Override
    public void doStuff() {
      if (dependency == null) {
        throw new IllegalStateException("FirstService: dependency is null");
      }
    }

    @Resource(name = "dependencyOne")
    public void setDependency(DependencyOne dependency) {
      this.dependency = dependency;
    }

    public DependencyOne getDependency() {
      return dependency;
    }
  }

  public static class SecondService implements Service {

    private DependencyTwo dependency;

    @Override
    public void doStuff() {
      if (dependency == null) {
        throw new IllegalStateException("SecondService: dependency is null");
      }
    }

    @Resource(name = "dependencyTwo")
    public void setDependency(DependencyTwo dependency) {
      this.dependency = dependency;
    }

    public DependencyTwo getDependency() {
      return dependency;
    }
  }

  public static class DependencyOne {
  }

  public static class DependencyTwo {
  }

}
