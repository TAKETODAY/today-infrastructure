/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.accept;

import org.jspecify.annotations.Nullable;

import infra.web.RequestContext;

/**
 * The main component that encapsulates configuration preferences and strategies
 * to manage API versioning for an application.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface ApiVersionStrategy {

  /**
   * Resolve the version value from a request, e.g. from a request header.
   *
   * @param request the current request
   * @return the version, if present or {@code null}
   */
  @Nullable
  String resolveVersion(RequestContext request);

  /**
   * Parse the version of a request into an Object.
   *
   * @param version the value to parse
   * @return an Object that represents the version
   */
  Comparable<?> parseVersion(String version);

  /**
   * Validate a request version, including required and supported version checks.
   *
   * @param requestVersion the version to validate
   * @param request the request
   * @throws MissingApiVersionException if the version is required, but not specified
   * @throws InvalidApiVersionException if the version is not supported
   */
  void validateVersion(@Nullable Comparable<?> requestVersion, RequestContext request)
          throws MissingApiVersionException, InvalidApiVersionException;

  /**
   * Return a default version to use for requests that don't specify one.
   */
  @Nullable
  Comparable<?> getDefaultVersion();

  /**
   * Convenience method to return the parsed and validated request version,
   * or the default version if configured.
   *
   * @param request the current request
   * @return the parsed request version, or the default version
   * @throws MissingApiVersionException if the version is required, but not specified
   * @throws InvalidApiVersionException if the version is not supported
   */
  @Nullable
  default Comparable<?> resolveParseAndValidateVersion(RequestContext request) {
    String value = resolveVersion(request);
    Comparable<?> version;
    if (value == null) {
      version = getDefaultVersion();
    }
    else {
      try {
        version = parseVersion(value);
      }
      catch (Exception ex) {
        throw new InvalidApiVersionException(value, null, ex);
      }
    }
    validateVersion(version, request);
    return version;
  }

  /**
   * Check if the requested API version is deprecated, and if so handle it
   * accordingly, e.g. by setting response headers to signal the deprecation,
   * to specify relevant dates and provide links to further details.
   *
   * @param version the resolved and parsed request version
   * @param handler the handler chosen for the request
   * @param request the current request
   * @see ApiVersionDeprecationHandler
   */
  void handleDeprecations(Comparable<?> version, Object handler, RequestContext request);

}
