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

package cn.taketoday.http;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ContentDisposition}
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author TODAY 2021/4/15 14:16
 * @since 3.0
 */
public class ContentDispositionTests {

  private static DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;

  @Test
  @SuppressWarnings("deprecation")
  void parse() {
    assertThat(parse("form-data; name=\"foo\"; filename=\"foo.txt\"; size=123"))
            .isEqualTo(ContentDisposition.formData()
                    .name("foo")
                    .filename("foo.txt")
                    .size(123L)
                    .build());
  }

  @Test
  void parseFilenameUnquoted() {
    assertThat(parse("form-data; filename=unquoted"))
            .isEqualTo(ContentDisposition.formData()
                    .filename("unquoted")
                    .build());
  }

  @Test
    // SPR-16091
  void parseFilenameWithSemicolon() {
    assertThat(parse("attachment; filename=\"filename with ; semicolon.txt\""))
            .isEqualTo(ContentDisposition.attachment()
                    .filename("filename with ; semicolon.txt")
                    .build());
  }

  @Test
  void parseEncodedFilename() {
    assertThat(parse("form-data; name=\"name\"; filename*=UTF-8''%E4%B8%AD%E6%96%87.txt"))
            .isEqualTo(ContentDisposition.formData()
                    .name("name")
                    .filename("中文.txt", StandardCharsets.UTF_8)
                    .build());
  }

  @Test
    // gh-24112
  void parseEncodedFilenameWithPaddedCharset() {
    assertThat(parse("attachment; filename*= UTF-8''some-file.zip"))
            .isEqualTo(ContentDisposition.attachment()
                    .filename("some-file.zip", StandardCharsets.UTF_8)
                    .build());
  }

  @Test
    // gh-26463
  void parseBase64EncodedFilename() {
    String input = "attachment; filename=\"=?UTF-8?B?5pel5pys6KqeLmNzdg==?=\"";
    assertThat(parse(input).getFilename()).isEqualTo("日本語.csv");
  }

  @Test
    // gh-26463
  void parseBase64EncodedShiftJISFilename() {
    String input = "attachment; filename=\"=?SHIFT_JIS?B?k/qWe4zqLmNzdg==?=\"";
    assertThat(parse(input).getFilename()).isEqualTo("日本語.csv");
  }

  @Test
  void parseEncodedFilenameWithoutCharset() {
    assertThat(parse("form-data; name=\"name\"; filename*=test.txt"))
            .isEqualTo(ContentDisposition.formData()
                    .name("name")
                    .filename("test.txt")
                    .build());
  }

  @Test
  void parseEncodedFilenameWithInvalidCharset() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> parse("form-data; name=\"name\"; filename*=UTF-16''test.txt"));
  }

  @Test
  void parseEncodedFilenameWithInvalidName() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> parse("form-data; name=\"name\"; filename*=UTF-8''%A"));

    assertThatIllegalArgumentException()
            .isThrownBy(() -> parse("form-data; name=\"name\"; filename*=UTF-8''%A.txt"));
  }

  @Test // gh-23077
  @SuppressWarnings("deprecation")
  void parseWithEscapedQuote() {
    BiConsumer<String, String> tester = (description, filename) ->
            assertThat(parse("form-data; name=\"file\"; filename=\"" + filename + "\"; size=123"))
                    .as(description)
                    .isEqualTo(ContentDisposition.formData().name("file").filename(filename).size(123L).build());

    tester.accept("Escaped quotes should be ignored",
            "\\\"The Twilight Zone\\\".txt");

    tester.accept("Escaped quotes preceded by escaped backslashes should be ignored",
            "\\\\\\\"The Twilight Zone\\\\\\\".txt");

    tester.accept("Escaped backslashes should not suppress quote",
            "The Twilight Zone \\\\");

    tester.accept("Escaped backslashes should not suppress quote",
            "The Twilight Zone \\\\\\\\");
  }

  @Test
  @SuppressWarnings("deprecation")
  void parseWithExtraSemicolons() {
    assertThat(parse("form-data; name=\"foo\";; ; filename=\"foo.txt\"; size=123"))
            .isEqualTo(ContentDisposition.formData()
                    .name("foo")
                    .filename("foo.txt")
                    .size(123L)
                    .build());
  }

  @Test
  @SuppressWarnings("deprecation")
  void parseDates() {
    ZonedDateTime creationTime = ZonedDateTime.parse("Mon, 12 Feb 2007 10:15:30 -0500", formatter);
    ZonedDateTime modificationTime = ZonedDateTime.parse("Tue, 13 Feb 2007 10:15:30 -0500", formatter);
    ZonedDateTime readTime = ZonedDateTime.parse("Wed, 14 Feb 2007 10:15:30 -0500", formatter);

    assertThat(
            parse("attachment; " +
                    "creation-date=\"" + creationTime.format(formatter) + "\"; " +
                    "modification-date=\"" + modificationTime.format(formatter) + "\"; " +
                    "read-date=\"" + readTime.format(formatter) + "\"")).isEqualTo(
            ContentDisposition.attachment()
                    .creationDate(creationTime)
                    .modificationDate(modificationTime)
                    .readDate(readTime)
                    .build());
  }

  @Test
  @SuppressWarnings("deprecation")
  void parseIgnoresInvalidDates() {
    ZonedDateTime readTime = ZonedDateTime.parse("Wed, 14 Feb 2007 10:15:30 -0500", formatter);

    assertThat(
            parse("attachment; " +
                    "creation-date=\"-1\"; " +
                    "modification-date=\"-1\"; " +
                    "read-date=\"" + readTime.format(formatter) + "\"")).isEqualTo(
            ContentDisposition.attachment()
                    .readDate(readTime)
                    .build());
  }

  @Test
  void parseEmpty() {
    assertThatIllegalArgumentException().isThrownBy(() -> parse(""));
  }

  @Test
  void parseNoType() {
    assertThatIllegalArgumentException().isThrownBy(() -> parse(";"));
  }

  @Test
  void parseInvalidParameter() {
    assertThatIllegalArgumentException().isThrownBy(() -> parse("foo;bar"));
  }

  private static ContentDisposition parse(String input) {
    return ContentDisposition.parse(input);
  }

  @Test
  @SuppressWarnings("deprecation")
  void format() {
    assertThat(
            ContentDisposition.formData()
                    .name("foo")
                    .filename("foo.txt")
                    .size(123L)
                    .build().toString())
            .isEqualTo("form-data; name=\"foo\"; filename=\"foo.txt\"; size=123");
  }

  @Test
  void formatWithEncodedFilename() {
    assertThat(
            ContentDisposition.formData()
                    .name("name")
                    .filename("中文.txt", StandardCharsets.UTF_8)
                    .build().toString())
            .isEqualTo("form-data; name=\"name\"; filename*=UTF-8''%E4%B8%AD%E6%96%87.txt");
  }

  @Test
  void formatWithEncodedFilenameUsingUsAscii() {
    assertThat(
            ContentDisposition.formData()
                    .name("name")
                    .filename("test.txt", StandardCharsets.US_ASCII)
                    .build()
                    .toString())
            .isEqualTo("form-data; name=\"name\"; filename=\"test.txt\"");
  }

  @Test
    // gh-24220
  void formatWithFilenameWithQuotes() {

    BiConsumer<String, String> tester = (input, output) -> {

      assertThat(ContentDisposition.formData().filename(input).build().toString())
              .isEqualTo("form-data; filename=\"" + output + "\"");

      assertThat(ContentDisposition.formData().filename(input, StandardCharsets.US_ASCII).build().toString())
              .isEqualTo("form-data; filename=\"" + output + "\"");
    };

    String filename = "\"foo.txt";
    tester.accept(filename, "\\" + filename);

    filename = "\\\"foo.txt";
    tester.accept(filename, filename);

    filename = "\\\\\"foo.txt";
    tester.accept(filename, "\\" + filename);

    filename = "\\\\\\\"foo.txt";
    tester.accept(filename, filename);

    filename = "\\\\\\\\\"foo.txt";
    tester.accept(filename, "\\" + filename);

    tester.accept("\"\"foo.txt", "\\\"\\\"foo.txt");
    tester.accept("\"\"\"foo.txt", "\\\"\\\"\\\"foo.txt");

    tester.accept("foo.txt\\", "foo.txt");
    tester.accept("foo.txt\\\\", "foo.txt\\\\");
    tester.accept("foo.txt\\\\\\", "foo.txt\\\\");
  }

  @Test
  void formatWithEncodedFilenameUsingInvalidCharset() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            ContentDisposition.formData()
                    .name("name")
                    .filename("test.txt", StandardCharsets.UTF_16)
                    .build()
                    .toString());
  }

}
