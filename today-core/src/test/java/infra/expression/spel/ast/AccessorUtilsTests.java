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

package infra.expression.spel.ast;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.expression.TargetedAccessor;
import infra.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/11/29 22:15
 */
class AccessorUtilsTests {

  private final TargetedAccessor animal1Accessor = createAccessor("Animal1", Animal.class);

  private final TargetedAccessor animal2Accessor = createAccessor("Animal2", Animal.class);

  private final TargetedAccessor cat1Accessor = createAccessor("Cat1", Cat.class);

  private final TargetedAccessor cat2Accessor = createAccessor("Cat2", Cat.class);

  private final TargetedAccessor generic1Accessor = createAccessor("Generic1", null);

  private final TargetedAccessor generic2Accessor = createAccessor("Generic2", null);

  private final List<TargetedAccessor> accessors = List.of(
          generic1Accessor,
          cat1Accessor,
          animal1Accessor,
          animal2Accessor,
          cat2Accessor,
          generic2Accessor
  );

  @Test
  void emptyAccessorsList() {
    List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry(new Cat(), List.of());
    assertThat(accessorsToTry).isEmpty();
  }

  @Test
  void noMatch() {
    List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry(new Dog(), List.of(cat1Accessor));
    assertThat(accessorsToTry).isEmpty();
  }

  @Test
  void singleExactTypeMatch() {
    List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry(new Cat(), List.of(cat1Accessor));
    assertThat(accessorsToTry).containsExactly(cat1Accessor);
  }

  @Test
  void exactTypeSupertypeAndGenericMatches() {
    List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry(new Cat(), accessors);
    assertThat(accessorsToTry).containsExactly(
            cat1Accessor, cat2Accessor, animal1Accessor, animal2Accessor, generic1Accessor, generic2Accessor);
  }

  @Test
  void supertypeAndGenericMatches() {
    List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry(new Dog(), accessors);
    assertThat(accessorsToTry).containsExactly(
            animal1Accessor, animal2Accessor, generic1Accessor, generic2Accessor);
  }

  @Test
  void genericMatches() {
    List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry("not an Animal", accessors);
    assertThat(accessorsToTry).containsExactly(generic1Accessor, generic2Accessor);
  }

  private static TargetedAccessor createAccessor(String name, Class<?> type) {
    return new DemoAccessor(name, (type != null ? new Class<?>[] { type } : null));
  }

  private record DemoAccessor(String name, Class<?>[] types) implements TargetedAccessor {

    @Override
    @Nullable
    public Class<?>[] getSpecificTargetClasses() {
      return this.types;
    }

    @Override
    public final String toString() {
      return this.name;
    }
  }

  sealed interface Animal permits Cat, Dog {
  }

  static final class Cat implements Animal {
  }

  static final class Dog implements Animal {
  }

}