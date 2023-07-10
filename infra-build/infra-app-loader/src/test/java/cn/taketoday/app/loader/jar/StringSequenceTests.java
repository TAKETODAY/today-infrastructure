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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for {@link StringSequence}.
 *
 * @author Phillip Webb
 */
class StringSequenceTests {

  @Test
  void createWhenSourceIsNullShouldThrowException() {
    assertThatNullPointerException().isThrownBy(() -> new StringSequence(null))
            .withMessage("Source must not be null");
  }

  @Test
  void createWithIndexWhenSourceIsNullShouldThrowException() {
    assertThatNullPointerException().isThrownBy(() -> new StringSequence(null, 0, 0))
            .withMessage("Source must not be null");
  }

  @Test
  void createWhenStartIsLessThanZeroShouldThrowException() {
    assertThatExceptionOfType(StringIndexOutOfBoundsException.class)
            .isThrownBy(() -> new StringSequence("x", -1, 0));
  }

  @Test
  void createWhenEndIsGreaterThanLengthShouldThrowException() {
    assertThatExceptionOfType(StringIndexOutOfBoundsException.class)
            .isThrownBy(() -> new StringSequence("x", 0, 2));
  }

  @Test
  void createFromString() {
    assertThat(new StringSequence("test")).hasToString("test");
  }

  @Test
  void subSequenceWithJustStartShouldReturnSubSequence() {
    assertThat(new StringSequence("smiles").subSequence(1)).hasToString("miles");
  }

  @Test
  void subSequenceShouldReturnSubSequence() {
    assertThat(new StringSequence("hamburger").subSequence(4, 8)).hasToString("urge");
    assertThat(new StringSequence("smiles").subSequence(1, 5)).hasToString("mile");
  }

  @Test
  void subSequenceWhenCalledMultipleTimesShouldReturnSubSequence() {
    assertThat(new StringSequence("hamburger").subSequence(4, 8).subSequence(1, 3)).hasToString("rg");
  }

  @Test
  void subSequenceWhenEndPastExistingEndShouldThrowException() {
    StringSequence sequence = new StringSequence("abcde").subSequence(1, 4);
    assertThat(sequence).hasToString("bcd");
    assertThat(sequence.subSequence(2, 3)).hasToString("d");
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> sequence.subSequence(3, 4));
  }

  @Test
  void subSequenceWhenStartPastExistingEndShouldThrowException() {
    StringSequence sequence = new StringSequence("abcde").subSequence(1, 4);
    assertThat(sequence).hasToString("bcd");
    assertThat(sequence.subSequence(2, 3)).hasToString("d");
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> sequence.subSequence(4, 3));
  }

  @Test
  void isEmptyWhenEmptyShouldReturnTrue() {
    assertThat(new StringSequence("").isEmpty()).isTrue();
  }

  @Test
  void isEmptyWhenNotEmptyShouldReturnFalse() {
    assertThat(new StringSequence("x").isEmpty()).isFalse();
  }

  @Test
  void lengthShouldReturnLength() {
    StringSequence sequence = new StringSequence("hamburger");
    assertThat(sequence).hasSize(9);
    assertThat(sequence.subSequence(4, 8)).hasSize(4);
  }

  @Test
  void charAtShouldReturnChar() {
    StringSequence sequence = new StringSequence("hamburger");
    assertThat(sequence.charAt(0)).isEqualTo('h');
    assertThat(sequence.charAt(1)).isEqualTo('a');
    assertThat(sequence.subSequence(4, 8).charAt(0)).isEqualTo('u');
    assertThat(sequence.subSequence(4, 8).charAt(1)).isEqualTo('r');
  }

  @Test
  void indexOfCharShouldReturnIndexOf() {
    StringSequence sequence = new StringSequence("aabbaacc");
    assertThat(sequence.indexOf('a')).isZero();
    assertThat(sequence.indexOf('b')).isEqualTo(2);
    assertThat(sequence.subSequence(2).indexOf('a')).isEqualTo(2);
  }

  @Test
  void indexOfStringShouldReturnIndexOf() {
    StringSequence sequence = new StringSequence("aabbaacc");
    assertThat(sequence.indexOf('a')).isZero();
    assertThat(sequence.indexOf('b')).isEqualTo(2);
    assertThat(sequence.subSequence(2).indexOf('a')).isEqualTo(2);
  }

  @Test
  void indexOfStringFromIndexShouldReturnIndexOf() {
    StringSequence sequence = new StringSequence("aabbaacc");
    assertThat(sequence.indexOf("a", 2)).isEqualTo(4);
    assertThat(sequence.indexOf("b", 3)).isEqualTo(3);
    assertThat(sequence.subSequence(2).indexOf("a", 3)).isEqualTo(3);
  }

  @Test
  void hashCodeShouldBeSameAsString() {
    assertThat(new StringSequence("hamburger")).hasSameHashCodeAs("hamburger");
    assertThat(new StringSequence("hamburger").subSequence(4, 8)).hasSameHashCodeAs("urge");
  }

  @Test
  void equalsWhenSameContentShouldMatch() {
    StringSequence a = new StringSequence("hamburger").subSequence(4, 8);
    StringSequence b = new StringSequence("urge");
    StringSequence c = new StringSequence("urgh");
    assertThat(a).isEqualTo(b).isNotEqualTo(c);
  }

  @Test
  void notEqualsWhenSequencesOfDifferentLength() {
    StringSequence a = new StringSequence("abcd");
    StringSequence b = new StringSequence("ef");
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void startsWithWhenExactMatch() {
    assertThat(new StringSequence("abc").startsWith("abc")).isTrue();
  }

  @Test
  void startsWithWhenLongerAndStartsWith() {
    assertThat(new StringSequence("abcd").startsWith("abc")).isTrue();
  }

  @Test
  void startsWithWhenLongerAndDoesNotStartWith() {
    assertThat(new StringSequence("abcd").startsWith("abx")).isFalse();
  }

  @Test
  void startsWithWhenShorterAndDoesNotStartWith() {
    assertThat(new StringSequence("ab").startsWith("abc")).isFalse();
    assertThat(new StringSequence("ab").startsWith("c")).isFalse();
  }

  @Test
  void startsWithOffsetWhenExactMatch() {
    assertThat(new StringSequence("xabc").startsWith("abc", 1)).isTrue();
  }

  @Test
  void startsWithOffsetWhenLongerAndStartsWith() {
    assertThat(new StringSequence("xabcd").startsWith("abc", 1)).isTrue();
  }

  @Test
  void startsWithOffsetWhenLongerAndDoesNotStartWith() {
    assertThat(new StringSequence("xabcd").startsWith("abx", 1)).isFalse();
  }

  @Test
  void startsWithOffsetWhenShorterAndDoesNotStartWith() {
    assertThat(new StringSequence("xab").startsWith("abc", 1)).isFalse();
    assertThat(new StringSequence("xab").startsWith("c", 1)).isFalse();
  }

  @Test
  void startsWithOnSubstringTailWhenMatch() {
    StringSequence subSequence = new StringSequence("xabc").subSequence(1);
    assertThat(subSequence.startsWith("abc")).isTrue();
    assertThat(subSequence.startsWith("abcd")).isFalse();
  }

  @Test
  void startsWithOnSubstringMiddleWhenMatch() {
    StringSequence subSequence = new StringSequence("xabc").subSequence(1, 3);
    assertThat(subSequence.startsWith("ab")).isTrue();
    assertThat(subSequence.startsWith("abc")).isFalse();
  }

}
