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

package cn.taketoday.buildpack.platform.docker.type;

import java.util.regex.Pattern;

/**
 * Regular Expressions for image names and references based on those found in the Docker
 * codebase.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @see <a href=
 * "https://github.com/docker/distribution/blob/master/reference/reference.go">Docker
 * grammar reference</a>
 * @see <a href=
 * "https://github.com/docker/distribution/blob/master/reference/regexp.go">Docker grammar
 * implementation</a>
 * @see <a href=
 * "https://stackoverflow.com/questions/37861791/how-are-docker-image-names-parsed">How
 * are Docker image names parsed?</a>
 */
final class Regex implements CharSequence {

  static final Pattern DOMAIN;

  static {
    Regex component = Regex.oneOf("[a-zA-Z0-9]", "[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]");
    Regex dotComponent = Regex.group("[.]", component);
    Regex colonPort = Regex.of("[:][0-9]+");
    Regex dottedDomain = Regex.group(component, dotComponent.oneOrMoreTimes());
    Regex dottedDomainAndPort = Regex.group(component, dotComponent.oneOrMoreTimes(), colonPort);
    Regex nameAndPort = Regex.group(component, colonPort);
    DOMAIN = Regex.oneOf(dottedDomain, nameAndPort, dottedDomainAndPort, "localhost").compile();
  }

  private static final Regex PATH_COMPONENT;

  static {
    Regex segment = Regex.of("[a-z0-9]+");
    Regex separator = Regex.group("[._]|__|[-]*");
    Regex separatedSegment = Regex.group(separator, segment).oneOrMoreTimes();
    PATH_COMPONENT = Regex.of(segment, Regex.group(separatedSegment).zeroOrOnce());
  }

  static final Pattern PATH;

  static {
    Regex component = PATH_COMPONENT;
    Regex slashComponent = Regex.group("[/]", component);
    Regex slashComponents = Regex.group(slashComponent.oneOrMoreTimes());
    PATH = Regex.of(component, slashComponents.zeroOrOnce()).compile();
  }

  static final Pattern TAG = Regex.of("^[\\w][\\w.-]{0,127}").compile();

  static final Pattern DIGEST = Regex.of("^[A-Za-z][A-Za-z0-9]*(?:[-_+.][A-Za-z][A-Za-z0-9]*)*[:][[A-Fa-f0-9]]{32,}")
          .compile();

  private final String value;

  private Regex(CharSequence value) {
    this.value = value.toString();
  }

  private Regex oneOrMoreTimes() {
    return new Regex(this.value + "+");
  }

  private Regex zeroOrOnce() {
    return new Regex(this.value + "?");
  }

  Pattern compile() {
    return Pattern.compile("^" + this.value + "$");
  }

  @Override
  public int length() {
    return this.value.length();
  }

  @Override
  public char charAt(int index) {
    return this.value.charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return this.value.subSequence(start, end);
  }

  @Override
  public String toString() {
    return this.value;
  }

  private static Regex of(CharSequence... expressions) {
    return new Regex(String.join("", expressions));
  }

  private static Regex oneOf(CharSequence... expressions) {
    return new Regex("(?:" + String.join("|", expressions) + ")");
  }

  private static Regex group(CharSequence... expressions) {
    return new Regex("(?:" + String.join("", expressions) + ")");
  }

}
