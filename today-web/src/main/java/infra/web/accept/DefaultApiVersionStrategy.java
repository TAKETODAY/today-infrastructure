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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.web.RequestContext;

/**
 * Default implementation of {@link ApiVersionStrategy} that delegates to the
 * configured version resolvers and version parser.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class DefaultApiVersionStrategy implements ApiVersionStrategy {

  private final List<ApiVersionResolver> versionResolvers;

  private final ApiVersionParser<?> versionParser;

  private final boolean versionRequired;

  @Nullable
  private final Comparable<?> defaultVersion;

  private final Set<Comparable<?>> supportedVersions = new TreeSet<>();

  /**
   * Create an instance.
   *
   * @param versionResolvers one or more resolvers to try; the first non-null
   * value returned by any resolver becomes the resolved used
   * @param versionParser parser for to raw version values
   * @param versionRequired whether a version is required; if a request
   * does not have a version, and a {@code defaultVersion} is not specified,
   * validation fails with {@link MissingApiVersionException}
   * @param defaultVersion a default version to assign to requests that
   * don't specify one
   */
  public DefaultApiVersionStrategy(List<ApiVersionResolver> versionResolvers,
          ApiVersionParser<?> versionParser, boolean versionRequired, @Nullable String defaultVersion) {

    Assert.notEmpty(versionResolvers, "At least one ApiVersionResolver is required");
    Assert.notNull(versionParser, "ApiVersionParser is required");

    this.versionResolvers = new ArrayList<>(versionResolvers);
    this.versionParser = versionParser;
    this.versionRequired = (versionRequired && defaultVersion == null);
    this.defaultVersion = (defaultVersion != null ? versionParser.parseVersion(defaultVersion) : null);
  }

  @Nullable
  @Override
  public Comparable<?> getDefaultVersion() {
    return this.defaultVersion;
  }

  /**
   * Add to the list of known, supported versions to check against in
   * {@link ApiVersionStrategy#validateVersion}. Request versions that are not
   * in the supported result in {@link InvalidApiVersionException}
   * in {@link ApiVersionStrategy#validateVersion}.
   *
   * @param versions the versions to add
   */
  public void addSupportedVersion(String... versions) {
    for (String version : versions) {
      this.supportedVersions.add(parseVersion(version));
    }
  }

  @Nullable
  @Override
  public String resolveVersion(RequestContext request) {
    for (ApiVersionResolver resolver : this.versionResolvers) {
      String version = resolver.resolveVersion(request);
      if (version != null) {
        return version;
      }
    }
    return null;
  }

  @Override
  public Comparable<?> parseVersion(String version) {
    return this.versionParser.parseVersion(version);
  }

  @Override
  public void validateVersion(@Nullable Comparable<?> requestVersion, RequestContext request)
          throws MissingApiVersionException, InvalidApiVersionException {

    if (requestVersion == null) {
      if (this.versionRequired) {
        throw new MissingApiVersionException();
      }
      return;
    }

    if (!this.supportedVersions.contains(requestVersion)) {
      throw new InvalidApiVersionException(requestVersion.toString());
    }
  }

  @Override
  public String toString() {
    return "DefaultApiVersionStrategy[supportedVersions=%s, versionRequired=%s, defaultVersion=%s]"
            .formatted(this.supportedVersions, this.versionRequired, this.defaultVersion);
  }

}
