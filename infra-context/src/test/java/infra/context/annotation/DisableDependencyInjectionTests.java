/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.beans.factory.annotation.EnableDependencyInjection;
import infra.stereotype.Component;

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

  @EnableDependencyInjection
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
