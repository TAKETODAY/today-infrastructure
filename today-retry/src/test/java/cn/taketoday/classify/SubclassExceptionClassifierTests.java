/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

public class SubclassExceptionClassifierTests {

  SubclassClassifier<Throwable, String> classifier = new SubclassClassifier<>();

  @Test
  public void testClassifyNullIsDefault() {
    assertThat(this.classifier.getDefault()).isEqualTo(this.classifier.classify(null));
  }

  @Test
  public void testClassifyNull() {
    assertThat(this.classifier.classify(null)).isNull();
  }

  @Test
  public void testClassifyNullNonDefault() {
    this.classifier = new SubclassClassifier<>("foo");
    assertThat(this.classifier.classify(null)).isEqualTo("foo");
  }

  @Test
  public void testClassifyRandomException() {
    assertThat(this.classifier.classify(new IllegalStateException("Foo"))).isNull();
  }

  @Test
  public void testClassifyExactMatch() {
    this.classifier.setTypeMap(
            Collections.<Class<? extends Throwable>, String>singletonMap(IllegalStateException.class, "foo"));
    assertThat(this.classifier.classify(new IllegalStateException("Foo"))).isEqualTo("foo");
  }

  @Test
  public void testClassifySubclassMatch() {
    this.classifier.setTypeMap(
            Collections.<Class<? extends Throwable>, String>singletonMap(RuntimeException.class, "foo"));
    assertThat(this.classifier.classify(new IllegalStateException("Foo"))).isEqualTo("foo");
  }

  @Test
  public void testClassifySuperclassDoesNotMatch() {
    this.classifier.setTypeMap(
            Collections.<Class<? extends Throwable>, String>singletonMap(IllegalStateException.class, "foo"));
    assertThat(this.classifier.classify(new RuntimeException("Foo"))).isEqualTo(this.classifier.getDefault());
  }

  @SuppressWarnings("serial")
  @Test
  public void testClassifyAncestorMatch() {
    this.classifier.setTypeMap(new HashMap<Class<? extends Throwable>, String>() {
      {
        put(Exception.class, "foo");
        put(IllegalArgumentException.class, "bar");
        put(RuntimeException.class, "spam");
      }
    });
    assertThat(this.classifier.classify(new IllegalStateException("Foo"))).isEqualTo("spam");
  }

  @SuppressWarnings("serial")
  @Test
  public void testClassifyAncestorMatch2() {
    this.classifier = new SubclassClassifier<>();
    this.classifier.setTypeMap(new HashMap<Class<? extends Throwable>, String>() {
      {
        put(SocketException.class, "1");
        put(FileNotFoundException.class, "buz");
        put(NoSuchElementException.class, "buz");
        put(ArrayIndexOutOfBoundsException.class, "buz");
        put(IllegalArgumentException.class, "bar");
        put(RuntimeException.class, "spam");
        put(ConnectException.class, "2");
      }
    });
    assertThat(this.classifier.classify(new SubConnectException())).isEqualTo("2");
  }

  @SuppressWarnings("serial")
  public static class SubConnectException extends ConnectException {

  }

}
