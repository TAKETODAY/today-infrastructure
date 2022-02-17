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

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.context.annotation.role.ComponentWithRole;
import cn.taketoday.context.annotation.role.ComponentWithoutRole;
import cn.taketoday.context.loader.ClassPathBeanDefinitionScanner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests the use of the @Role and @Description annotation on @Bean methods and @Component classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 4.0
 */
public class RoleAndDescriptionAnnotationTests {

  @Test
  public void onBeanMethod() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(Config.class);
    ctx.refresh();
    assertThat(ctx.getBeanDefinition("foo").getRole()).isEqualTo(BeanDefinition.ROLE_APPLICATION);
    assertThat(ctx.getBeanDefinition("foo").getDescription()).isNull();
    assertThat(ctx.getBeanDefinition("bar").getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
    assertThat(ctx.getBeanDefinition("bar").getDescription()).isEqualTo("A Bean method with a role");
  }

  @Test
  public void onComponentClass() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(ComponentWithoutRole.class, ComponentWithRole.class);
    ctx.refresh();
    assertThat(ctx.getBeanDefinition("componentWithoutRole").getRole()).isEqualTo(BeanDefinition.ROLE_APPLICATION);
    assertThat(ctx.getBeanDefinition("componentWithoutRole").getDescription()).isNull();
    assertThat(ctx.getBeanDefinition("componentWithRole").getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
    assertThat(ctx.getBeanDefinition("componentWithRole").getDescription()).isEqualTo("A Component with a role");
  }

  @Test
  public void viaComponentScanning() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(ctx);
    scanner.scan("cn.taketoday.context.annotation.role");
    ctx.refresh();

    assertThat(ctx.getBeanDefinition("componentWithoutRole").getRole()).isEqualTo(BeanDefinition.ROLE_APPLICATION);
    assertThat(ctx.getBeanDefinition("componentWithoutRole").getDescription()).isNull();
    assertThat(ctx.getBeanDefinition("componentWithRole").getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
    assertThat(ctx.getBeanDefinition("componentWithRole").getDescription()).isEqualTo("A Component with a role");
  }

  @Configuration
  static class Config {
    @Bean
    public String foo() {
      return "foo";
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Description("A Bean method with a role")
    public String bar() {
      return "bar";
    }
  }

}
