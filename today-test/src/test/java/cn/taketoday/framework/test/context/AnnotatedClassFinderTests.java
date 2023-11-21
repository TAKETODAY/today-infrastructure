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

package cn.taketoday.framework.test.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.framework.InfraConfiguration;
import cn.taketoday.framework.test.context.example.ExampleConfig;
import cn.taketoday.framework.test.context.example.scan.Example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/27 21:38
 */
class AnnotatedClassFinderTests {

  private final AnnotatedClassFinder finder = new AnnotatedClassFinder(InfraConfiguration.class);

  @Test
  void findFromClassWhenSourceIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.finder.findFromClass((Class<?>) null))
            .withMessageContaining("Source is required");
  }

  @Test
  void findFromPackageWhenSourceIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.finder.findFromPackage((String) null))
            .withMessageContaining("Source is required");
  }

  @Test
  void findFromPackageWhenNoConfigurationFoundShouldReturnNull() {
    Class<?> config = this.finder.findFromPackage("cn.taketoday.framework");
    assertThat(config).isNull();
  }

  @Test
  void findFromClassWhenConfigurationIsFoundShouldReturnConfiguration() {
    Class<?> config = this.finder.findFromClass(Example.class);
    assertThat(config).isEqualTo(ExampleConfig.class);
  }

  @Test
  void findFromPackageWhenConfigurationIsFoundShouldReturnConfiguration() {
    Class<?> config = this.finder.findFromPackage("cn.taketoday.framework.test.context.example.scan");
    assertThat(config).isEqualTo(ExampleConfig.class);
  }

}