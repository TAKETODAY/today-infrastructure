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

import cn.taketoday.classify.annotation.Classifier;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class ClassifierAdapterTests {

  private ClassifierAdapter<String, Integer> adapter = new ClassifierAdapter<String, Integer>();

  @Test
  public void testClassifierAdapterObject() {
    adapter = new ClassifierAdapter<String, Integer>(new Object() {
      @Classifier
      public Integer getValue(String key) {
        return Integer.parseInt(key);
      }

      @SuppressWarnings("unused")
      public Integer getAnother(String key) {
        throw new UnsupportedOperationException("Not allowed");
      }
    });
    assertEquals(23, adapter.classify("23").intValue());
  }

  @Test(expected = IllegalStateException.class)
  public void testClassifierAdapterObjectWithNoAnnotation() {
    adapter = new ClassifierAdapter<String, Integer>(new Object() {
      @SuppressWarnings("unused")
      public Integer getValue(String key) {
        return Integer.parseInt(key);
      }

      @SuppressWarnings("unused")
      public Integer getAnother(String key) {
        throw new UnsupportedOperationException("Not allowed");
      }
    });
    assertEquals(23, adapter.classify("23").intValue());
  }

  @Test
  public void testClassifierAdapterObjectSingleMethodWithNoAnnotation() {
    adapter = new ClassifierAdapter<String, Integer>(new Object() {
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
    assertEquals(23, adapter.classify("23").intValue());
  }

  @SuppressWarnings({ "serial" })
  @Test
  public void testClassifierAdapterClassifier() {
    adapter = new ClassifierAdapter<String, Integer>(
            new cn.taketoday.classify.Classifier<String, Integer>() {
              public Integer classify(String classifiable) {
                return Integer.valueOf(classifiable);
              }
            });
    assertEquals(23, adapter.classify("23").intValue());
  }

  @Test
  public void testClassifyWithSetter() {
    adapter.setDelegate(new Object() {
      @Classifier
      public Integer getValue(String key) {
        return Integer.parseInt(key);
      }
    });
    assertEquals(23, adapter.classify("23").intValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testClassifyWithWrongType() {
    adapter.setDelegate(new Object() {
      @Classifier
      public String getValue(Integer key) {
        return key.toString();
      }
    });
    assertEquals(23, adapter.classify("23").intValue());
  }

  @SuppressWarnings("serial")
  @Test
  public void testClassifyWithClassifier() {
    adapter.setDelegate(new cn.taketoday.classify.Classifier<String, Integer>() {
      public Integer classify(String classifiable) {
        return Integer.valueOf(classifiable);
      }
    });
    assertEquals(23, adapter.classify("23").intValue());
  }

}
