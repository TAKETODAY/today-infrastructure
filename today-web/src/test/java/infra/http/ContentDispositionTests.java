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

package infra.http;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

import static infra.http.ContentDisposition.parse;
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
class ContentDispositionTests {

  @Test
  void parseFilenameQuoted() {
    assertThat(parse("form-data; name=\"foo\"; filename=\"foo.txt\""))
            .isEqualTo(ContentDisposition.formData()
                    .name("foo")
                    .filename("foo.txt")
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
  void parseEncodedFilenameWithPaddedCharset() {
    assertThat(parse("attachment; filename*= UTF-8''some-file.zip"))
            .isEqualTo(ContentDisposition.attachment()
                    .filename("some-file.zip", StandardCharsets.UTF_8)
                    .build());
  }

  @Test
  void parseBase64EncodedFilename() {
    String input = "attachment; filename=\"=?UTF-8?B?5pel5pys6KqeLmNzdg==?=\"";
    assertThat(parse(input).getFilename()).isEqualTo("日本語.csv");
  }

  @Test
  void parseBase64EncodedFilenameMultipleSegments() {
    String input =
            "attachment; filename=\"=?utf-8?B?U3ByaW5n5qGG5p625Li65Z+65LqOSmF2YeeahOeOsOS7o+S8geS4muW6lA==?= " +
                    "=?utf-8?B?55So56iL5bqP5o+Q5L6b5LqG5YWo6Z2i55qE57yW56iL5ZKM6YWN572u5qih?= " +
                    "=?utf-8?B?5Z6LLnR4dA==?=\"";
    assertThat(parse(input).getFilename()).isEqualTo("Spring框架为基于Java的现代企业应用程序提供了全面的编程和配置模型.txt");
  }

  @Test
  void parseBase64EncodedShiftJISFilename() {
    String input = "attachment; filename=\"=?SHIFT_JIS?B?k/qWe4zqLmNzdg==?=\"";
    assertThat(parse(input).getFilename()).isEqualTo("日本語.csv");
  }

  @Test
  void parseQuotedPrintableFilename() {
    String input = "attachment; filename=\"=?UTF-8?Q?=E6=97=A5=E6=9C=AC=E8=AA=9E.csv?=\"";
    assertThat(parse(input).getFilename()).isEqualTo("日本語.csv");
  }

  @Test
  void parseQuotedPrintableFilenameMultipleSegments() {
    String input =
            "attachment; filename=\"=?utf-8?Q?Infra=E6=A1=86=E6=9E=B6=E4=B8=BA=E5=9F=BA=E4=BA=8E?=" +
                    "=?utf-8?Q?Java=E7=9A=84=E7=8E=B0=E4=BB=A3=E4=BC=81=E4=B8=9A=E5=BA=94?=" +
                    "=?utf-8?Q?=E7=94=A8=E7=A8=8B=E5=BA=8F=E6=8F=90=E4=BE=9B=E4=BA=86=E5=85=A8?=" +
                    "=?utf-8?Q?=E9=9D=A2=E7=9A=84=E7=BC=96=E7=A8=8B=E5=92=8C=E9=85=8D=E7=BD=AE?=" +
                    "=?utf-8?Q?=E6=A8=A1=E5=9E=8B.txt?=\"";
    assertThat(parse(input).getFilename()).isEqualTo("Infra框架为基于Java的现代企业应用程序提供了全面的编程和配置模型.txt");

  }

  @Test
  void parseQuotedPrintableShiftJISFilename() {
    String input = "attachment; filename=\"=?SHIFT_JIS?Q?=93=FA=96{=8C=EA.csv?=\"";
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

  @Test
  void parseBackslash() {
    String s = "form-data; name=\"foo\"; filename=\"foo\\\\bar \\\"baz\\\" qux \\\\\\\" quux.txt\"";
    ContentDisposition cd = parse(
            s);
    assertThat(cd.getName()).isEqualTo("foo");
    assertThat(cd.getFilename()).isEqualTo("foo\\bar \"baz\" qux \\\" quux.txt");
    assertThat(cd.toString()).isEqualTo(s);
  }

  @Test
  void parseBackslashInLastPosition() {
    ContentDisposition cd = parse("form-data; name=\"foo\"; filename=\"bar\\\"");
    assertThat(cd.getName()).isEqualTo("foo");
    assertThat(cd.getFilename()).isEqualTo("bar\\");
    assertThat(cd.toString()).isEqualTo("form-data; name=\"foo\"; filename=\"bar\\\\\"");
  }

  @Test
  void parseWindowsPath() {
    ContentDisposition cd = parse("form-data; name=\"foo\"; filename=\"D:\\foo\\bar.txt\"");
    assertThat(cd.getName()).isEqualTo("foo");
    assertThat(cd.getFilename()).isEqualTo("D:\\foo\\bar.txt");
    assertThat(cd.toString()).isEqualTo("form-data; name=\"foo\"; filename=\"D:\\\\foo\\\\bar.txt\"");
  }

  @Test
  void parseWithExtraSemicolons() {
    assertThat(parse("form-data; name=\"foo\";; ; filename=\"foo.txt\";"))
            .isEqualTo(ContentDisposition.formData()
                    .name("foo")
                    .filename("foo.txt")
                    .build());
  }

  @Test
  void parseAttributesCaseInsensitively() {
    ContentDisposition cd = parse("form-data; Name=\"foo\"; FileName=\"bar.txt\"");
    assertThat(cd.getName()).isEqualTo("foo");
    assertThat(cd.getFilename()).isEqualTo("bar.txt");
    assertThat(cd.toString()).isEqualTo("form-data; name=\"foo\"; filename=\"bar.txt\"");
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

  @Test
  void format() {
    assertThat(
            ContentDisposition.formData()
                    .name("foo")
                    .filename("foo.txt")
                    .build().toString())
            .isEqualTo("form-data; name=\"foo\"; filename=\"foo.txt\"");
  }

  @Test
  void formatWithEncodedFilename() {
    assertThat(
            ContentDisposition.formData()
                    .name("name")
                    .filename("中文.txt", StandardCharsets.UTF_8)
                    .build().toString())
            .isEqualTo("form-data; name=\"name\"; " +
                    "filename=\"=?UTF-8?Q?=E4=B8=AD=E6=96=87.txt?=\"; " +
                    "filename*=UTF-8''%E4%B8%AD%E6%96%87.txt");
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
  void formatWithFilenameWithQuotes() {
    BiConsumer<String, String> tester = (input, output) -> {
      assertThat(ContentDisposition.formData().filename(input).build().toString())
              .isEqualTo("form-data; filename=\"" + output + "\"");
      assertThat(ContentDisposition.formData().filename(input, StandardCharsets.US_ASCII).build().toString())
              .isEqualTo("form-data; filename=\"" + output + "\"");
    };

    String filename = "\"foo.txt";
    tester.accept(filename, "\\\"foo.txt");

    filename = "\\\"foo.txt";
    tester.accept(filename, "\\\\\\\"foo.txt");

    filename = "\\\\\"foo.txt";
    tester.accept(filename, "\\\\\\\\\\\"foo.txt");

    filename = "\\\\\\\"foo.txt";
    tester.accept(filename, "\\\\\\\\\\\\\\\"foo.txt");

    filename = "\\\\\\\\\"foo.txt";
    tester.accept(filename, "\\\\\\\\\\\\\\\\\\\"foo.txt");

    tester.accept("\"\"foo.txt", "\\\"\\\"foo.txt");
    tester.accept("\"\"\"foo.txt", "\\\"\\\"\\\"foo.txt");

    tester.accept("foo.txt\\", "foo.txt\\\\");
    tester.accept("foo.txt\\\\", "foo.txt\\\\\\\\");
    tester.accept("foo.txt\\\\\\", "foo.txt\\\\\\\\\\\\");
  }

  @Test
  void formatWithUtf8FilenameWithQuotes() {
    String filename = "\"中文.txt";
    assertThat(ContentDisposition.formData().filename(filename, StandardCharsets.UTF_8).build().toString())
            .isEqualTo("form-data; filename=\"=?UTF-8?Q?=22=E4=B8=AD=E6=96=87.txt?=\"; filename*=UTF-8''%22%E4%B8%AD%E6%96%87.txt");
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

  @Test
  void parseFormatted() {
    ContentDisposition cd = ContentDisposition.builder("form-data")
            .name("foo")
            .filename("foo\\bar \"baz\" qux \\\" quux.txt").build();
    ContentDisposition parsed = parse(cd.toString());
    assertThat(parsed).isEqualTo(cd);
    assertThat(parsed.toString()).isEqualTo(cd.toString());
  }

  @Test
  void parseFormattedWithQuestionMark() {
    String filename = "filename with ?问号.txt";
    ContentDisposition cd = ContentDisposition.attachment()
            .filename(filename, StandardCharsets.UTF_8)
            .build();
    String result = cd.toString();
    assertThat(result).isEqualTo("attachment; " +
            "filename=\"=?UTF-8?Q?filename_with_=3F=E9=97=AE=E5=8F=B7.txt?=\"; " +
            "filename*=UTF-8''filename%20with%20%3F%E9%97%AE%E5%8F%B7.txt");

    String[] parts = result.split("; ");

    String quotedPrintableFilename = parts[0] + "; " + parts[1];
    assertThat(parse(quotedPrintableFilename).getFilename())
            .isEqualTo(filename);

    String rfc5987Filename = parts[0] + "; " + parts[2];
    assertThat(parse(rfc5987Filename).getFilename())
            .isEqualTo(filename);
  }

  @Test
  void attachmentType() {
    ContentDisposition attachment = ContentDisposition.attachment().build();
    assertThat(attachment.isAttachment()).isTrue();
    assertThat(attachment.isFormData()).isFalse();
    assertThat(attachment.isInline()).isFalse();
  }

  @Test
  void formDataType() {
    ContentDisposition formData = ContentDisposition.formData().build();
    assertThat(formData.isAttachment()).isFalse();
    assertThat(formData.isFormData()).isTrue();
    assertThat(formData.isInline()).isFalse();
  }

  @Test
  void inlineType() {
    ContentDisposition inline = ContentDisposition.inline().build();
    assertThat(inline.isAttachment()).isFalse();
    assertThat(inline.isFormData()).isFalse();
    assertThat(inline.isInline()).isTrue();
  }

}
