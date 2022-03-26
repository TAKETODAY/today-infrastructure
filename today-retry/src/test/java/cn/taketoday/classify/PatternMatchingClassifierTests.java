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

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class PatternMatchingClassifierTests {

  private PatternMatchingClassifier<String> classifier = new PatternMatchingClassifier<String>();

  private Map<String, String> map;

  @Before
  public void createMap() {
    map = new HashMap<String, String>();
    map.put("foo", "bar");
    map.put("*", "spam");
  }

  @Test
  public void testSetPatternMap() {
    classifier.setPatternMap(map);
    assertEquals("bar", classifier.classify("foo"));
    assertEquals("spam", classifier.classify("bucket"));
  }

  @Test
  public void testCreateFromMap() {
    classifier = new PatternMatchingClassifier<String>(map);
    assertEquals("bar", classifier.classify("foo"));
    assertEquals("spam", classifier.classify("bucket"));
  }

}
