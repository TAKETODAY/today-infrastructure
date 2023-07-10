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

package cn.taketoday.app.loader.jar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AsciiBytes}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class AsciiBytesTests {

  private static final char NO_SUFFIX = 0;

  @Test
  void createFromBytes() {
    AsciiBytes bytes = new AsciiBytes(new byte[] { 65, 66 });
    assertThat(bytes).hasToString("AB");
  }

  @Test
  void createFromBytesWithOffset() {
    AsciiBytes bytes = new AsciiBytes(new byte[] { 65, 66, 67, 68 }, 1, 2);
    assertThat(bytes).hasToString("BC");
  }

  @Test
  void createFromString() {
    AsciiBytes bytes = new AsciiBytes("AB");
    assertThat(bytes).hasToString("AB");
  }

  @Test
  void length() {
    AsciiBytes b1 = new AsciiBytes(new byte[] { 65, 66 });
    AsciiBytes b2 = new AsciiBytes(new byte[] { 65, 66, 67, 68 }, 1, 2);
    assertThat(b1.length()).isEqualTo(2);
    assertThat(b2.length()).isEqualTo(2);
  }

  @Test
  void startWith() {
    AsciiBytes abc = new AsciiBytes(new byte[] { 65, 66, 67 });
    AsciiBytes ab = new AsciiBytes(new byte[] { 65, 66 });
    AsciiBytes bc = new AsciiBytes(new byte[] { 65, 66, 67 }, 1, 2);
    AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
    assertThat(abc.startsWith(abc)).isTrue();
    assertThat(abc.startsWith(ab)).isTrue();
    assertThat(abc.startsWith(bc)).isFalse();
    assertThat(abc.startsWith(abcd)).isFalse();
  }

  @Test
  void endsWith() {
    AsciiBytes abc = new AsciiBytes(new byte[] { 65, 66, 67 });
    AsciiBytes bc = new AsciiBytes(new byte[] { 65, 66, 67 }, 1, 2);
    AsciiBytes ab = new AsciiBytes(new byte[] { 65, 66 });
    AsciiBytes aabc = new AsciiBytes(new byte[] { 65, 65, 66, 67 });
    assertThat(abc.endsWith(abc)).isTrue();
    assertThat(abc.endsWith(bc)).isTrue();
    assertThat(abc.endsWith(ab)).isFalse();
    assertThat(abc.endsWith(aabc)).isFalse();
  }

  @Test
  void substringFromBeingIndex() {
    AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
    assertThat(abcd.substring(0)).hasToString("ABCD");
    assertThat(abcd.substring(1)).hasToString("BCD");
    assertThat(abcd.substring(2)).hasToString("CD");
    assertThat(abcd.substring(3)).hasToString("D");
    assertThat(abcd.substring(4).toString()).isEmpty();
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> abcd.substring(5));
  }

  @Test
  void substring() {
    AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
    assertThat(abcd.substring(0, 4)).hasToString("ABCD");
    assertThat(abcd.substring(1, 3)).hasToString("BC");
    assertThat(abcd.substring(3, 4)).hasToString("D");
    assertThat(abcd.substring(3, 3).toString()).isEmpty();
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> abcd.substring(3, 5));
  }

  @Test
  void hashCodeAndEquals() {
    AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
    AsciiBytes bc = new AsciiBytes(new byte[] { 66, 67 });
    AsciiBytes bc_substring = new AsciiBytes(new byte[] { 65, 66, 67, 68 }).substring(1, 3);
    AsciiBytes bc_string = new AsciiBytes("BC");
    assertThat(bc).hasSameHashCodeAs(bc);
    assertThat(bc).hasSameHashCodeAs(bc_substring);
    assertThat(bc).hasSameHashCodeAs(bc_string);
    assertThat(bc).isEqualTo(bc);
    assertThat(bc).isEqualTo(bc_substring);
    assertThat(bc).isEqualTo(bc_string);
    assertThat(bc.hashCode()).isNotEqualTo(abcd.hashCode());
    assertThat(bc).isNotEqualTo(abcd);
  }

  @Test
  void hashCodeSameAsString() {
    hashCodeSameAsString("abcABC123xyz!");
  }

  @Test
  void hashCodeSameAsStringWithSpecial() {
    hashCodeSameAsString("special/\u00EB.dat");
  }

  @Test
  void hashCodeSameAsStringWithCyrillicCharacters() {
    hashCodeSameAsString("\u0432\u0435\u0441\u043D\u0430");
  }

  @Test
  void hashCodeSameAsStringWithEmoji() {
    hashCodeSameAsString("\ud83d\udca9");
  }

  private void hashCodeSameAsString(String input) {
    assertThat(new AsciiBytes(input)).hasSameHashCodeAs(input);
  }

  @Test
  void matchesSameAsString() {
    matchesSameAsString("abcABC123xyz!");
  }

  @Test
  void matchesSameAsStringWithSpecial() {
    matchesSameAsString("special/\u00EB.dat");
  }

  @Test
  void matchesSameAsStringWithCyrillicCharacters() {
    matchesSameAsString("\u0432\u0435\u0441\u043D\u0430");
  }

  @Test
  void matchesDifferentLengths() {
    assertThat(new AsciiBytes("abc").matches("ab", NO_SUFFIX)).isFalse();
    assertThat(new AsciiBytes("abc").matches("abcd", NO_SUFFIX)).isFalse();
    assertThat(new AsciiBytes("abc").matches("abc", NO_SUFFIX)).isTrue();
    assertThat(new AsciiBytes("abc").matches("a", 'b')).isFalse();
    assertThat(new AsciiBytes("abc").matches("abc", 'd')).isFalse();
    assertThat(new AsciiBytes("abc").matches("ab", 'c')).isTrue();
  }

  @Test
  void matchesSuffix() {
    assertThat(new AsciiBytes("ab").matches("a", 'b')).isTrue();
  }

  @Test
  void matchesSameAsStringWithEmoji() {
    matchesSameAsString("\ud83d\udca9");
  }

  @Test
  void hashCodeFromInstanceMatchesHashCodeFromString() {
    String name = "fonts/宋体/simsun.ttf";
    assertThat(new AsciiBytes(name).hashCode()).isEqualTo(AsciiBytes.hashCode(name));
  }

  @Test
  void instanceCreatedFromCharSequenceMatchesSameCharSequence() {
    String name = "fonts/宋体/simsun.ttf";
    assertThat(new AsciiBytes(name).matches(name, NO_SUFFIX)).isTrue();
  }

  private void matchesSameAsString(String input) {
    assertThat(new AsciiBytes(input).matches(input, NO_SUFFIX)).isTrue();
  }

}
