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

package infra.web.accept;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import infra.lang.Assert;

/**
 * Parser for semantic API versioning with  a major, minor, and patch values.
 * For example "1", "1.0", "1.2", "1.2.0", "1.2.3". Leading, non-integer
 * characters, as in "v1.0", are skipped.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class SemanticApiVersionParser implements ApiVersionParser<SemanticApiVersionParser.Version> {

  private static final Pattern semantinVersionPattern = Pattern.compile("^(\\d+)(\\.(\\d+))?(\\.(\\d+))?$");

  @Override
  public Version parseVersion(String version) {
    Assert.notNull(version, "'version' is required");

    version = skipNonDigits(version);

    Matcher matcher = semantinVersionPattern.matcher(version);
    Assert.state(matcher.matches(), "Invalid API version format");

    String major = matcher.group(1);
    String minor = matcher.group(3);
    String patch = matcher.group(5);

    return new Version(Integer.parseInt(major),
            (minor != null ? Integer.parseInt(minor) : 0),
            (patch != null ? Integer.parseInt(patch) : 0));
  }

  private static String skipNonDigits(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (Character.isDigit(value.charAt(i))) {
        return value.substring(i);
      }
    }
    return "";
  }

  /**
   * Representation of a semantic version.
   */
  public static final class Version implements Comparable<Version> {

    public final int major;

    public final int minor;

    public final int patch;

    Version(int major, int minor, int patch) {
      this.major = major;
      this.minor = minor;
      this.patch = patch;
    }

    @Override
    public int compareTo(Version other) {
      int result = Integer.compare(this.major, other.major);
      if (result != 0) {
        return result;
      }
      result = Integer.compare(this.minor, other.minor);
      if (result != 0) {
        return result;
      }
      return Integer.compare(this.patch, other.patch);
    }

    @Override
    public boolean equals(Object other) {
      return (this == other || (other instanceof Version ov &&
              this.major == ov.major &&
              this.minor == ov.minor &&
              this.patch == ov.patch));
    }

    @Override
    public int hashCode() {
      int result = this.major;
      result = 31 * result + this.minor;
      result = 31 * result + this.patch;
      return result;
    }

    @Override
    public String toString() {
      return this.major + "." + this.minor + "." + this.patch;
    }
  }

}
