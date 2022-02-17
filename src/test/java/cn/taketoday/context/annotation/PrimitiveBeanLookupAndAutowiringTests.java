/*
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.beans.factory.annotation.Autowired;
import jakarta.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests changes introduced for SPR-8874, allowing beans of primitive types to be looked
 * up via getBean(Class), or to be injected using @Autowired or @Injected or @Resource.
 * Prior to these changes, an attempt to lookup or inject a bean of type boolean would
 * fail because all Framework beans are Objects, regardless of initial type due to the way
 * that ObjectFactory works.
 *
 * Now these attempts to lookup or inject primitive types work, thanks to simple changes
 * in AbstractBeanFactory using ClassUtils#isAssignable methods instead of the built-in
 * Class#isAssignableFrom. The former takes into account primitives and their object
 * wrapper types, whereas the latter does not.
 *
 * @author Chris Beams
 * @since 4.0
 */
public class PrimitiveBeanLookupAndAutowiringTests {

  @Test
  public void primitiveLookupByName() {
    ApplicationContext ctx = new StandardApplicationContext(Config.class);
    boolean b = ctx.getBean("b", boolean.class);
    assertThat(b).isEqualTo(true);
    int i = ctx.getBean("i", int.class);
    assertThat(i).isEqualTo(42);
  }

  @Test
  public void primitiveLookupByType() {
    ApplicationContext ctx = new StandardApplicationContext(Config.class);
    boolean b = ctx.getBean(boolean.class);
    assertThat(b).isEqualTo(true);
    int i = ctx.getBean(int.class);
    assertThat(i).isEqualTo(42);
  }

  @Test
  public void primitiveAutowiredInjection() {
    ApplicationContext ctx =
            new StandardApplicationContext(Config.class, AutowiredComponent.class);
    assertThat(ctx.getBean(AutowiredComponent.class).b).isEqualTo(true);
    assertThat(ctx.getBean(AutowiredComponent.class).i).isEqualTo(42);
  }

  @Test
  public void primitiveResourceInjection() {
    ApplicationContext ctx =
            new StandardApplicationContext(Config.class, ResourceComponent.class);
    assertThat(ctx.getBean(ResourceComponent.class).b).isEqualTo(true);
    assertThat(ctx.getBean(ResourceComponent.class).i).isEqualTo(42);
  }

  @Configuration
  static class Config {
    @Bean
    public boolean b() {
      return true;
    }

    @Bean
    public int i() {
      return 42;
    }
  }

  static class AutowiredComponent {
    @Autowired
    boolean b;
    @Autowired
    int i;
  }

  static class ResourceComponent {
    @Resource
    boolean b;
    @Autowired
    int i;
  }
}
