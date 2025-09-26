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

package infra.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 */
public class Spr10668Tests {

  @Test
  public void testSelfInjectHierarchy() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ChildConfig.class);
    assertThat(context.getBean(MyComponent.class)).isNotNull();
    assertThat(context.getBean(ParentConfig.class)).isNotNull();
    assertThat(context.getBean(ParentConfig.class).component).isNotNull();
    context.close();
  }

  @Configuration
  public static class ParentConfig {

    @Autowired(required = false)
    MyComponent component;
  }

  @Configuration
//  @Lazy
  public static class ChildConfig extends ParentConfig {

    @Bean
    public MyComponentImpl myComponent() {
      return new MyComponentImpl();
    }
  }

  public interface MyComponent { }

  public static class MyComponentImpl implements MyComponent { }

}
