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

package infra.app.loader.zip;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ZipString}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ZipStringTests {

  @ParameterizedTest
  @EnumSource
  void hashGeneratesCorrectHashCode(HashSourceType sourceType) throws Exception {
    testHash(sourceType, true, "abcABC123xyz!");
    testHash(sourceType, false, "abcABC123xyz!");
  }

  @ParameterizedTest
  @EnumSource
  void hashWhenHasSpecialCharsGeneratesCorrectHashCode(HashSourceType sourceType) throws Exception {
    testHash(sourceType, true, "special/\u00EB.dat");
  }

  @ParameterizedTest
  @EnumSource
  void hashWhenHasCyrillicCharsGeneratesCorrectHashCode(HashSourceType sourceType) throws Exception {
    testHash(sourceType, true, "\u0432\u0435\u0441\u043D\u0430");
  }

  @ParameterizedTest
  @EnumSource
  void hashWhenHasEmojiGeneratesCorrectHashCode(HashSourceType sourceType) throws Exception {
    testHash(sourceType, true, "\ud83d\udca9");
  }

  @ParameterizedTest
  @EnumSource
  void hashWhenOnlyDifferenceIsEndSlashGeneratesSameHashCode(HashSourceType sourceType) throws Exception {
    testHash(sourceType, "", true, "/".hashCode());
    testHash(sourceType, "/", true, "/".hashCode());
    testHash(sourceType, "a/b", true, "a/b/".hashCode());
    testHash(sourceType, "a/b/", true, "a/b/".hashCode());
  }

  void testHash(HashSourceType sourceType, boolean addSlash, String source) throws Exception {
    String expected = (addSlash && !source.endsWith("/")) ? source + "/" : source;
    testHash(sourceType, source, addSlash, expected.hashCode());
  }

  void testHash(HashSourceType sourceType, String source, boolean addEndSlash, int expected) throws Exception {
    switch (sourceType) {
      case STRING -> {
        assertThat(ZipString.hash(source, addEndSlash)).isEqualTo(expected);
      }
      case CHAR_SEQUENCE -> {
        CharSequence charSequence = new StringBuilder(source);
        assertThat(ZipString.hash(charSequence, addEndSlash)).isEqualTo(expected);
      }
      case DATA_BLOCK -> {
        ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(source.getBytes(StandardCharsets.UTF_8));
        assertThat(ZipString.hash(null, dataBlock, 0, (int) dataBlock.size(), addEndSlash)).isEqualTo(expected);
      }
      case SINGLE_BYTE_READ_DATA_BLOCK -> {
        ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(source.getBytes(StandardCharsets.UTF_8), 1);
        assertThat(ZipString.hash(null, dataBlock, 0, (int) dataBlock.size(), addEndSlash)).isEqualTo(expected);
      }
    }
  }

  @Test
  void matchesWhenExactMatchReturnsTrue() throws Exception {
    assertMatches("one/two/three", "one/two/three", false).isTrue();
  }

  @Test
  void matchesWhenNotMatchWithSameLengthReturnsFalse() throws Exception {
    assertMatches("one/two/three", "one/too/three", false).isFalse();
  }

  @Test
  void matchesWhenExactMatchWithSpecialCharsReturnsTrue() throws Exception {
    assertMatches("special/\u00EB.dat", "special/\u00EB.dat", false).isTrue();
  }

  @Test
  void matchesWhenExactMatchWithCyrillicCharsReturnsTrue() throws Exception {
    assertMatches("\u0432\u0435\u0441\u043D\u0430", "\u0432\u0435\u0441\u043D\u0430", false).isTrue();
  }

  @Test
  void matchesWhenNoMatchWithCyrillicCharsReturnsFalse() throws Exception {
    assertMatches("\u0432\u0435\u0441\u043D\u0430", "\u0432\u0435\u0441\u043D\u043D", false).isFalse();
  }

  @Test
  void matchesWhenExactMatchWithEmojiCharsReturnsTrue() throws Exception {
    assertMatches("\ud83d\udca9", "\ud83d\udca9", false).isTrue();
  }

  @Test
  void matchesWithAddSlash() throws Exception {
    assertMatches("META-INF/MANFIFEST.MF", "META-INF/MANFIFEST.MF", true).isTrue();
    assertMatches("one/two/three/", "one/two/three", true).isTrue();
    assertMatches("one/two/three", "one/two/three/", true).isFalse();
    assertMatches("one/two/three/", "one/too/three", true).isFalse();
    assertMatches("one/two/three", "one/too/three/", true).isFalse();
    assertMatches("one/two/three//", "one/two/three", true).isFalse();
    assertMatches("one/two/three", "one/two/three//", true).isFalse();
  }

  @Test
  void matchesWhenDataBlockShorterThenCharSequenceReturnsFalse() throws Exception {
    assertMatches("one/two/thre", "one/two/three", false).isFalse();
  }

  @Test
  void matchesWhenCharSequenceShorterThanDataBlockReturnsFalse() throws Exception {
    assertMatches("one/two/three", "one/two/thre", false).isFalse();
  }

  @Test
  void startsWithWhenStartsWith() throws Exception {
    assertStartsWith("one/two", "one/").isEqualTo(4);
  }

  @Test
  void startsWithWhenExact() throws Exception {
    assertStartsWith("one/", "one/").isEqualTo(4);
  }

  @Test
  void startsWithWhenTooShort() throws Exception {
    assertStartsWith("one/two", "one/two/three/").isEqualTo(-1);
  }

  @Test
  void startsWithWhenDoesNotStartWith() throws Exception {
    assertStartsWith("one/three/", "one/two/").isEqualTo(-1);
  }

  @Test
  void zipStringWhenMultiCodePointAtBufferBoundary() throws Exception {
    StringBuilder source = new StringBuilder();
    source.append("A".repeat(ZipString.BUFFER_SIZE - 1));
    source.append("\u1EFF");
    String charSequence = source.toString();
    source.append("suffix");
    assertStartsWith(source.toString(), charSequence);
  }

  private AbstractBooleanAssert<?> assertMatches(String source, CharSequence charSequence, boolean addSlash)
          throws Exception {
    ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(source.getBytes(StandardCharsets.UTF_8));
    return assertThat(ZipString.matches(null, dataBlock, 0, (int) dataBlock.size(), charSequence, addSlash));
  }

  private AbstractIntegerAssert<?> assertStartsWith(String source, CharSequence charSequence) throws IOException {
    ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(source.getBytes(StandardCharsets.UTF_8));
    return assertThat(ZipString.startsWith(null, dataBlock, 0, (int) dataBlock.size(), charSequence));
  }

  enum HashSourceType {

    STRING, CHAR_SEQUENCE, DATA_BLOCK, SINGLE_BYTE_READ_DATA_BLOCK

  }

}
