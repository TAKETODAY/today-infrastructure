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

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.DirectFieldAccessor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BinaryExceptionClassifierTests {

  BinaryExceptionClassifier classifier = new BinaryExceptionClassifier(false);

  @Test
  public void testClassifyNullIsDefault() {
    assertFalse(classifier.classify(null));
  }

  @Test
  public void testFalseIsDefault() {
    assertFalse(classifier.getDefault());
  }

  @Test
  public void testDefaultProvided() {
    classifier = new BinaryExceptionClassifier(true);
    assertTrue(classifier.getDefault());
  }

  @Test
  public void testClassifyRandomException() {
    assertFalse(classifier.classify(new IllegalStateException("foo")));
  }

  @Test
  public void testClassifyExactMatch() {
    Collection<Class<? extends Throwable>> set = Collections
            .<Class<? extends Throwable>>singleton(IllegalStateException.class);
    assertTrue(new BinaryExceptionClassifier(set).classify(new IllegalStateException("Foo")));
  }

  @Test
  public void testClassifyExactMatchInCause() {
    Collection<Class<? extends Throwable>> set = Collections
            .<Class<? extends Throwable>>singleton(IllegalStateException.class);
    BinaryExceptionClassifier binaryExceptionClassifier = new BinaryExceptionClassifier(set);
    binaryExceptionClassifier.setTraverseCauses(true);
    assertTrue(binaryExceptionClassifier.classify(new RuntimeException(new IllegalStateException("Foo"))));
  }

  @Test
  public void testClassifySubclassMatchInCause() {
    Collection<Class<? extends Throwable>> set = Collections
            .<Class<? extends Throwable>>singleton(IllegalStateException.class);
    BinaryExceptionClassifier binaryExceptionClassifier = new BinaryExceptionClassifier(set);
    binaryExceptionClassifier.setTraverseCauses(true);
    assertTrue(binaryExceptionClassifier.classify(new RuntimeException(new FooException("Foo"))));
  }

  @Test
  public void testClassifySubclassMatchInCauseFalse() {
    Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
    map.put(IllegalStateException.class, true);
    map.put(BarException.class, false);
    BinaryExceptionClassifier binaryExceptionClassifier = new BinaryExceptionClassifier(map, true);
    binaryExceptionClassifier.setTraverseCauses(true);
    assertTrue(
            binaryExceptionClassifier.classify(new RuntimeException(new FooException("Foo", new BarException()))));
    assertTrue(((Map<?, ?>) new DirectFieldAccessor(binaryExceptionClassifier).getPropertyValue("classified"))
            .containsKey(FooException.class));
  }

  @Test
  public void testTypesProvidedInConstructor() {
    classifier = new BinaryExceptionClassifier(
            Collections.<Class<? extends Throwable>>singleton(IllegalStateException.class));
    assertTrue(classifier.classify(new IllegalStateException("Foo")));
  }

  @Test
  public void testTypesProvidedInConstructorWithNonDefault() {
    classifier = new BinaryExceptionClassifier(
            Collections.<Class<? extends Throwable>>singleton(IllegalStateException.class), false);
    assertFalse(classifier.classify(new IllegalStateException("Foo")));
  }

  @Test
  public void testTypesProvidedInConstructorWithNonDefaultInCause() {
    classifier = new BinaryExceptionClassifier(
            Collections.<Class<? extends Throwable>>singleton(IllegalStateException.class), false);
    classifier.setTraverseCauses(true);
    assertFalse(classifier.classify(new RuntimeException(new RuntimeException(new IllegalStateException("Foo")))));
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
