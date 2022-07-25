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

package cn.taketoday.http;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import cn.taketoday.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/23 00:39
 */
class MediaTypeTests {

  @Test
  public void testToString() throws Exception {
    MediaType mediaType = new MediaType("text", "plain", 0.7);
    String result = mediaType.toString();
    assertThat(result).as("Invalid toString() returned").isEqualTo("text/plain;q=0.7");
  }

  @Test
  public void slashInType() {
    assertThatIllegalArgumentException().isThrownBy(() -> new MediaType("text/plain"));
  }

  @Test
  public void slashInSubtype() {
    assertThatIllegalArgumentException().isThrownBy(() -> new MediaType("text", "/"));
  }

  @Test
  public void getDefaultQualityValue() {
    MediaType mediaType = new MediaType("text", "plain");
    assertThat(mediaType.getQualityValue()).as("Invalid quality value").isCloseTo(1D, Assertions.within(0D));
  }

  @Test
  public void parseMediaType() throws Exception {
    String s = "audio/*; q=0.2";
    MediaType mediaType = MediaType.parseMediaType(s);
    assertThat(mediaType.getType()).as("Invalid type").isEqualTo("audio");
    assertThat(mediaType.getSubtype()).as("Invalid subtype").isEqualTo("*");
    assertThat(mediaType.getQualityValue()).as("Invalid quality factor").isCloseTo(0.2D, Assertions.within(0D));
  }

  @Test
  public void parseMediaTypeNoSubtype() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("audio"));
  }

  @Test
  public void parseMediaTypeNoSubtypeSlash() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("audio/"));
  }

  @Test
  public void parseMediaTypeTypeRange() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("*/json"));
  }

  @Test
  public void parseMediaTypeIllegalType() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("audio(/basic"));
  }

  @Test
  public void parseMediaTypeIllegalSubtype() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("audio/basic)"));
  }

  @Test
  public void parseMediaTypeEmptyParameterAttribute() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("audio/*;=value"));
  }

  @Test
  public void parseMediaTypeEmptyParameterValue() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("audio/*;attr="));
  }

  @Test
  public void parseMediaTypeIllegalParameterAttribute() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("audio/*;attr<=value"));
  }

  @Test
  public void parseMediaTypeIllegalParameterValue() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("audio/*;attr=v>alue"));
  }

  @Test
  public void parseMediaTypeIllegalQualityFactor() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("audio/basic;q=1.1"));
  }

  @Test
  public void parseMediaTypeIllegalCharset() {
    assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() -> MediaType.parseMediaType("text/html; charset=foo-bar"));
  }

  @Test
  public void parseURLConnectionMediaType() throws Exception {
    String s = "*; q=.2";
    MediaType mediaType = MediaType.parseMediaType(s);
    assertThat(mediaType.getType()).as("Invalid type").isEqualTo("*");
    assertThat(mediaType.getSubtype()).as("Invalid subtype").isEqualTo("*");
    assertThat(mediaType.getQualityValue()).as("Invalid quality factor").isCloseTo(0.2D, Assertions.within(0D));
  }

  @Test
  public void parseMediaTypes() throws Exception {
    String s = "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c";
    List<MediaType> mediaTypes = MediaType.parseMediaTypes(s);
    assertThat(mediaTypes).as("No media types returned").isNotNull();
    assertThat(mediaTypes.size()).as("Invalid amount of media types").isEqualTo(4);

    mediaTypes = MediaType.parseMediaTypes("");
    assertThat(mediaTypes).as("No media types returned").isNotNull();
    assertThat(mediaTypes.size()).as("Invalid amount of media types").isEqualTo(0);
  }

  public void parseMediaTypesWithTrailingComma() {
    List<MediaType> mediaTypes = MediaType.parseMediaTypes("text/plain, text/html, ");
    assertThat(mediaTypes).as("No media types returned").isNotNull();
    assertThat(mediaTypes.size()).as("Incorrect number of media types").isEqualTo(2);
  }

  @Test
  public void compareTo() {
    MediaType audioBasic = new MediaType("audio", "basic");
    MediaType audio = new MediaType("audio");
    MediaType audioWave = new MediaType("audio", "wave");
    MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
    MediaType audioBasic07 = new MediaType("audio", "basic", 0.7);

    // equal
    assertThat(audioBasic.compareTo(audioBasic)).as("Invalid comparison result").isEqualTo(0);
    assertThat(audio.compareTo(audio)).as("Invalid comparison result").isEqualTo(0);
    assertThat(audioBasicLevel.compareTo(audioBasicLevel)).as("Invalid comparison result").isEqualTo(0);

    assertThat(audioBasicLevel.compareTo(audio) > 0).as("Invalid comparison result").isTrue();

    List<MediaType> expected = new ArrayList<>();
    expected.add(audio);
    expected.add(audioBasic);
    expected.add(audioBasicLevel);
    expected.add(audioBasic07);
    expected.add(audioWave);

    List<MediaType> result = new ArrayList<>(expected);
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
  public void compareToConsistentWithEquals() {
    MediaType m1 = MediaType.parseMediaType("text/html; q=0.7; charset=iso-8859-1");
    MediaType m2 = MediaType.parseMediaType("text/html; charset=iso-8859-1; q=0.7");

    assertThat(m2).as("Media types not equal").isEqualTo(m1);
    assertThat(m1.compareTo(m2)).as("compareTo() not consistent with equals").isEqualTo(0);
    assertThat(m2.compareTo(m1)).as("compareTo() not consistent with equals").isEqualTo(0);

    m1 = MediaType.parseMediaType("text/html; q=0.7; charset=iso-8859-1");
    m2 = MediaType.parseMediaType("text/html; Q=0.7; charset=iso-8859-1");
    assertThat(m2).as("Media types not equal").isEqualTo(m1);
    assertThat(m1.compareTo(m2)).as("compareTo() not consistent with equals").isEqualTo(0);
    assertThat(m2.compareTo(m1)).as("compareTo() not consistent with equals").isEqualTo(0);
  }

  @Test
  public void compareToCaseSensitivity() {
    MediaType m1 = new MediaType("audio", "basic");
    MediaType m2 = new MediaType("Audio", "Basic");
    assertThat(m1.compareTo(m2)).as("Invalid comparison result").isEqualTo(0);
    assertThat(m2.compareTo(m1)).as("Invalid comparison result").isEqualTo(0);

    m1 = new MediaType("audio", "basic", Collections.singletonMap("foo", "bar"));
    m2 = new MediaType("audio", "basic", Collections.singletonMap("Foo", "bar"));
    assertThat(m1.compareTo(m2)).as("Invalid comparison result").isEqualTo(0);
    assertThat(m2.compareTo(m1)).as("Invalid comparison result").isEqualTo(0);

    m1 = new MediaType("audio", "basic", Collections.singletonMap("foo", "bar"));
    m2 = new MediaType("audio", "basic", Collections.singletonMap("foo", "Bar"));
    assertThat(m1.compareTo(m2) != 0).as("Invalid comparison result").isTrue();
    assertThat(m2.compareTo(m1) != 0).as("Invalid comparison result").isTrue();

  }

  @Test
  public void specificityComparator() throws Exception {
    MediaType audioBasic = new MediaType("audio", "basic");
    MediaType audioWave = new MediaType("audio", "wave");
    MediaType audio = new MediaType("audio");
    MediaType audio03 = new MediaType("audio", "*", 0.3);
    MediaType audio07 = new MediaType("audio", "*", 0.7);
    MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
    MediaType textHtml = new MediaType("text", "html");
    MediaType allXml = new MediaType("application", "*+xml");
    MediaType all = MediaType.ALL;

    Comparator<MediaType> comp = MediaType.SPECIFICITY_COMPARATOR;

    // equal
    assertThat(comp.compare(audioBasic, audioBasic)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(audio, audio)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(audio07, audio07)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(audio03, audio03)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(audioBasicLevel, audioBasicLevel)).as("Invalid comparison result").isEqualTo(0);

    // specific to unspecific
    assertThat(comp.compare(audioBasic, audio) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audioBasic, all) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audio, all) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(MediaType.APPLICATION_XHTML_XML, allXml) < 0).as("Invalid comparison result").isTrue();

    // unspecific to specific
    assertThat(comp.compare(audio, audioBasic) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(allXml, MediaType.APPLICATION_XHTML_XML) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(all, audioBasic) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(all, audio) > 0).as("Invalid comparison result").isTrue();

    // qualifiers
    assertThat(comp.compare(audio, audio07) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audio07, audio) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audio07, audio03) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audio03, audio07) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audio03, all) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(all, audio03) > 0).as("Invalid comparison result").isTrue();

    // other parameters
    assertThat(comp.compare(audioBasic, audioBasicLevel) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audioBasicLevel, audioBasic) < 0).as("Invalid comparison result").isTrue();

    // different types
    assertThat(comp.compare(audioBasic, textHtml)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(textHtml, audioBasic)).as("Invalid comparison result").isEqualTo(0);

    // different subtypes
    assertThat(comp.compare(audioBasic, audioWave)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(audioWave, audioBasic)).as("Invalid comparison result").isEqualTo(0);
  }

  @Test
  public void sortBySpecificityRelated() {
    MediaType audioBasic = new MediaType("audio", "basic");
    MediaType audio = new MediaType("audio");
    MediaType audio03 = new MediaType("audio", "*", 0.3);
    MediaType audio07 = new MediaType("audio", "*", 0.7);
    MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
    MediaType all = MediaType.ALL;

    List<MediaType> expected = new ArrayList<>();
    expected.add(audioBasicLevel);
    expected.add(audioBasic);
    expected.add(audio);
    expected.add(audio07);
    expected.add(audio03);
    expected.add(all);

    List<MediaType> result = new ArrayList<>(expected);
    Random rnd = new Random();
    // shuffle & sort 10 times
    for (int i = 0; i < 10; i++) {
      Collections.shuffle(result, rnd);
      MediaType.sortBySpecificity(result);

      for (int j = 0; j < result.size(); j++) {
        assertThat(result.get(j)).as("Invalid media type at " + j).isSameAs(expected.get(j));
      }
    }
  }

  @Test
  public void sortBySpecificityUnrelated() {
    MediaType audioBasic = new MediaType("audio", "basic");
    MediaType audioWave = new MediaType("audio", "wave");
    MediaType textHtml = new MediaType("text", "html");

    List<MediaType> expected = new ArrayList<>();
    expected.add(textHtml);
    expected.add(audioBasic);
    expected.add(audioWave);

    List<MediaType> result = new ArrayList<>(expected);
    MediaType.sortBySpecificity(result);

    for (int i = 0; i < result.size(); i++) {
      assertThat(result.get(i)).as("Invalid media type at " + i).isSameAs(expected.get(i));
    }

  }

  @Test
  public void qualityComparator() throws Exception {
    MediaType audioBasic = new MediaType("audio", "basic");
    MediaType audioWave = new MediaType("audio", "wave");
    MediaType audio = new MediaType("audio");
    MediaType audio03 = new MediaType("audio", "*", 0.3);
    MediaType audio07 = new MediaType("audio", "*", 0.7);
    MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
    MediaType textHtml = new MediaType("text", "html");
    MediaType allXml = new MediaType("application", "*+xml");
    MediaType all = MediaType.ALL;

    Comparator<MediaType> comp = MediaType.QUALITY_VALUE_COMPARATOR;

    // equal
    assertThat(comp.compare(audioBasic, audioBasic)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(audio, audio)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(audio07, audio07)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(audio03, audio03)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(audioBasicLevel, audioBasicLevel)).as("Invalid comparison result").isEqualTo(0);

    // specific to unspecific
    assertThat(comp.compare(audioBasic, audio) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audioBasic, all) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audio, all) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(MediaType.APPLICATION_XHTML_XML, allXml) < 0).as("Invalid comparison result").isTrue();

    // unspecific to specific
    assertThat(comp.compare(audio, audioBasic) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(all, audioBasic) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(all, audio) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(allXml, MediaType.APPLICATION_XHTML_XML) > 0).as("Invalid comparison result").isTrue();

    // qualifiers
    assertThat(comp.compare(audio, audio07) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audio07, audio) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audio07, audio03) < 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audio03, audio07) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audio03, all) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(all, audio03) < 0).as("Invalid comparison result").isTrue();

    // other parameters
    assertThat(comp.compare(audioBasic, audioBasicLevel) > 0).as("Invalid comparison result").isTrue();
    assertThat(comp.compare(audioBasicLevel, audioBasic) < 0).as("Invalid comparison result").isTrue();

    // different types
    assertThat(comp.compare(audioBasic, textHtml)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(textHtml, audioBasic)).as("Invalid comparison result").isEqualTo(0);

    // different subtypes
    assertThat(comp.compare(audioBasic, audioWave)).as("Invalid comparison result").isEqualTo(0);
    assertThat(comp.compare(audioWave, audioBasic)).as("Invalid comparison result").isEqualTo(0);
  }

  @Test
  public void sortByQualityRelated() {
    MediaType audioBasic = new MediaType("audio", "basic");
    MediaType audio = new MediaType("audio");
    MediaType audio03 = new MediaType("audio", "*", 0.3);
    MediaType audio07 = new MediaType("audio", "*", 0.7);
    MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
    MediaType all = MediaType.ALL;

    List<MediaType> expected = new ArrayList<>();
    expected.add(audioBasicLevel);
    expected.add(audioBasic);
    expected.add(audio);
    expected.add(all);
    expected.add(audio07);
    expected.add(audio03);

    List<MediaType> result = new ArrayList<>(expected);
    Random rnd = new Random();
    // shuffle & sort 10 times
    for (int i = 0; i < 10; i++) {
      Collections.shuffle(result, rnd);
      MediaType.sortByQualityValue(result);

      for (int j = 0; j < result.size(); j++) {
        assertThat(result.get(j)).as("Invalid media type at " + j).isSameAs(expected.get(j));
      }
    }
  }

  @Test
  public void sortByQualityUnrelated() {
    MediaType audioBasic = new MediaType("audio", "basic");
    MediaType audioWave = new MediaType("audio", "wave");
    MediaType textHtml = new MediaType("text", "html");

    List<MediaType> expected = new ArrayList<>();
    expected.add(textHtml);
    expected.add(audioBasic);
    expected.add(audioWave);

    List<MediaType> result = new ArrayList<>(expected);
    MediaType.sortBySpecificity(result);

    for (int i = 0; i < result.size(); i++) {
      assertThat(result.get(i)).as("Invalid media type at " + i).isSameAs(expected.get(i));
    }
  }

  @Test
  public void isConcrete() {
    assertThat(MediaType.TEXT_PLAIN.isConcrete()).as("text/plain not concrete").isTrue();
    assertThat(MediaType.ALL.isConcrete()).as("*/* concrete").isFalse();
    assertThat(new MediaType("text", "*").isConcrete()).as("text/* concrete").isFalse();
  }

  @Test
  void isMoreSpecific() {
    MediaType audio = new MediaType("audio");
    MediaType audioBasic = new MediaType("audio", "basic");
    MediaType audioBasic07 = new MediaType("audio", "basic", 0.7);
    MediaType audioBasic03 = new MediaType("audio", "basic", 0.3);

    assertThat(audioBasic.isMoreSpecific(audio)).isTrue();
    assertThat(audio.isMoreSpecific(audioBasic)).isFalse();

    assertThat(audio.isMoreSpecific(audioBasic07)).isTrue();
    assertThat(audioBasic07.isMoreSpecific(audio)).isFalse();

    assertThat(audioBasic07.isMoreSpecific(audioBasic03)).isTrue();
    assertThat(audioBasic03.isMoreSpecific(audioBasic07)).isFalse();

    assertThat(audioBasic.isMoreSpecific(MediaType.TEXT_HTML)).isFalse();
  }

  @Test
  void isLessSpecific() {
    MediaType audio = new MediaType("audio");
    MediaType audioBasic = new MediaType("audio", "basic");
    MediaType audioBasic07 = new MediaType("audio", "basic", 0.7);
    MediaType audioBasic03 = new MediaType("audio", "basic", 0.3);

    assertThat(audioBasic.isLessSpecific(audio)).isFalse();
    assertThat(audio.isLessSpecific(audioBasic)).isTrue();

    assertThat(audio.isLessSpecific(audioBasic07)).isFalse();
    assertThat(audioBasic07.isLessSpecific(audio)).isTrue();

    assertThat(audioBasic07.isLessSpecific(audioBasic03)).isFalse();
    assertThat(audioBasic03.isLessSpecific(audioBasic07)).isTrue();

    assertThat(audioBasic.isLessSpecific(MediaType.TEXT_HTML)).isFalse();
  }

  // ------------------------------------------------

  @Test
  public void of() {
    assertThat(MediaType.fromFileName("file.xml")).isEqualTo(MediaType.APPLICATION_XML);
    assertThat(MediaType.fromFileName("file.js")).isEqualTo(MediaType.parseMediaType("application/javascript"));
    assertThat(MediaType.fromFileName("file.css")).isEqualTo(MediaType.parseMediaType("text/css"));
    assertThat(MediaType.fromFileName("file.foobar")).isNull();
    assertThat(MediaType.fromFileName(null)).isNull();
    assertThat(MediaType.fromResource(null)).isNull();
  }

  @Test
  public void sortBySpecificity() {
    MediaType audioBasic = new MediaType("audio", "basic");
    MediaType audio = new MediaType("audio");
    MediaType audio03 = new MediaType("audio", "*", 0.3);
    MediaType audio07 = new MediaType("audio", "*", 0.7);
    MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
    MediaType all = MediaType.ALL;

    List<MediaType> expected = new ArrayList<>();
    expected.add(audioBasicLevel);
    expected.add(audioBasic);
    expected.add(audio);
    expected.add(all);
    expected.add(audio07);
    expected.add(audio03);

    List<MediaType> result = new ArrayList<>(expected);
    Random rnd = new Random();
    // shuffle & sort 10 times
    for (int i = 0; i < 10; i++) {
      Collections.shuffle(result, rnd);
      MimeTypeUtils.sortBySpecificity(result);

      assertThat(result).containsExactlyElementsOf(expected);

    }
  }

}
