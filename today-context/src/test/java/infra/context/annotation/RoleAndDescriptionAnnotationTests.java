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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.role.ComponentWithRole;
import infra.context.annotation.role.ComponentWithoutRole;

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
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class);
    ctx.refresh();
    assertThat(ctx.getBeanDefinition("foo").getRole()).isEqualTo(BeanDefinition.ROLE_APPLICATION);
    assertThat(ctx.getBeanDefinition("foo").getDescription()).isNull();
    assertThat(ctx.getBeanDefinition("bar").getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
    assertThat(ctx.getBeanDefinition("bar").getDescription()).isEqualTo("A Bean method with a role");
  }

  @Test
  public void onComponentClass() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ComponentWithoutRole.class, ComponentWithRole.class);
    ctx.refresh();
    assertThat(ctx.getBeanDefinition("componentWithoutRole").getRole()).isEqualTo(BeanDefinition.ROLE_APPLICATION);
    assertThat(ctx.getBeanDefinition("componentWithoutRole").getDescription()).isNull();
    assertThat(ctx.getBeanDefinition("componentWithRole").getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
    assertThat(ctx.getBeanDefinition("componentWithRole").getDescription()).isEqualTo("A Component with a role");
  }

  @Test
  public void viaComponentScanning() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(ctx);
    scanner.scan("infra.context.annotation.role");
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
