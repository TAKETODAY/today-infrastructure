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

import cn.taketoday.lang.Assert;

/**
 * A Docker image name of the form {@literal "docker.io/library/ubuntu"}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @see ImageReference
 * @see #of(String)
 * @since 4.0
 */
public class ImageName {

  private static final String DEFAULT_DOMAIN = "docker.io";

  private static final String OFFICIAL_REPOSITORY_NAME = "library";

  private static final String LEGACY_DOMAIN = "index.docker.io";

  private final String domain;

  private final String name;

  private final String string;

  ImageName(String domain, String path) {
    Assert.hasText(path, "Path must not be empty");
    this.domain = getDomainOrDefault(domain);
    this.name = getNameWithDefaultPath(this.domain, path);
    this.string = this.domain + "/" + this.name;
  }

  /**
   * Return the domain for this image name.
   *
   * @return the domain
   */
  public String getDomain() {
    return this.domain;
  }

  /**
   * Return the name of this image.
   *
   * @return the image name
   */
  public String getName() {
    return this.name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ImageName other = (ImageName) obj;
    boolean result = true;
    result = result && this.domain.equals(other.domain);
    result = result && this.name.equals(other.name);
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + this.domain.hashCode();
    result = prime * result + this.name.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return this.string;
  }

  public String toLegacyString() {
    if (DEFAULT_DOMAIN.equals(this.domain)) {
      return LEGACY_DOMAIN + "/" + this.name;
    }
    return this.string;
  }

  private String getDomainOrDefault(String domain) {
    if (domain == null || LEGACY_DOMAIN.equals(domain)) {
      return DEFAULT_DOMAIN;
    }
    return domain;
  }

  private String getNameWithDefaultPath(String domain, String name) {
    if (DEFAULT_DOMAIN.equals(domain) && !name.contains("/")) {
      return OFFICIAL_REPOSITORY_NAME + "/" + name;
    }
    return name;
  }

  /**
   * Create a new {@link ImageName} from the given value. The following value forms can
   * be used:
   * <ul>
   * <li>{@code name} (maps to {@code docker.io/library/name})</li>
   * <li>{@code domain/name}</li>
   * <li>{@code domain:port/name}</li>
   * </ul>
   *
   * @param value the value to parse
   * @return an {@link ImageName} instance
   */
  public static ImageName of(String value) {
    Assert.hasText(value, "Value must not be empty");
    String domain = parseDomain(value);
    String path = (domain != null) ? value.substring(domain.length() + 1) : value;
    Assert.isTrue(Regex.PATH.matcher(path).matches(),
            () -> "Unable to parse name \"" + value + "\". "
                    + "Image name must be in the form '[domainHost:port/][path/]name', "
                    + "with 'path' and 'name' containing only [a-z0-9][.][_][-]");
    return new ImageName(domain, path);
  }

  static String parseDomain(String value) {
    int firstSlash = value.indexOf('/');
    String candidate = (firstSlash != -1) ? value.substring(0, firstSlash) : null;
    if (candidate != null && Regex.DOMAIN.matcher(candidate).matches()) {
      return candidate;
    }
    return null;
  }

}
