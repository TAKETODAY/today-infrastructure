/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.lang;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * A lightweight, immutable representation of a version.
 * <p>
 * A version string follows the format:
 * <pre>
 * {major}.{minor}.{patch}-{type}.{step}-{extension}
 * </pre>
 * where the {@code {type}} is one of {@link #Draft}, {@link #SNAPSHOT},
 * {@link #Alpha}, {@link #Beta} or {@link #RELEASE}. The {@code {patch}},
 * {@code {step}} and {@code {extension}} parts are optional, and the extension
 * may itself contain hyphens.
 * <p>
 * Versions are ordered by major, minor and patch numbers first, then by type
 * precedence ({@code Draft < SNAPSHOT < Alpha < Beta < RELEASE}), and finally by
 * the step number.
 * <p>
 * The static {@link #instance} is loaded from the "Implementation-Version"
 * manifest attribute of the jar file and can be obtained via {@link #get()}.
 *
 * @param major the major version
 * @param minor the minor version
 * @param patch the patch version
 * @param type the version type, one of {@link #Draft}, {@link #SNAPSHOT},
 * {@link #Alpha}, {@link #Beta} or {@link #RELEASE}
 * @param step the version step
 * @param extension the version extension, may be {@code null}
 * @param implementationVersion the "Implementation-Version" manifest attribute
 * @author TODAY 2021/10/11 23:28
 * @since 4.0
 */
public record Version(int major, int minor, int patch, String type, int step,
                      @Nullable String extension, String implementationVersion) implements Comparable<Version> {

  /**
   * The {@code Draft} version type.
   */
  public static final String Draft = "Draft";

  /**
   * The {@code Alpha} version type.
   */
  public static final String Alpha = "Alpha";

  /**
   * The {@code Beta} version type.
   */
  public static final String Beta = "Beta";

  /**
   * The final, stable {@code RELEASE} version type.
   */
  public static final String RELEASE = "RELEASE";

  /**
   * The {@code SNAPSHOT} version type.
   */
  public static final String SNAPSHOT = "SNAPSHOT";

  /**
   * The version of the running application, resolved lazily from the
   * "Implementation-Version" manifest attribute.
   */
  public static final Version instance;

  /**
   * Version type precedence, from the least to the most stable:
   * {@code Draft < SNAPSHOT < Alpha < Beta < RELEASE}.
   */
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
   * Parses a {@link Version} from a version string.
   * <p>
   * The accepted format is {@code {major}.{minor}.{patch}-{type}.{step}-{extension}}.
   * The {@code {patch}}, {@code {step}} and {@code {extension}} parts are optional,
   * and the extension may itself contain hyphens. For example:
   * <pre>
   * "4.0.0"                -&gt; type=RELEASE, step=0, extension=null
   * "4.0.0-Beta.3"         -&gt; type=Beta,    step=3, extension=null
   * "4.0.0-Alpha.3-jdk8"   -&gt; type=Alpha,   step=3, extension="jdk8"
   * "4.0.0-Alpha.3-my-jdk" -&gt; type=Alpha,   step=3, extension="my-jdk"
   * </pre>
   *
   * @param implementationVersion the version string to parse
   * @return the parsed {@link Version}
   */
  public static Version parse(String implementationVersion) {
    String type;
    String extension = null;
    int major;
    int minor;
    int patch = 0;
    int step = 0;

    String[] split = implementationVersion.split("-");

    if (split.length == 1) {
      type = RELEASE;
    }
    else {
      if (split.length >= 3) {
        // extension is optional and may itself contain hyphens
        StringBuilder extensionBuilder = new StringBuilder(split[2]);
        for (int i = 3; i < split.length; i++) {
          extensionBuilder.append('-').append(split[i]);
        }
        extension = extensionBuilder.toString();
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
      patch = Integer.parseInt(number[2]);
    }

    return new Version(major, minor, patch, type, step, extension, implementationVersion);
  }

  /**
   * Return whether this version is a final, stable release.
   *
   * @return {@code true} if this version is a release, {@code false} otherwise
   */
  public boolean isRelease() {
    return RELEASE.equals(type);
  }

  /**
   * Return whether this version is a SNAPSHOT version.
   *
   * @return {@code true} if this version is a SNAPSHOT, {@code false} otherwise
   */
  public boolean isSnapshot() {
    return SNAPSHOT.equals(type);
  }

  /**
   * Return whether this version is an Alpha version.
   *
   * @return {@code true} if this version is an Alpha, {@code false} otherwise
   */
  public boolean isAlpha() {
    return Alpha.equals(type);
  }

  /**
   * Return whether this version is a Beta version.
   *
   * @return {@code true} if this version is a Beta, {@code false} otherwise
   */
  public boolean isBeta() {
    return Beta.equals(type);
  }

  /**
   * Return whether this version is a Draft version.
   *
   * @return {@code true} if this version is a Draft, {@code false} otherwise
   */
  public boolean isDraft() {
    return Draft.equals(type);
  }

  /**
   * Return whether this version is a pre-release version, i.e. any version
   * that is not a final {@link #RELEASE}.
   *
   * @return {@code true} if this version is a pre-release, {@code false} otherwise
   */
  public boolean isPreRelease() {
    return !isRelease();
  }

  /**
   * Return whether this version is newer than the given version.
   *
   * @param other the version to compare
   * @return {@code true} if this version is newer than {@code other}
   */
  public boolean isNewerThan(Version other) {
    return compareTo(other) > 0;
  }

  /**
   * Return whether this version is older than the given version.
   *
   * @param other the version to compare
   * @return {@code true} if this version is older than {@code other}
   */
  public boolean isOlderThan(Version other) {
    return compareTo(other) < 0;
  }

  /**
   * Return whether this version is equal to or newer than the given version.
   *
   * @param other the version to compare
   * @return {@code true} if this version is equal to or newer than {@code other}
   */
  public boolean isEqualOrNewerThan(Version other) {
    return compareTo(other) >= 0;
  }

  /**
   * Return whether this version is equal to or older than the given version.
   *
   * @param other the version to compare
   * @return {@code true} if this version is equal to or older than {@code other}
   */
  public boolean isEqualOrOlderThan(Version other) {
    return compareTo(other) <= 0;
  }

  /**
   * Return whether this version has exactly the given major, minor and patch numbers.
   * The type, step and extension are not considered.
   *
   * @param major the major version to match
   * @param minor the minor version to match
   * @param patch the patch version to match
   * @return {@code true} if the numeric version matches, {@code false} otherwise
   */
  public boolean matches(int major, int minor, int patch) {
    return this.major == major && this.minor == minor && this.patch == patch;
  }

  /**
   * Return the version string without the leading {@code 'v'} prefix.
   *
   * @return the raw implementation version string
   */
  public String toVersionString() {
    return implementationVersion;
  }

  /**
   * Return a copy of this version with its extension removed.
   *
   * @return the version without extension, or this version if it has none
   */
  public Version withoutExtension() {
    if (extension == null) {
      return this;
    }
    String base = implementationVersion.substring(0, implementationVersion.length() - extension.length() - 1);
    return new Version(major, minor, patch, type, step, null, base);
  }

  /**
   * Return a copy of this version with the given extension.
   * <p>
   * Passing {@code null} removes the extension (equivalent to
   * {@link #withoutExtension()}), while passing the current extension returns
   * {@code this} without creating a new instance.
   *
   * @param extension the new extension, may be {@code null} to remove it
   * @return a copy with the given extension, or this version if unchanged
   */
  public Version withExtension(@Nullable String extension) {
    if (Objects.equals(this.extension, extension)) {
      return this;
    }
    if (extension == null) {
      return withoutExtension();
    }
    String base = withoutExtension().implementationVersion();
    return new Version(major, minor, patch, type, step, extension, base + "-" + extension);
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

    // Compare patch version
    result = Integer.compare(patch, o.patch);
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
   * Return the version of the running application.
   *
   * @return the version instance resolved from the "Implementation-Version"
   * manifest attribute
   * @see Package#getImplementationVersion()
   */
  public static Version get() {
    return instance;
  }
}
