/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.annotation.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 12:14
 */
class ConfigurationsTests {

  @Test
  void createWhenClassesIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new TestConfigurations(null))
            .withMessageContaining("Classes must not be null");
  }

  @Test
  void createShouldSortClasses() {
    TestSortedConfigurations configurations = new TestSortedConfigurations(
            Arrays.asList(OutputStream.class, InputStream.class));
    assertThat(configurations.getClasses()).containsExactly(InputStream.class, OutputStream.class);
  }

  @Test
  void getClassesShouldMergeByClassAndSort() {
    Configurations c1 = new TestSortedConfigurations(Arrays.asList(OutputStream.class, InputStream.class));
    Configurations c2 = new TestConfigurations(Collections.singletonList(Short.class));
    Configurations c3 = new TestSortedConfigurations(Arrays.asList(String.class, Integer.class));
    Configurations c4 = new TestConfigurations(Arrays.asList(Long.class, Byte.class));
    Class<?>[] classes = Configurations.getClasses(c1, c2, c3, c4);
    assertThat(classes).containsExactly(Short.class, Long.class, Byte.class, InputStream.class, Integer.class,
            OutputStream.class, String.class);
  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class TestConfigurations extends Configurations {

    protected TestConfigurations(Collection<Class<?>> classes) {
      super(classes);
    }

    @Override
    protected Configurations merge(Set<Class<?>> mergedClasses) {
      return new TestConfigurations(mergedClasses);
    }

  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  static class TestSortedConfigurations extends Configurations {

    protected TestSortedConfigurations(Collection<Class<?>> classes) {
      super(classes);
    }

    @Override
    protected Collection<Class<?>> sort(Collection<Class<?>> classes) {
      ArrayList<Class<?>> sorted = new ArrayList<>(classes);
      sorted.sort(Comparator.comparing(ClassUtils::getShortName));
      return sorted;
    }

    @Override
    protected Configurations merge(Set<Class<?>> mergedClasses) {
      return new TestSortedConfigurations(mergedClasses);
    }

  }

}
