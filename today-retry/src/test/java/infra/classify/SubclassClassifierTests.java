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

package infra.classify;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class SubclassClassifierTests {

  @Test
  public void testClassifyInterface() {
    SubclassClassifier<Object, String> classifier = new SubclassClassifier<>();
    classifier.setTypeMap(Collections.<Class<?>, String>singletonMap(Supplier.class, "foo"));
    assertThat(classifier.classify(new Foo())).isEqualTo("foo");
  }

  @Test
  public void testClassifyInterfaceOfParent() {
    SubclassClassifier<Object, String> classifier = new SubclassClassifier<>();
    classifier.setTypeMap(Collections.<Class<?>, String>singletonMap(Supplier.class, "foo"));
    assertThat(classifier.classify(new Bar())).isEqualTo("foo");
  }

  public class Bar extends Foo {

  }

  public static class Foo implements Supplier<String> {

    @Override
    public String get() {
      return "foo";
    }

  }

}
