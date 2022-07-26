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

package cn.taketoday.classify;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.DirectFieldAccessor;

import static org.assertj.core.api.Assertions.assertThat;

public class BinaryExceptionClassifierTests {

  BinaryExceptionClassifier classifier = new BinaryExceptionClassifier(false);

  @Test
  public void testClassifyNullIsDefault() {
    assertThat(classifier.classify(null)).isFalse();
  }

  @Test
  public void testFalseIsDefault() {
    assertThat(classifier.getDefault()).isFalse();
  }

  @Test
  public void testDefaultProvided() {
    classifier = new BinaryExceptionClassifier(true);
    assertThat(classifier.getDefault()).isTrue();
  }

  @Test
  public void testClassifyRandomException() {
    assertThat(classifier.classify(new IllegalStateException("foo"))).isFalse();
  }

  @Test
  public void testClassifyExactMatch() {
    Collection<Class<? extends Throwable>> set = Collections
            .singleton(IllegalStateException.class);
    assertThat(new BinaryExceptionClassifier(set).classify(new IllegalStateException("Foo"))).isTrue();
  }

  @Test
  public void testClassifyExactMatchInCause() {
    Collection<Class<? extends Throwable>> set = Collections
            .singleton(IllegalStateException.class);
    BinaryExceptionClassifier binaryExceptionClassifier = new BinaryExceptionClassifier(set);
    binaryExceptionClassifier.setTraverseCauses(true);
    assertThat(binaryExceptionClassifier.classify(new RuntimeException(new IllegalStateException("Foo")))).isTrue();
  }

  @Test
  public void testClassifySubclassMatchInCause() {
    Collection<Class<? extends Throwable>> set = Collections
            .singleton(IllegalStateException.class);
    BinaryExceptionClassifier binaryExceptionClassifier = new BinaryExceptionClassifier(set);
    binaryExceptionClassifier.setTraverseCauses(true);
    assertThat(binaryExceptionClassifier.classify(new RuntimeException(new FooException("Foo")))).isTrue();
  }

  @Test
  public void testClassifySubclassMatchInCauseFalse() {
    Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
    map.put(IllegalStateException.class, true);
    map.put(BarException.class, false);
    BinaryExceptionClassifier binaryExceptionClassifier = new BinaryExceptionClassifier(map, true);
    binaryExceptionClassifier.setTraverseCauses(true);
    assertThat(
            binaryExceptionClassifier.classify(new RuntimeException(new FooException("Foo", new BarException()))))
            .isTrue();
    assertThat(((Map<?, ?>) new DirectFieldAccessor(binaryExceptionClassifier).getPropertyValue("classified"))
            .containsKey(FooException.class)).isTrue();
  }

  @Test
  public void testTypesProvidedInConstructor() {
    classifier = new BinaryExceptionClassifier(
            Collections.singleton(IllegalStateException.class));
    assertThat(classifier.classify(new IllegalStateException("Foo"))).isTrue();
  }

  @Test
  public void testTypesProvidedInConstructorWithNonDefault() {
    classifier = new BinaryExceptionClassifier(
            Collections.singleton(IllegalStateException.class), false);
    assertThat(classifier.classify(new IllegalStateException("Foo"))).isFalse();
  }

  @Test
  public void testTypesProvidedInConstructorWithNonDefaultInCause() {
    classifier = new BinaryExceptionClassifier(
            Collections.singleton(IllegalStateException.class), false);
    classifier.setTraverseCauses(true);
    assertThat(classifier.classify(new RuntimeException(new RuntimeException(new IllegalStateException("Foo")))))
            .isFalse();
  }

  @SuppressWarnings("serial")
  private class FooException extends IllegalStateException {

    private FooException(String s) {
      super(s);
    }

    private FooException(String s, Throwable t) {
      super(s, t);
    }

  }

  @SuppressWarnings("serial")
  private class BarException extends RuntimeException {

    private BarException() {
      super();
    }

  }

}
