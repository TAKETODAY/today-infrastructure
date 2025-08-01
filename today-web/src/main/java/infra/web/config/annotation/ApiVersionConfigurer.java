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
import java.util.Set;

import infra.http.MediaType;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.web.accept.ApiVersionDeprecationHandler;
import infra.web.accept.ApiVersionParser;
import infra.web.accept.ApiVersionResolver;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.accept.InvalidApiVersionException;
import infra.web.accept.MediaTypeParamApiVersionResolver;
import infra.web.accept.PathApiVersionResolver;
import infra.web.accept.SemanticApiVersionParser;
import infra.web.accept.StandardApiVersionDeprecationHandler;

/**
 * Configure API versioning.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class ApiVersionConfigurer {

  private final ArrayList<ApiVersionResolver> versionResolvers = new ArrayList<>();

  @Nullable
  private ApiVersionParser<?> versionParser;

  @Nullable
  private Boolean versionRequired;

  @Nullable
  private String defaultVersion;

  private final Set<String> supportedVersions = new LinkedHashSet<>();

  private boolean detectSupportedVersions = true;

  @Nullable
  private ApiVersionDeprecationHandler deprecationHandler;

  /**
   * Add resolver to extract the version from a request header.
   *
   * @param headerName the header name to check
   */
  public ApiVersionConfigurer useRequestHeader(String headerName) {
    this.versionResolvers.add(request -> request.requestHeaders().getFirst(headerName));
    return this;
  }

  /**
   * Add resolver to extract the version from a request parameter.
   *
   * @param paramName the parameter name to check
   */
  public ApiVersionConfigurer useRequestParam(String paramName) {
    this.versionResolvers.add(request -> request.getParameter(paramName));
    return this;
  }

  /**
   * Add resolver to extract the version from a path segment.
   *
   * @param index the index of the path segment to check; e.g. for URL's like
   * "/{version}/..." use index 0, for "/api/{version}/..." index 1.
   */
  public ApiVersionConfigurer usePathSegment(int index) {
    this.versionResolvers.add(new PathApiVersionResolver(index));
    return this;
  }

  /**
   * Add resolver to extract the version from a media type parameter found in
   * the Accept or Content-Type headers.
   *
   * @param compatibleMediaType the media type to extract the parameter from with
   * the match established via {@link MediaType#isCompatibleWith(MediaType)}
   * @param paramName the name of the parameter
   */
  public ApiVersionConfigurer useMediaTypeParameter(MediaType compatibleMediaType, String paramName) {
    this.versionResolvers.add(new MediaTypeParamApiVersionResolver(compatibleMediaType, paramName));
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
   * Add to the list of supported versions to check against before raising
   * {@link InvalidApiVersionException} for unknown versions.
   * <p>By default, actual version values that appear in request mappings are
   * used for validation. Therefore, use of this method is optional. However,
   * if you prefer to use explicitly configured, supported versions only, then
   * set {@link #detectSupportedVersions} to {@code false}.
   * <p>Note that the initial API version, if not explicitly declared in any
   * request mappings, may need to be declared here instead as a supported
   * version.
   *
   * @param versions supported versions to add
   */
  public ApiVersionConfigurer addSupportedVersions(String... versions) {
    Collections.addAll(this.supportedVersions, versions);
    return this;
  }

  /**
   * Whether to use versions from mappings for supported version validation.
   * <p>By default, this is {@code true} in which case mapped versions are
   * considered supported versions. Set this to {@code false} if you want to
   * use only explicitly configured {@link #addSupportedVersions(String...)
   * supported versions}.
   *
   * @param detect whether to use detected versions for validation
   */
  public ApiVersionConfigurer detectSupportedVersions(boolean detect) {
    this.detectSupportedVersions = detect;
    return this;
  }

  /**
   * Configure a handler to add handling for requests with a deprecated API
   * version. Typically, this involves sending hints and information about
   * the deprecation in response headers.
   *
   * @param handler the handler to use
   * @see StandardApiVersionDeprecationHandler
   */
  public ApiVersionConfigurer setDeprecationHandler(@Nullable ApiVersionDeprecationHandler handler) {
    this.deprecationHandler = handler;
    return this;
  }

  @Nullable
  protected ApiVersionStrategy getApiVersionStrategy() {
    if (this.versionResolvers.isEmpty()) {
      Assert.state(isNotCustomized(), "API version config customized, but no ApiVersionResolver provided");
      return null;
    }

    var strategy = new DefaultApiVersionStrategy(this.versionResolvers,
            this.versionParser != null ? this.versionParser : new SemanticApiVersionParser(),
            this.versionRequired != null ? this.versionRequired : true,
            this.defaultVersion, this.detectSupportedVersions, this.deprecationHandler);

    for (String supportedVersion : supportedVersions) {
      strategy.addSupportedVersion(supportedVersion);
    }

    return strategy;
  }

  private boolean isNotCustomized() {
    return (this.versionParser == null && this.versionRequired == null &&
            this.defaultVersion == null && this.supportedVersions.isEmpty());
  }
}
