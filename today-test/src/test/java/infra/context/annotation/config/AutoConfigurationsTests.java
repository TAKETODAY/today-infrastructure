/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 12:15
 */
class AutoConfigurationsTests {

  @Test
  void ofShouldCreateOrderedConfigurations() {
    Configurations configurations = AutoConfigurations.of(AutoConfigureA.class, AutoConfigureB.class);
    assertThat(Configurations.getClasses(configurations)).containsExactly(AutoConfigureB.class,
            AutoConfigureA.class);
  }

  @Test
  void whenHasReplacementForAutoConfigureAfterShouldCreateOrderedConfigurations() {
    Configurations configurations = new AutoConfigurations(this::replaceB,
            Arrays.asList(AutoConfigureA.class, AutoConfigureB2.class));
    assertThat(Configurations.getClasses(configurations)).containsExactly(AutoConfigureB2.class,
            AutoConfigureA.class);
  }

  @Test
  void whenHasReplacementForClassShouldReplaceClass() {
    Configurations configurations = new AutoConfigurations(this::replaceB,
            Arrays.asList(AutoConfigureA.class, AutoConfigureB.class));
    assertThat(Configurations.getClasses(configurations)).containsExactly(AutoConfigureB2.class,
            AutoConfigureA.class);
  }

  @Test
  void getBeanNameShouldUseClassName() {
    Configurations configurations = AutoConfigurations.of(AutoConfigureA.class, AutoConfigureB.class);
    assertThat(configurations.getBeanName(AutoConfigureA.class)).isEqualTo(AutoConfigureA.class.getName());
  }

  private String replaceB(String className) {
    return (!AutoConfigureB.class.getName().equals(className)) ? className : AutoConfigureB2.class.getName();
  }

  @AutoConfigureAfter(AutoConfigureB.class)
  static class AutoConfigureA {

  }

  static class AutoConfigureB {

  }

  static class AutoConfigureB2 {

  }

}
