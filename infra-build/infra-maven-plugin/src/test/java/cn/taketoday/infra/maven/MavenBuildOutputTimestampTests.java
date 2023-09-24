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

package cn.taketoday.infra.maven;

import org.junit.jupiter.api.Test;

import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/24 21:48
 */
class MavenBuildOutputTimestampTests {

  @Test
  void shouldParseNull() {
    assertThat(parse(null)).isNull();
  }

  @Test
  void shouldParseSingleDigit() {
    assertThat(parse("0")).isEqualTo(Instant.parse("1970-01-01T00:00:00Z"));
  }

  @Test
  void shouldNotParseSingleCharacter() {
    assertThat(parse("a")).isNull();
  }

  @Test
  void shouldParseIso8601() {
    assertThat(parse("2011-12-03T10:15:30Z")).isEqualTo(Instant.parse("2011-12-03T10:15:30Z"));
  }

  @Test
  void shouldParseIso8601WithMilliseconds() {
    assertThat(parse("2011-12-03T10:15:30.12345Z")).isEqualTo(Instant.parse("2011-12-03T10:15:30Z"));
  }

  @Test
  void shouldFailIfIso8601BeforeMin() {
    assertThatIllegalArgumentException().isThrownBy(() -> parse("1970-01-01T00:00:00Z"))
            .withMessage(
                    "'1970-01-01T00:00:00Z' is not within the valid range 1980-01-01T00:00:02Z to 2099-12-31T23:59:59Z");
  }

  @Test
  void shouldFailIfIso8601AfterMax() {
    assertThatIllegalArgumentException().isThrownBy(() -> parse("2100-01-01T00:00:00Z"))
            .withMessage(
                    "'2100-01-01T00:00:00Z' is not within the valid range 1980-01-01T00:00:02Z to 2099-12-31T23:59:59Z");
  }

  @Test
  void shouldFailIfNotIso8601() {
    assertThatIllegalArgumentException().isThrownBy(() -> parse("dummy"))
            .withMessage("Can't parse 'dummy' to instant");
  }

  @Test
  void shouldParseIso8601WithOffset() {
    assertThat(parse("2019-10-05T20:37:42+06:00")).isEqualTo(Instant.parse("2019-10-05T14:37:42Z"));
  }

  @Test
  void shouldParseToFileTime() {
    assertThat(parseFileTime(null)).isEqualTo(null);
    assertThat(parseFileTime("0")).isEqualTo(FileTime.fromMillis(0));
    assertThat(parseFileTime("2019-10-05T14:37:42Z")).isEqualTo(FileTime.fromMillis(1570286262000L));
  }

  private static Instant parse(String timestamp) {
    return new MavenBuildOutputTimestamp(timestamp).toInstant();
  }

  private static FileTime parseFileTime(String timestamp) {
    return new MavenBuildOutputTimestamp(timestamp).toFileTime();
  }

}