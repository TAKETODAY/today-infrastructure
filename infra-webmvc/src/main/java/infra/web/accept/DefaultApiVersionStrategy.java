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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import infra.lang.Assert;
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

  private final boolean detectSupportedVersions;

  private final Set<Comparable<?>> detectedVersions = new TreeSet<>();

  @Nullable
  private final ApiVersionDeprecationHandler deprecationHandler;

  private final Predicate<Comparable<?>> supportedVersionPredicate;

  /**
   * Create an instance.
   *
   * @param versionResolvers one or more resolvers to try; the first non-null
   * value returned by any resolver becomes the value used
   * @param versionParser parser for raw version values
   * @param versionRequired whether a version is required leading to
   * {@link MissingApiVersionException} for requests that don't have one;
   * by default set to true unless there is a defaultVersion
   * @param defaultVersion a default version to assign to requests that
   * don't specify one
   * @param detectSupportedVersions whether to use API versions that appear in
   * mappings for supported version validation (true), or use only explicitly
   * configured versions (false).
   * @param deprecationHandler handler to send hints and information about
   * deprecated API versions to clients
   */
  public DefaultApiVersionStrategy(List<ApiVersionResolver> versionResolvers,
          ApiVersionParser<?> versionParser, @Nullable Boolean versionRequired, @Nullable String defaultVersion,
          boolean detectSupportedVersions, @Nullable ApiVersionDeprecationHandler deprecationHandler,
          @Nullable Predicate<Comparable<?>> supportedVersionPredicate) {

    Assert.notEmpty(versionResolvers, "At least one ApiVersionResolver is required");
    Assert.notNull(versionParser, "ApiVersionParser is required");
    Assert.isTrue(defaultVersion == null || versionRequired == null || !versionRequired,
            "versionRequired cannot be set to true if a defaultVersion is also configured");

    this.versionResolvers = new ArrayList<>(versionResolvers);
    this.versionParser = versionParser;
    this.versionRequired = versionRequired != null ? versionRequired : defaultVersion == null;
    this.defaultVersion = defaultVersion != null ? versionParser.parseVersion(defaultVersion) : null;
    this.detectSupportedVersions = detectSupportedVersions;
    this.deprecationHandler = deprecationHandler;
    this.supportedVersionPredicate = initSupportedVersionPredicate(supportedVersionPredicate);

    if (defaultVersion != null) {
      addSupportedVersion(defaultVersion);
    }
  }

  @Nullable
  @Override
  public Comparable<?> getDefaultVersion() {
    return this.defaultVersion;
  }

  /**
   * Add to the list of supported versions to check against in
   * {@link ApiVersionStrategy#validateVersion} before raising
   * {@link InvalidApiVersionException} for unknown versions.
   * <p>By default, actual version values that appear in request mappings are
   * considered supported, and use of this method is optional. However, if you
   * prefer to use only explicitly configured, supported versions, then set
   * {@code detectSupportedVersions} flag to {@code false}.
   *
   * @param versions the supported versions to add
   * @see #addMappedVersion(String...)
   */
  public void addSupportedVersion(String... versions) {
    for (String version : versions) {
      this.supportedVersions.add(parseVersion(version));
    }
  }

  /**
   * Internal method to add to the list of actual version values that appear in
   * request mappings, which allows supported versions to be discovered rather
   * than {@link #addSupportedVersion(String...) configured}.
   * <p>If you prefer to use explicitly configured, supported versions only,
   * set the {@code detectSupportedVersions} flag to {@code false}.
   *
   * @param versions the versions to add
   * @see #addSupportedVersion(String...)
   */
  public void addMappedVersion(String... versions) {
    for (String version : versions) {
      this.detectedVersions.add(parseVersion(version));
    }
  }

  /**
   * Whether the strategy is configured to detect supported versions.
   * If this is set to {@code false} then {@link #addMappedVersion} is ignored
   * and the list of supported versions can be built explicitly through calls
   * to {@link #addSupportedVersion}.
   */
  public boolean detectSupportedVersions() {
    return this.detectSupportedVersions;
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

    if (!supportedVersionPredicate.test(requestVersion)) {
      throw new InvalidApiVersionException(requestVersion.toString());
    }
  }

  @Override
  public void handleDeprecations(Comparable<?> version, Object handler, RequestContext request) {
    if (deprecationHandler != null) {
      deprecationHandler.handleVersion(version, handler, request);
    }
  }

  private Predicate<Comparable<?>> initSupportedVersionPredicate(@Nullable Predicate<Comparable<?>> predicate) {
    return predicate != null ? predicate : new DefaultVersionPredicatePredicate();
  }

  @Override
  public String toString() {
    return "DefaultApiVersionStrategy[supportedVersions=%s, mappedVersions=%s, detectSupportedVersions=%s, versionRequired=%s, defaultVersion=%s]"
            .formatted(this.supportedVersions, this.detectedVersions, this.detectSupportedVersions, this.versionRequired, this.defaultVersion);
  }

  private final class DefaultVersionPredicatePredicate implements Predicate<Comparable<?>> {

    @Override
    public boolean test(Comparable<?> version) {
      return supportedVersions.contains(version) || (detectSupportedVersions && detectedVersions.contains(version));
    }

  }

}
