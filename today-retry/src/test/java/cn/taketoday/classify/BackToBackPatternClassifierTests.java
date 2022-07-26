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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.classify.annotation.Classifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 */
public class BackToBackPatternClassifierTests {

  private BackToBackPatternClassifier<String, String> classifier = new BackToBackPatternClassifier<>();

  private Map<String, String> map;

  @BeforeEach
  public void createMap() {
    map = new HashMap<>();
    map.put("foo", "bar");
    map.put("*", "spam");
  }

  @Test
  public void testNoClassifiers() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> classifier.classify("foo"));
  }

  @Test
  public void testCreateFromConstructor() {
    classifier = new BackToBackPatternClassifier<>(
            new PatternMatchingClassifier<>(Collections.singletonMap("oof", "bucket")),
            new PatternMatchingClassifier<>(map));
    assertThat(classifier.classify("oof")).isEqualTo("spam");
  }

  @Test
  public void testSetRouterDelegate() {
    classifier.setRouterDelegate(new Object() {
      @Classifier
      public String convert(String value) {
        return "bucket";
      }
    });
    classifier.setMatcherMap(map);
    assertThat(classifier.classify("oof")).isEqualTo("spam");
  }

  @Test
  public void testSingleMethodWithNoAnnotation() {
    classifier = new BackToBackPatternClassifier<>();
    classifier.setRouterDelegate(new RouterDelegate());
    classifier.setMatcherMap(map);
    assertThat(classifier.classify("oof")).isEqualTo("spam");
  }

  @SuppressWarnings("serial")
  private class RouterDelegate implements cn.taketoday.classify.Classifier<Object, String> {

    @Override
    public String classify(Object classifiable) {
      return "bucket";
    }

  }

}
