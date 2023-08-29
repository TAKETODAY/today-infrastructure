/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SentenceExtractor}.
 *
 * @author Stephane Nicoll
 */
class SentenceExtractorTests {

  private static final String NEW_LINE = System.lineSeparator();

  private final SentenceExtractor extractor = new SentenceExtractor();

  @Test
  void extractFirstSentence() {
    String sentence = this.extractor.getFirstSentence("My short description. More stuff.");
    assertThat(sentence).isEqualTo("My short description.");
  }

  @Test
  void extractFirstSentenceNewLineBeforeDot() {
    String sentence = this.extractor
            .getFirstSentence("My short" + NEW_LINE + "description." + NEW_LINE + "More stuff.");
    assertThat(sentence).isEqualTo("My short description.");
  }

  @Test
  void extractFirstSentenceNewLineBeforeDotWithSpaces() {
    String sentence = this.extractor
            .getFirstSentence("My short  " + NEW_LINE + " description.  " + NEW_LINE + "More stuff.");
    assertThat(sentence).isEqualTo("My short description.");
  }

  @Test
  void extractFirstSentenceNoDot() {
    String sentence = this.extractor.getFirstSentence("My short description");
    assertThat(sentence).isEqualTo("My short description");
  }

  @Test
  void extractFirstSentenceNoDotMultipleLines() {
    String sentence = this.extractor.getFirstSentence("My short description " + NEW_LINE + " More stuff");
    assertThat(sentence).isEqualTo("My short description");
  }

  @Test
  void extractFirstSentenceNull() {
    assertThat(this.extractor.getFirstSentence(null)).isNull();
  }

}
