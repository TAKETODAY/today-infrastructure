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

package infra.web.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.lang.Nullable;
import infra.web.accept.ApiVersionParser;
import infra.web.accept.ApiVersionResolver;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.accept.PathApiVersionResolver;
import infra.web.accept.SemanticApiVersionParser;

/**
 * Configure API versioning.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class ApiVersionConfigurer {

  private final List<ApiVersionResolver> versionResolvers = new ArrayList<>();

  @Nullable
  private ApiVersionParser<?> versionParser;

  private boolean versionRequired = true;

  @Nullable
  private String defaultVersion;

  private final Set<String> supportedVersions = new LinkedHashSet<>();

  /**
   * Add a resolver that extracts the API version from a request header.
   *
   * @param headerName the header name to check
   */
  public ApiVersionConfigurer useRequestHeader(String headerName) {
    this.versionResolvers.add(request -> request.requestHeaders().getFirst(headerName));
    return this;
  }

  /**
   * Add a resolver that extracts the API version from a request parameter.
   *
   * @param paramName the parameter name to check
   */
  public ApiVersionConfigurer useRequestParam(String paramName) {
    this.versionResolvers.add(request -> request.getParameter(paramName));
    return this;
  }

  /**
   * Add a resolver that extracts the API version from a path segment.
   *
   * @param index the index of the path segment to check; e.g. for URL's like
   * "/{version}/..." use index 0, for "/api/{version}/..." index 1.
   */
  public ApiVersionConfigurer usePathSegment(int index) {
    this.versionResolvers.add(new PathApiVersionResolver(index));
    return this;
  }

  /**
   * Add custom resolvers to resolve the API version.
   *
   * @param resolvers the resolvers to use
   */
  public ApiVersionConfigurer useVersionResolver(ApiVersionResolver... resolvers) {
    this.versionResolvers.addAll(Arrays.asList(resolvers));
    return this;
  }

  /**
   * Configure a parser to parse API versions with.
   * <p>By default, {@link SemanticApiVersionParser} is used.
   *
   * @param versionParser the parser to user
   */
  public ApiVersionConfigurer setVersionParser(@Nullable ApiVersionParser<?> versionParser) {
    this.versionParser = versionParser;
    return this;
  }

  /**
   * Whether requests are required to have an API version. When set to
   * {@code true}, {@link infra.web.accept.MissingApiVersionException}
   * is raised, resulting in a 400 response if the request doesn't have an API
   * version. When set to false, a request without a version is considered to
   * accept any version.
   * <p>By default, this is set to {@code true} when API versioning is enabled
   * by adding at least one {@link ApiVersionResolver}). When a
   * {@link #setDefaultVersion defaultVersion} is also set, this is
   * automatically set to {@code false}.
   *
   * @param required whether an API version is required.
   */
  public ApiVersionConfigurer setVersionRequired(boolean required) {
    this.versionRequired = required;
    return this;
  }

  /**
   * Configure a default version to assign to requests that don't specify one.
   *
   * @param defaultVersion the default version to use
   */
  public ApiVersionConfigurer setDefaultVersion(@Nullable String defaultVersion) {
    this.defaultVersion = defaultVersion;
    return this;
  }

  /**
   * Add to the list of supported versions to validate request versions against.
   * Request versions that are not supported result in
   * {@link infra.web.accept.InvalidApiVersionException}.
   * <p>Note that the set of supported versions is populated from versions
   * listed in controller mappings. Therefore, typically you do not have to
   * manage this list except for the initial API version, when controller
   * don't have to have a version to start.
   *
   * @param versions supported versions to add
   */
  public ApiVersionConfigurer addSupportedVersions(String... versions) {
    Collections.addAll(this.supportedVersions, versions);
    return this;
  }

  @Nullable
  public ApiVersionStrategy getApiVersionStrategy() {
    if (this.versionResolvers.isEmpty()) {
      return null;
    }

    DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(this.versionResolvers,
            (this.versionParser != null ? this.versionParser : new SemanticApiVersionParser()),
            this.versionRequired, this.defaultVersion);

    this.supportedVersions.forEach(strategy::addSupportedVersion);

    return strategy;
  }

}
