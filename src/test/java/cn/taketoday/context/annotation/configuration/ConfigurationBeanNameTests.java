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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.AnnotationBeanNamePopulator;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.lang.Component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests ensuring that configuration class bean names as expressed via @Configuration
 * or @Component 'value' attributes are indeed respected, and that customization of bean
 * naming through a BeanNameGenerator strategy works as expected.
 *
 * @author Chris Beams
 * @since 3.1.1
 */
public class ConfigurationBeanNameTests {

  @Test
  public void registerOuterConfig() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(A.class);
    ctx.refresh();
    assertThat(ctx.containsBean("outer")).isTrue();
    assertThat(ctx.containsBean("imported")).isTrue();
    assertThat(ctx.containsBean("nested")).isTrue();
    assertThat(ctx.containsBean("nestedBean")).isTrue();
  }

  @Test
  public void registerNestedConfig() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(A.B.class);
    ctx.refresh();
    assertThat(ctx.containsBean("outer")).isFalse();
    assertThat(ctx.containsBean("imported")).isFalse();
    assertThat(ctx.containsBean("nested")).isTrue();
    assertThat(ctx.containsBean("nestedBean")).isTrue();
  }

  @Test
  public void registerOuterConfig_withBeanNameGenerator() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.setBeanNameGenerator(new AnnotationBeanNamePopulator() {
      @Override
      public String populateName(
              BeanDefinition definition, BeanDefinitionRegistry registry) {
        String name = "custom-" + super.populateName(definition, registry);
        definition.setName(name);
        return name;
      }
    });
    ctx.register(A.class);
    ctx.refresh();
    assertThat(ctx.containsBean("custom-outer")).isTrue();
    assertThat(ctx.containsBean("custom-imported")).isTrue();
    assertThat(ctx.containsBean("custom-nested")).isTrue();
    assertThat(ctx.containsBean("nestedBean")).isTrue();
  }

  @Configuration("outer")
  @Import(C.class)
  static class A {
    @Component("nested")
    static class B {
      @Bean
      public String nestedBean() { return ""; }
    }
  }

  @Configuration("imported")
  static class C {
    @Bean
    public String s() { return "s"; }
  }
}
