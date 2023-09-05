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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.EnableDependencyInjection;
import cn.taketoday.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/5 15:53
 */
class DisableDependencyInjectionTests {

  @Test
  void isDisableAllDependencyInjection() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(OuterConfig.class);
      context.refresh();

      assertThat(context.getBeanDefinition("string").isEnableDependencyInjection()).isFalse();
      assertThat(context.getBeanDefinition("disabled").isEnableDependencyInjection()).isFalse();
      assertThat(context.getBeanDefinition("innerLevel22Disabled").isEnableDependencyInjection()).isFalse();
      assertThat(context.getBeanDefinition("enableDI").isEnableDependencyInjection()).isTrue();

      assertThat(context.getBeanDefinition(OuterConfig.class).isEnableDependencyInjection()).isTrue();
      assertThat(context.getBeanDefinition(OuterConfig.InnerLevel1.class).isEnableDependencyInjection()).isFalse();
      assertThat(context.getBeanDefinition(OuterConfig.InnerLevel1.InnerLevel2.class).isEnableDependencyInjection()).isFalse();
      assertThat(context.getBeanDefinition(OuterConfig.InnerLevel1.InnerLevel2.InnerLevel3.class).isEnableDependencyInjection()).isTrue();
      assertThat(context.getBeanDefinition(OuterConfig.InnerLevel1.InnerLevel22.class).isEnableDependencyInjection()).isFalse();
    }

  }

  @DisableAllDependencyInjection
  @Configuration(proxyBeanMethods = false)
  static class OuterConfig {

    @Configuration(proxyBeanMethods = false)
    static class InnerLevel1 {

      @Configuration(proxyBeanMethods = false)
      static class InnerLevel2 {

        @Component
        static String disabled() {
          return "disabled";
        }

        @EnableDependencyInjection
        @Configuration(proxyBeanMethods = false)
        static class InnerLevel3 {

          @Component
          static String string() {
            return "string";
          }

          @Component
          @EnableDependencyInjection
          static String enableDI() {
            return "enableDI";
          }

        }
      }

      @Configuration(proxyBeanMethods = false)
      static class InnerLevel22 {

        @Component
        static String innerLevel22Disabled() {
          return "innerLevel22Disabled";
        }
      }
    }

  }

}
