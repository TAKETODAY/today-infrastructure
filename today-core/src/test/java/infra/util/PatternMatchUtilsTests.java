/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/1 22:04
 */
class PatternMatchUtilsTests {

  @Test
  void nullAndEmptyValues() {
    assertDoesNotMatch((String) null, null);
    assertDoesNotMatch((String) null, "");
    assertDoesNotMatch("123", null);

    assertDoesNotMatch((String[]) null, null);
    assertDoesNotMatch((String[]) null, "");
    assertDoesNotMatch(new String[] {}, null);
  }

  @Test
  void trivial() {
    assertMatches("", "");
    assertMatches("123", "123");
    assertMatches("*", "123");

    assertMatches(new String[] { "" }, "");
    assertMatches(new String[] { "123" }, "123");
    assertMatches(new String[] { "*" }, "123");

    assertMatches(new String[] { null, "" }, "");
    assertMatches(new String[] { null, "123" }, "123");
    assertMatches(new String[] { null, "*" }, "123");

    testMixedCaseMatch("abC", "Abc");
  }

  @Test
  void startsWith() {
    assertMatches("get*", "getMe");
    assertDoesNotMatch("get*", "setMe");
    testMixedCaseMatch("geT*", "GetMe");
  }

  @Test
  void endsWith() {
    assertMatches("*Test", "getMeTest");
    assertDoesNotMatch("*Test", "setMe");
    testMixedCaseMatch("*TeSt", "getMeTesT");
  }

  @Test
  void between() {
    assertDoesNotMatch("*stuff*", "getMeTest");
    assertMatches("*stuff*", "getstuffTest");
    assertMatches("*stuff*", "stuffTest");
    assertMatches("*stuff*", "getstuff");
    assertMatches("*stuff*", "stuff");
    testMixedCaseMatch("*stuff*", "getStuffTest");
    testMixedCaseMatch("*stuff*", "StuffTest");
    testMixedCaseMatch("*stuff*", "getStuff");
    testMixedCaseMatch("*stuff*", "Stuff");
  }

  @Test
  void startsEnds() {
    assertMatches("on*Event", "onMyEvent");
    assertMatches("on*Event", "onEvent");
    assertDoesNotMatch("3*3", "3");
    assertMatches("3*3", "33");
    testMixedCaseMatch("on*Event", "OnMyEvenT");
    testMixedCaseMatch("on*Event", "OnEvenT");
  }

  @Test
  void startsEndsBetween() {
    assertMatches("12*45*78", "12345678");
    assertDoesNotMatch("12*45*78", "123456789");
    assertDoesNotMatch("12*45*78", "012345678");
    assertMatches("12*45*78", "124578");
    assertMatches("12*45*78", "1245457878");
    assertDoesNotMatch("3*3*3", "33");
    assertMatches("3*3*3", "333");
  }

  @Test
  void ridiculous() {
    assertMatches("*1*2*3*", "0011002001010030020201030");
    assertDoesNotMatch("1*2*3*4", "10300204");
    assertDoesNotMatch("1*2*3*3", "10300203");
    assertMatches("*1*2*3*", "123");
    assertDoesNotMatch("*1*2*3*", "132");
  }

  @Test
  void patternVariants() {
    assertDoesNotMatch("*a", "*");
    assertMatches("*a", "a");
    assertDoesNotMatch("*a", "b");
    assertMatches("*a", "aa");
    assertMatches("*a", "ba");
    assertDoesNotMatch("*a", "ab");
    assertDoesNotMatch("**a", "*");
    assertMatches("**a", "a");
    assertDoesNotMatch("**a", "b");
    assertMatches("**a", "aa");
    assertMatches("**a", "ba");
    assertDoesNotMatch("**a", "ab");
  }

  private void assertMatches(String pattern, String str) {
    assertThat(PatternMatchUtils.simpleMatch(pattern, str)).isTrue();
    assertThat(PatternMatchUtils.simpleMatchIgnoreCase(pattern, str)).isTrue();
  }

  private void assertDoesNotMatch(String pattern, String str) {
    assertThat(PatternMatchUtils.simpleMatch(pattern, str)).isFalse();
    assertThat(PatternMatchUtils.simpleMatchIgnoreCase(pattern, str)).isFalse();
  }

  private void testMixedCaseMatch(String pattern, String str) {
    assertThat(PatternMatchUtils.simpleMatch(pattern, str)).isFalse();
    assertThat(PatternMatchUtils.simpleMatchIgnoreCase(pattern, str)).isTrue();
  }

  private void assertMatches(String[] patterns, String str) {
    assertThat(PatternMatchUtils.simpleMatch(patterns, str)).isTrue();
    assertThat(PatternMatchUtils.simpleMatchIgnoreCase(patterns, str)).isTrue();
  }

  private void assertDoesNotMatch(String[] patterns, String str) {
    assertThat(PatternMatchUtils.simpleMatch(patterns, str)).isFalse();
    assertThat(PatternMatchUtils.simpleMatchIgnoreCase(patterns, str)).isFalse();
  }

}