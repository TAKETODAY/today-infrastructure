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

package infra.lang;

import java.util.List;
import java.util.Objects;

/**
 * Class that exposes the version. Fetches the
 * "Implementation-Version" manifest attribute from the jar file.
 *
 * @param major major version
 * @param micro micro version
 * @param minor minor version
 * @param step step
 * @param type type
 * @param extension version extension
 * @param implementationVersion "Implementation-Version" manifest attribute
 * @author TODAY 2021/10/11 23:28
 * @since 4.0
 */
public record Version(int major, int minor, int micro, String type, int step,
        @Nullable String extension, String implementationVersion) implements Comparable<Version> {

  public static final String Draft = "Draft";
  public static final String Alpha = "Alpha";
  public static final String Beta = "Beta";
  public static final String RELEASE = "RELEASE";
  public static final String SNAPSHOT = "SNAPSHOT";

  public static final Version instance;

  // Define version type precedence: Draft < SNAPSHOT < Alpha < Beta < RELEASE

  private static final List<String> TYPE_PRECEDENCE = List.of(Draft, SNAPSHOT, Alpha, Beta, RELEASE);

  static {
    String implementationVersion = VersionExtractor.forClass(Version.class);
    if (implementationVersion != null) {
      instance = parse(implementationVersion);
    }
    else {
      instance = new Version(0, 0, 0, RELEASE, 0, null, "Unknown");
      System.err.println("infra.lang.Version cannot get 'implementationVersion' in manifest.");
    }
  }

  /**
   * parse {@link Version},
   * version format: {major}.{minor}.{micro}-{type}.{step}-{extension}
   *
   * @param implementationVersion 'implementationVersion' in manifest
   */
  static Version parse(String implementationVersion) {
    String type;
    String extension = null;
    int major;
    int minor;
    int micro = 0;
    int step = 0;

    String[] split = implementationVersion.split("-");

    if (split.length == 1) {
      type = RELEASE;
    }
    else {
      if (split.length == 3) {
        extension = split[2]; // optional
      }

      type = split[1];
      String[] typeSplit = type.split("\\.");
      if (typeSplit.length == 2) {
        type = typeSplit[0];
        step = Integer.parseInt(typeSplit[1]);
      }
    }

    String[] number = split[0].split("\\.");
    major = Integer.parseInt(number[0]);
    minor = Integer.parseInt(number[1]);
    if (number.length == 3) {
      micro = Integer.parseInt(number[2]);
    }

    return new Version(major, minor, micro, type, step, extension, implementationVersion);
  }

  @Override
  public int compareTo(Version o) {
    if (this == o) {
      return 0;
    }

    // Compare major version
    int result = Integer.compare(major, o.major);
    if (result != 0) {
      return result;
    }

    // Compare minor version
    result = Integer.compare(minor, o.minor);
    if (result != 0) {
      return result;
    }

    // Compare micro version
    result = Integer.compare(micro, o.micro);
    if (result != 0) {
      return result;
    }

    // Compare type
    result = compareType(type, o.type);
    if (result != 0) {
      return result;
    }

    // Compare step
    return Integer.compare(step, o.step);
  }

  private static int compareType(String type1, String type2) {
    if (type1.equals(type2)) {
      return 0;
    }
    int index1 = TYPE_PRECEDENCE.indexOf(type1);
    int index2 = TYPE_PRECEDENCE.indexOf(type2);
    if (index1 >= 0 && index2 >= 0) {
      return Integer.compare(index1, index2);
    }
    return type1.compareTo(type2);
  }

  @Override
  public String toString() {
    return "v" + implementationVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Version version))
      return false;
    return Objects.equals(implementationVersion, version.implementationVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(implementationVersion);
  }

  /**
   * @see Package#getImplementationVersion()
   */
  public static Version get() {
    return instance;
  }
}
