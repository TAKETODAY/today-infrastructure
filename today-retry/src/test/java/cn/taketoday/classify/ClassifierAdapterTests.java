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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class ClassifierAdapterTests {

  private ClassifierAdapter<String, Integer> adapter = new ClassifierAdapter<>();

  @Test
  public void testClassifierAdapterObject() {
    adapter = new ClassifierAdapter<>(new Object() {
      @cn.taketoday.classify.annotation.Classifier
      public Integer getValue(String key) {
        return Integer.parseInt(key);
      }

      @SuppressWarnings("unused")
      public Integer getAnother(String key) {
        throw new UnsupportedOperationException("Not allowed");
      }
    });
    assertThat(adapter.classify("23").intValue()).isEqualTo(23);
  }

  @Test
  public void testClassifierAdapterObjectWithNoAnnotation() {
    assertThatIllegalStateException().isThrownBy(() -> new ClassifierAdapter<>(new Object() {
      @SuppressWarnings("unused")
      public Integer getValue(String key) {
        return Integer.parseInt(key);
      }

      @SuppressWarnings("unused")
      public Integer getAnother(String key) {
        throw new UnsupportedOperationException("Not allowed");
      }
    }));
  }

  @Test
  public void testClassifierAdapterObjectSingleMethodWithNoAnnotation() {
    adapter = new ClassifierAdapter<>(new Object() {
      @SuppressWarnings("unused")
      public Integer getValue(String key) {
        return Integer.parseInt(key);
      }

      @SuppressWarnings("unused")
      public void doNothing(String key) {
      }

      @SuppressWarnings("unused")
      public String doNothing(String key, int value) {
        return "foo";
      }
    });
    assertThat(adapter.classify("23").intValue()).isEqualTo(23);
  }

  @Test
  public void testClassifierAdapterClassifier() {
    adapter = new ClassifierAdapter<>(Integer::valueOf);
    assertThat(adapter.classify("23").intValue()).isEqualTo(23);
  }

  @Test
  public void testClassifyWithSetter() {
    adapter.setDelegate(new Object() {
      @cn.taketoday.classify.annotation.Classifier
      public Integer getValue(String key) {
        return Integer.parseInt(key);
      }
    });
    assertThat(adapter.classify("23").intValue()).isEqualTo(23);
  }

  @Test
  public void testClassifyWithWrongType() {
    adapter.setDelegate(new Object() {
      @cn.taketoday.classify.annotation.Classifier
      public String getValue(Integer key) {
        return key.toString();
      }
    });
    assertThatIllegalArgumentException().isThrownBy(() -> adapter.classify("23"));
  }

  @Test
  public void testClassifyWithClassifier() {
    adapter.setDelegate(Integer::valueOf);
    assertThat(adapter.classify("23").intValue()).isEqualTo(23);
  }

}
