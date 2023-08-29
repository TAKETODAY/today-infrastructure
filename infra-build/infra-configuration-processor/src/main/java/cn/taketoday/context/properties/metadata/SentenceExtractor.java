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

import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Utility to extract the first sentence of a text.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class SentenceExtractor {

  String getFirstSentence(String text) {
    if (text == null) {
      return null;
    }
    int dot = text.indexOf('.');
    if (dot != -1) {
      BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.US);
      breakIterator.setText(text);
      String sentence = text.substring(breakIterator.first(), breakIterator.next());
      return removeSpaceBetweenLine(sentence.trim());
    }
    else {
      String[] lines = text.split(System.lineSeparator());
      return lines[0].trim();
    }
  }

  private String removeSpaceBetweenLine(String text) {
    String[] lines = text.split(System.lineSeparator());
    return Arrays.stream(lines).map(String::trim).collect(Collectors.joining(" "));
  }

}
