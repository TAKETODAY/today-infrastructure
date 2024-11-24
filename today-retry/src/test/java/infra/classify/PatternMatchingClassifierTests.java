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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
public class PatternMatchingClassifierTests {

  private PatternMatchingClassifier<String> classifier = new PatternMatchingClassifier<>();

  private Map<String, String> map;

  @BeforeEach
  public void createMap() {
    map = new HashMap<>();
    map.put("foo", "bar");
    map.put("*", "spam");
  }

  @Test
  public void testSetPatternMap() {
    classifier.setPatternMap(map);
    assertThat(classifier.classify("foo")).isEqualTo("bar");
    assertThat(classifier.classify("bucket")).isEqualTo("spam");
  }

  @Test
  public void testCreateFromMap() {
    classifier = new PatternMatchingClassifier<>(map);
    assertThat(classifier.classify("foo")).isEqualTo("bar");
    assertThat(classifier.classify("bucket")).isEqualTo("spam");
  }

}
