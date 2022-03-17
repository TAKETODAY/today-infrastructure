/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cn.taketoday.core.testfixture.io.SerializationTestUtils;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link MimeType}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Dimitrios Liapis
 */
public class MimeTypeTest {

  @Test
  public void slashInSubtype() {
    assertThatIllegalArgumentException().isThrownBy(() -> new MimeType("text", "/"));
  }

  @Test
  public void valueOfNoSubtype() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeType.valueOf("audio"));
  }

  @Test
  public void valueOfNoSubtypeSlash() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeType.valueOf("audio/"));
  }

  @Test
  public void valueOfIllegalType() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeType.valueOf("audio(/basic"));
  }

  @Test
  public void valueOfIllegalSubtype() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeType.valueOf("audio/basic)"));
  }

  @Test
  public void valueOfIllegalCharset() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeType.valueOf("text/html; charset=foo-bar"));
  }

  @Test
  public void parseCharset() {
    String s = "text/html; charset=iso-8859-1";
    MimeType mimeType = MimeType.valueOf(s);
    assertThat(mimeType.getType()).as("Invalid type").isEqualTo("text");
    assertThat(mimeType.getSubtype()).as("Invalid subtype").isEqualTo("html");
    assertThat(mimeType.getCharset()).as("Invalid charset").isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  public void parseQuotedCharset() {
    String s = "application/xml;charset=\"utf-8\"";
    MimeType mimeType = MimeType.valueOf(s);
    assertThat(mimeType.getType()).as("Invalid type").isEqualTo("application");
    assertThat(mimeType.getSubtype()).as("Invalid subtype").isEqualTo("xml");
    assertThat(mimeType.getCharset()).as("Invalid charset").isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  public void parseQuotedSeparator() {
    String s = "application/xop+xml;charset=utf-8;type=\"application/soap+xml;action=\\\"https://x.y.z\\\"\"";
    MimeType mimeType = MimeType.valueOf(s);
    assertThat(mimeType.getType()).as("Invalid type").isEqualTo("application");
    assertThat(mimeType.getSubtype()).as("Invalid subtype").isEqualTo("xop+xml");
    assertThat(mimeType.getCharset()).as("Invalid charset").isEqualTo(StandardCharsets.UTF_8);
    assertThat(mimeType.getParameter("type")).isEqualTo("\"application/soap+xml;action=\\\"https://x.y.z\\\"\"");
  }

  @Test
  public void includes() {
    MimeType textPlain = MimeTypeUtils.TEXT_PLAIN;
    assertThat(textPlain.includes(textPlain)).as("Equal types is not inclusive").isTrue();
    MimeType allText = new MimeType("text");

    assertThat(allText.includes(textPlain)).as("All subtypes is not inclusive").isTrue();
    assertThat(textPlain.includes(allText)).as("All subtypes is inclusive").isFalse();

    assertThat(MimeTypeUtils.ALL.includes(textPlain)).as("All types is not inclusive").isTrue();
    assertThat(textPlain.includes(MimeTypeUtils.ALL)).as("All types is inclusive").isFalse();

    assertThat(MimeTypeUtils.ALL.includes(textPlain)).as("All types is not inclusive").isTrue();
    assertThat(textPlain.includes(MimeTypeUtils.ALL)).as("All types is inclusive").isFalse();

    MimeType applicationSoapXml = new MimeType("application", "soap+xml");
    MimeType applicationWildcardXml = new MimeType("application", "*+xml");
    MimeType suffixXml = new MimeType("application", "x.y+z+xml"); // SPR-15795

    assertThat(applicationSoapXml.includes(applicationSoapXml)).isTrue();
    assertThat(applicationWildcardXml.includes(applicationWildcardXml)).isTrue();
    assertThat(applicationWildcardXml.includes(suffixXml)).isTrue();

    assertThat(applicationWildcardXml.includes(applicationSoapXml)).isTrue();
    assertThat(applicationSoapXml.includes(applicationWildcardXml)).isFalse();
    assertThat(suffixXml.includes(applicationWildcardXml)).isFalse();

    assertThat(applicationWildcardXml.includes(MimeTypeUtils.APPLICATION_JSON)).isFalse();
  }

  @Test
  public void isCompatible() {
    MimeType textPlain = MimeTypeUtils.TEXT_PLAIN;
    assertThat(textPlain.isCompatibleWith(textPlain)).as("Equal types is not compatible").isTrue();
    MimeType allText = new MimeType("text");

    assertThat(allText.isCompatibleWith(textPlain)).as("All subtypes is not compatible").isTrue();
    assertThat(textPlain.isCompatibleWith(allText)).as("All subtypes is not compatible").isTrue();

    assertThat(MimeTypeUtils.ALL.isCompatibleWith(textPlain)).as("All types is not compatible").isTrue();
    assertThat(textPlain.isCompatibleWith(MimeTypeUtils.ALL)).as("All types is not compatible").isTrue();

    assertThat(MimeTypeUtils.ALL.isCompatibleWith(textPlain)).as("All types is not compatible").isTrue();
    assertThat(textPlain.isCompatibleWith(MimeTypeUtils.ALL)).as("All types is compatible").isTrue();

    MimeType applicationSoapXml = new MimeType("application", "soap+xml");
    MimeType applicationWildcardXml = new MimeType("application", "*+xml");
    MimeType suffixXml = new MimeType("application", "x.y+z+xml"); // SPR-15795

    assertThat(applicationSoapXml.isCompatibleWith(applicationSoapXml)).isTrue();
    assertThat(applicationWildcardXml.isCompatibleWith(applicationWildcardXml)).isTrue();
    assertThat(applicationWildcardXml.isCompatibleWith(suffixXml)).isTrue();

    assertThat(applicationWildcardXml.isCompatibleWith(applicationSoapXml)).isTrue();
    assertThat(applicationSoapXml.isCompatibleWith(applicationWildcardXml)).isTrue();
    assertThat(suffixXml.isCompatibleWith(applicationWildcardXml)).isTrue();

    assertThat(applicationWildcardXml.isCompatibleWith(MimeTypeUtils.APPLICATION_JSON)).isFalse();
  }

  @Test
  public void testToString() {
    MimeType mimeType = new MimeType("text", "plain");
    String result = mimeType.toString();
    assertThat(result).as("Invalid toString() returned").isEqualTo("text/plain");
  }

  @Test
  public void parseMimeType() {
    String s = "audio/*";
    MimeType mimeType = MimeTypeUtils.parseMimeType(s);
    assertThat(mimeType.getType()).as("Invalid type").isEqualTo("audio");
    assertThat(mimeType.getSubtype()).as("Invalid subtype").isEqualTo("*");
  }

  @Test
  public void parseMimeTypeNoSubtype() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("audio"));
  }

  @Test
  public void parseMimeTypeNoSubtypeSlash() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("audio/"));
  }

  @Test
  public void parseMimeTypeTypeRange() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("*/json"));
  }

  @Test
  public void parseMimeTypeIllegalType() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("audio(/basic"));
  }

  @Test
  public void parseMimeTypeIllegalSubtype() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("audio/basic)"));
  }

  @Test
  public void parseMimeTypeMissingTypeAndSubtype() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("     ;a=b"));
  }

  @Test
  public void parseMimeTypeEmptyParameterAttribute() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("audio/*;=value"));
  }

  @Test
  public void parseMimeTypeEmptyParameterValue() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("audio/*;attr="));
  }

  @Test
  public void parseMimeTypeIllegalParameterAttribute() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("audio/*;attr<=value"));
  }

  @Test
  public void parseMimeTypeIllegalParameterValue() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("audio/*;attr=v>alue"));
  }

  @Test
  public void parseMimeTypeIllegalCharset() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType(
            "text/html; charset=foo-bar"));
  }

  @Test // SPR-8917
  public void parseMimeTypeQuotedParameterValue() {
    MimeType mimeType = MimeTypeUtils.parseMimeType("audio/*;attr=\"v>alue\"");
    assertThat(mimeType.getParameter("attr")).isEqualTo("\"v>alue\"");
  }

  @Test // SPR-8917
  public void parseMimeTypeSingleQuotedParameterValue() {
    MimeType mimeType = MimeTypeUtils.parseMimeType("audio/*;attr='v>alue'");
    assertThat(mimeType.getParameter("attr")).isEqualTo("'v>alue'");
  }

  @Test // SPR-16630
  public void parseMimeTypeWithSpacesAroundEquals() {
    MimeType mimeType = MimeTypeUtils.parseMimeType("multipart/x-mixed-replace;boundary = --myboundary");
    assertThat(mimeType.getParameter("boundary")).isEqualTo("--myboundary");
  }

  @Test // SPR-16630
  public void parseMimeTypeWithSpacesAroundEqualsAndQuotedValue() {
    MimeType mimeType = MimeTypeUtils.parseMimeType("text/plain; foo = \" bar \" ");
    assertThat(mimeType.getParameter("foo")).isEqualTo("\" bar \"");
  }

  @Test
  public void parseMimeTypeIllegalQuotedParameterValue() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType("audio/*;attr=\""));
  }

  @Test
  public void parseMimeTypeNull() {
    assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> MimeTypeUtils.parseMimeType(null));
  }

  @Test
  public void parseMimeTypes() {
    String s = "text/plain, text/html, text/x-dvi, text/x-c";
    List<MimeType> mimeTypes = MimeTypeUtils.parseMimeTypes(s);
    Assertions.assertThat(mimeTypes).as("No mime types returned").isNotNull();
    assertThat(mimeTypes.size()).as("Invalid amount of mime types").isEqualTo(4);

    mimeTypes = MimeTypeUtils.parseMimeTypes(null);
    Assertions.assertThat(mimeTypes).as("No mime types returned").isNotNull();
    assertThat(mimeTypes.size()).as("Invalid amount of mime types").isEqualTo(0);
  }

  @Test // gh-23241
  public void parseMimeTypesWithTrailingComma() {
    List<MimeType> mimeTypes = MimeTypeUtils.parseMimeTypes("text/plain, text/html,");
    Assertions.assertThat(mimeTypes).as("No mime types returned").isNotNull();
    assertThat(mimeTypes.size()).as("Incorrect number of mime types").isEqualTo(2);
  }

  @Test // SPR-17459
  public void parseMimeTypesWithQuotedParameters() {
    testWithQuotedParameters("foo/bar;param=\",\"");
    testWithQuotedParameters("foo/bar;param=\"s,a,\"");
    testWithQuotedParameters("foo/bar;param=\"s,\"", "text/x-c");
    testWithQuotedParameters("foo/bar;param=\"a\\\"b,c\"");
    testWithQuotedParameters("foo/bar;param=\"\\\\\"");
    testWithQuotedParameters("foo/bar;param=\"\\,\\\"");
  }

  private void testWithQuotedParameters(String... mimeTypes) {
    String s = String.join(",", mimeTypes);
    List<MimeType> actual = MimeTypeUtils.parseMimeTypes(s);
    assertThat(actual.size()).isEqualTo(mimeTypes.length);
    for (int i = 0; i < mimeTypes.length; i++) {
      assertThat(actual.get(i).toString()).isEqualTo(mimeTypes[i]);
    }
  }

  @Test
  public void compareTo() {
    MimeType audioBasic = new MimeType("audio", "basic");
    MimeType audio = new MimeType("audio");
    MimeType audioWave = new MimeType("audio", "wave");
    MimeType audioBasicLevel = new MimeType("audio", "basic", singletonMap("level", "1"));

    // equal
    assertThat(audioBasic.compareTo(audioBasic)).as("Invalid comparison result").isEqualTo(0);
    assertThat(audio.compareTo(audio)).as("Invalid comparison result").isEqualTo(0);
    assertThat(audioBasicLevel.compareTo(audioBasicLevel)).as("Invalid comparison result").isEqualTo(0);

    assertThat(audioBasicLevel.compareTo(audio) > 0).as("Invalid comparison result").isTrue();

    List<MimeType> expected = new ArrayList<>();
    expected.add(audio);
    expected.add(audioBasic);
    expected.add(audioBasicLevel);
    expected.add(audioWave);

    List<MimeType> result = new ArrayList<>(expected);
    Random rnd = new Random();
    // shuffle & sort 10 times
    for (int i = 0; i < 10; i++) {
      Collections.shuffle(result, rnd);
      Collections.sort(result);

      for (int j = 0; j < result.size(); j++) {
        assertThat(result.get(j)).as("Invalid media type at " + j + ", run " + i).isSameAs(expected.get(j));
      }
    }
  }

  @Test
  public void compareToCaseSensitivity() {
    MimeType m1 = new MimeType("audio", "basic");
    MimeType m2 = new MimeType("Audio", "Basic");
    assertThat(m1.compareTo(m2)).as("Invalid comparison result").isEqualTo(0);
    assertThat(m2.compareTo(m1)).as("Invalid comparison result").isEqualTo(0);

    m1 = new MimeType("audio", "basic", singletonMap("foo", "bar"));
    m2 = new MimeType("audio", "basic", singletonMap("Foo", "bar"));
    assertThat(m1.compareTo(m2)).as("Invalid comparison result").isEqualTo(0);
    assertThat(m2.compareTo(m1)).as("Invalid comparison result").isEqualTo(0);

    m1 = new MimeType("audio", "basic", singletonMap("foo", "bar"));
    m2 = new MimeType("audio", "basic", singletonMap("foo", "Bar"));
    assertThat(m1.compareTo(m2) != 0).as("Invalid comparison result").isTrue();
    assertThat(m2.compareTo(m1) != 0).as("Invalid comparison result").isTrue();
  }

  @Test
  void isMoreSpecific() {
    MimeType audioBasic = new MimeType("audio", "basic");
    MimeType audio = new MimeType("audio");
    MimeType audioWave = new MimeType("audio", "wave");
    MimeType audioBasicLevel = new MimeType("audio", "basic", singletonMap("level", "1"));

    assertThat(audioBasic.isMoreSpecific(audioBasicLevel)).isFalse();
    assertThat(audioBasicLevel.isMoreSpecific(audioBasic)).isTrue();

    assertThat(audio.isMoreSpecific(MimeTypeUtils.ALL)).isTrue();
    assertThat(MimeTypeUtils.ALL.isMoreSpecific(audio)).isFalse();

    assertThat(audioBasicLevel.isMoreSpecific(audioBasic)).isTrue();
    assertThat(audioBasic.isMoreSpecific(audioBasicLevel)).isFalse();

    assertThat(audioBasic.isMoreSpecific(MimeTypeUtils.TEXT_HTML)).isFalse();
    assertThat(audioBasic.isMoreSpecific(audioWave)).isFalse();
    assertThat(audioBasicLevel.isMoreSpecific(MimeTypeUtils.TEXT_HTML)).isFalse();
  }

  @Test
  void isLessSpecific() {
    MimeType audioBasic = new MimeType("audio", "basic");
    MimeType audio = new MimeType("audio");
    MimeType audioWave = new MimeType("audio", "wave");
    MimeType audioBasicLevel = new MimeType("audio", "basic", singletonMap("level", "1"));

    assertThat(audioBasic.isLessSpecific(audioBasicLevel)).isTrue();
    assertThat(audioBasicLevel.isLessSpecific(audioBasic)).isFalse();

    assertThat(audio.isLessSpecific(MimeTypeUtils.ALL)).isFalse();
    assertThat(MimeTypeUtils.ALL.isLessSpecific(audio)).isTrue();

    assertThat(audioBasicLevel.isLessSpecific(audioBasic)).isFalse();
    assertThat(audioBasic.isLessSpecific(audioBasicLevel)).isTrue();

    assertThat(audioBasic.isLessSpecific(MimeTypeUtils.TEXT_HTML)).isFalse();
    assertThat(audioBasic.isLessSpecific(audioWave)).isFalse();
    assertThat(audioBasicLevel.isLessSpecific(MimeTypeUtils.TEXT_HTML)).isFalse();
  }

  @Test
  void sortBySpecificity() {
    MimeType audioBasic = new MimeType("audio", "basic");
    MimeType audio = new MimeType("audio");
    MimeType audioWave = new MimeType("audio", "wave");
    MimeType audioBasicLevel = new MimeType("audio", "basic", singletonMap("level", "1"));

    List<MimeType> mimeTypes = new ArrayList<>(List.of(MimeTypeUtils.ALL, audio, audioWave, audioBasic,
            audioBasicLevel));
    MimeTypeUtils.sortBySpecificity(mimeTypes);

    assertThat(mimeTypes).containsExactly(audioWave, audioBasicLevel, audioBasic, audio, MimeTypeUtils.ALL);
  }

  @Test
  void bubbleSort() {
    List<Integer> list = new ArrayList<>(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1));
    MimeTypeUtils.bubbleSort(list, (i1, i2) -> i1 > i2);
    assertThat(list).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  @Test
    // SPR-13157
  void equalsIsCaseInsensitiveForCharsets() {
    MimeType m1 = new MimeType("text", "plain", singletonMap("charset", "UTF-8"));
    MimeType m2 = new MimeType("text", "plain", singletonMap("charset", "utf-8"));
    assertThat(m2).isEqualTo(m1);
    assertThat(m1).isEqualTo(m2);
    assertThat(m1.compareTo(m2)).isEqualTo(0);
    assertThat(m2.compareTo(m1)).isEqualTo(0);
  }

  @Test
    // gh-26127
  void serialize() throws Exception {
    MimeType original = new MimeType("text", "plain", StandardCharsets.UTF_8);
    MimeType deserialized = SerializationTestUtils.serializeAndDeserialize(original);
    assertThat(deserialized).isEqualTo(original);
    assertThat(original).isEqualTo(deserialized);
  }

  @Test
  void parseSubtypeSuffix() {
    MimeType type = new MimeType("application", "vdn.something+json");
    assertThat(type.getSubtypeSuffix()).isEqualTo("json");
    type = new MimeType("application", "vdn.something");
    assertThat(type.getSubtypeSuffix()).isNull();
    type = new MimeType("application", "vdn.something+");
    assertThat(type.getSubtypeSuffix()).isEqualTo("");
    type = new MimeType("application", "vdn.some+thing+json");
    assertThat(type.getSubtypeSuffix()).isEqualTo("json");
  }

}
