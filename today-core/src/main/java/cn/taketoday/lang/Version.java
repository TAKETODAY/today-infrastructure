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

package cn.taketoday.lang;

import java.util.Objects;

/**
 * Class that exposes the version. Fetches the
 * "Implementation-Version" manifest attribute from the jar file.
 *
 * @author TODAY 2021/10/11 23:28
 * @since 4.0
 */
public record Version(
        int major, int minor, int micro, String type, int step,
        @Nullable String extension, String implementationVersion) {
  public static final String Draft = "Draft";
  public static final String Alpha = "Alpha";
  public static final String Beta = "Beta";
  public static final String RELEASE = "RELEASE";
  public static final String SNAPSHOT = "SNAPSHOT";

  public static final Version instance;

  static {
    String implementationVersion = VersionExtractor.forClass(Version.class);
    if (implementationVersion != null) {
      instance = parse(implementationVersion);
    }
    else {
      instance = new Version(0, 0, 0, RELEASE, 0, null, "Unknown");
      System.err.println("cn.taketoday.lang.Version cannot get 'implementationVersion' in manifest.");
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
    int micro;
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

    String ver = split[0];
    String[] verSplit = ver.split("\\.");
    major = Integer.parseInt(verSplit[0]);
    minor = Integer.parseInt(verSplit[1]);
    micro = Integer.parseInt(verSplit[2]);

    return new Version(major, minor, micro, type, step, extension, implementationVersion);
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
