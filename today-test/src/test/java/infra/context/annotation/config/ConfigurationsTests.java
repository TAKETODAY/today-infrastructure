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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 12:14
 */
class ConfigurationsTests {

  @Test
  void createWhenClassesIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new TestConfigurations((Collection<Class<?>>) null))
            .withMessageContaining("Classes is required");
  }

  @Test
  void createShouldSortClassesUsingSortMethod() {
    TestDeprecatedSortedConfigurations configurations = new TestDeprecatedSortedConfigurations(
            Arrays.asList(OutputStream.class, InputStream.class));
    assertThat(configurations.getClasses()).containsExactly(InputStream.class, OutputStream.class);
  }

  @Test
  void getClassesShouldMergeByClassAndSortUsingSortMethod() {
    Configurations c1 = new TestDeprecatedSortedConfigurations(
            Arrays.asList(OutputStream.class, InputStream.class));
    Configurations c2 = new TestConfigurations(Collections.singletonList(Short.class));
    Configurations c3 = new TestDeprecatedSortedConfigurations(Arrays.asList(String.class, Integer.class));
    Configurations c4 = new TestConfigurations(Arrays.asList(Long.class, Byte.class));
    Class<?>[] classes = Configurations.getClasses(c1, c2, c3, c4);
    assertThat(classes).containsExactly(Short.class, Long.class, Byte.class, InputStream.class, Integer.class,
            OutputStream.class, String.class);
  }

  @Test
  void createShouldSortClasses() {
    TestConfigurations configurations = new TestConfigurations(Sorter.instance, OutputStream.class,
            InputStream.class);
    assertThat(configurations.getClasses()).containsExactly(InputStream.class, OutputStream.class);
  }

  @Test
  void getClassesShouldMergeByClassAndSort() {
    Configurations c1 = new TestSortedConfigurations(OutputStream.class, InputStream.class);
    Configurations c2 = new TestConfigurations(Short.class);
    Configurations c3 = new TestSortedConfigurations(String.class, Integer.class);
    Configurations c4 = new TestConfigurations(Long.class, Byte.class);
    Class<?>[] classes = Configurations.getClasses(c1, c2, c3, c4);
    assertThat(classes).containsExactly(Short.class, Long.class, Byte.class, InputStream.class, Integer.class,
            OutputStream.class, String.class);
  }

  @Test
  void getBeanNameWhenNoFunctionReturnsNull() {
    Configurations configurations = new TestConfigurations(Short.class);
    assertThat(configurations.getBeanName(Short.class)).isNull();
  }

  @Test
  void getBeanNameWhenFunctionReturnsBeanName() {
    Configurations configurations = new TestConfigurations(Sorter.instance, List.of(Short.class), Class::getName);
    assertThat(configurations.getBeanName(Short.class)).isEqualTo(Short.class.getName());
  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class TestConfigurations extends Configurations {

    TestConfigurations(Class<?>... classes) {
      this(Arrays.asList(classes));
    }

    TestConfigurations(UnaryOperator<Collection<Class<?>>> sorter, Class<?>... classes) {
      this(sorter, Arrays.asList(classes), null);
    }

    TestConfigurations(UnaryOperator<Collection<Class<?>>> sorter, Collection<Class<?>> classes,
            Function<Class<?>, String> beanNameGenerator) {
      super(sorter, classes, beanNameGenerator);
    }

    TestConfigurations(Collection<Class<?>> classes) {
      super(classes);
    }

    @Override
    protected Configurations merge(Set<Class<?>> mergedClasses) {
      return new TestConfigurations(mergedClasses);
    }

  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  static class TestSortedConfigurations extends Configurations {

    protected TestSortedConfigurations(Class<?>... classes) {
      this(Arrays.asList(classes));
    }

    protected TestSortedConfigurations(Collection<Class<?>> classes) {
      super(Sorter.instance, classes, null);
    }

    @Override
    protected Configurations merge(Set<Class<?>> mergedClasses) {
      return new TestSortedConfigurations(mergedClasses);
    }

  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  @SuppressWarnings("removal")
  static class TestDeprecatedSortedConfigurations extends Configurations {

    protected TestDeprecatedSortedConfigurations(Collection<Class<?>> classes) {
      super(classes);
    }

    @Override
    protected Collection<Class<?>> sort(Collection<Class<?>> classes) {
      return Sorter.instance.apply(classes);
    }

    @Override
    protected Configurations merge(Set<Class<?>> mergedClasses) {
      return new TestDeprecatedSortedConfigurations(mergedClasses);
    }

  }

  static class Sorter implements UnaryOperator<Collection<Class<?>>> {

    static final Sorter instance = new Sorter();

    @Override
    public Collection<Class<?>> apply(Collection<Class<?>> classes) {
      ArrayList<Class<?>> sorted = new ArrayList<>(classes);
      sorted.sort(Comparator.comparing(ClassUtils::getShortName));
      return sorted;

    }

  }

}
