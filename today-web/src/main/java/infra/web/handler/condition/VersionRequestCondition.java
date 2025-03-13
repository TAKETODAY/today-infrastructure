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

package infra.web.handler.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.InvalidApiVersionException;
import infra.web.accept.NotAcceptableApiVersionException;
import infra.web.annotation.RequestMapping;

/**
 * Request condition to map based on the API version of the request.
 * Versions can be fixed (e.g. "1.2") or baseline (e.g. "1.2+") as described
 * in {@link RequestMapping#version()}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class VersionRequestCondition extends AbstractRequestCondition<VersionRequestCondition> {

  private static final String VERSION_ATTRIBUTE_NAME = VersionRequestCondition.class.getName() + ".VERSION";

  private static final String NO_VERSION_ATTRIBUTE = "NO_VERSION";

  private static final ApiVersionStrategy NO_OP_VERSION_STRATEGY = new NoOpApiVersionStrategy();

  @Nullable
  private final String versionValue;

  @Nullable
  private final Object version;

  private final boolean baselineVersion;

  private final ApiVersionStrategy versionStrategy;

  private final Set<String> content;

  public VersionRequestCondition() {
    this.versionValue = null;
    this.version = null;
    this.baselineVersion = false;
    this.versionStrategy = NO_OP_VERSION_STRATEGY;
    this.content = Collections.emptySet();
  }

  public VersionRequestCondition(String configuredVersion, ApiVersionStrategy versionStrategy) {
    this.baselineVersion = configuredVersion.endsWith("+");
    this.versionValue = updateVersion(configuredVersion, this.baselineVersion);
    this.version = versionStrategy.parseVersion(this.versionValue);
    this.versionStrategy = versionStrategy;
    this.content = Set.of(configuredVersion);
  }

  private static String updateVersion(String version, boolean baselineVersion) {
    return (baselineVersion ? version.substring(0, version.length() - 1) : version);
  }

  @Override
  protected Collection<String> getContent() {
    return this.content;
  }

  @Override
  protected String getToStringInfix() {
    return " && ";
  }

  public @Nullable String getVersion() {
    return this.versionValue;
  }

  @Override
  public VersionRequestCondition combine(VersionRequestCondition other) {
    return (other.version != null ? other : this);
  }

  @Nullable
  @Override
  public VersionRequestCondition getMatchingCondition(RequestContext request) {
    if (this.version == null) {
      return this;
    }

    Comparable<?> version = (Comparable<?>) request.getAttribute(VERSION_ATTRIBUTE_NAME);
    if (version == null) {
      String value = this.versionStrategy.resolveVersion(request);
      version = (value != null ? parseVersion(value) : this.versionStrategy.getDefaultVersion());
      this.versionStrategy.validateVersion(version, request);
      version = (version != null ? version : NO_VERSION_ATTRIBUTE);
      request.setAttribute(VERSION_ATTRIBUTE_NAME, (version));
    }

    if (version == NO_VERSION_ATTRIBUTE) {
      return this;
    }

    // At this stage, match all versions as baseline versions.
    // Strict matching for fixed versions is enforced at the end in handleMatch.

    int result = compareVersions(this.version, version);
    return (result <= 0 ? this : null);
  }

  private Comparable<?> parseVersion(String value) {
    try {
      return this.versionStrategy.parseVersion(value);
    }
    catch (Exception ex) {
      throw new InvalidApiVersionException(value, null, ex);
    }
  }

  @SuppressWarnings("unchecked")
  private <V extends Comparable<V>> int compareVersions(Object v1, Object v2) {
    return ((V) v1).compareTo((V) v2);
  }

  @Override
  public int compareTo(VersionRequestCondition other, RequestContext request) {
    Object otherVersion = other.version;
    if (this.version == null && otherVersion == null) {
      return 0;
    }
    else if (this.version != null && otherVersion != null) {
      // make higher version bubble up
      return (-1 * compareVersions(this.version, otherVersion));
    }
    else {
      return (this.version != null ? -1 : 1);
    }
  }

  /**
   * Perform a final check on the matched request mapping version.
   * <p>In order to ensure baseline versions are properly capped by higher
   * fixed versions, initially we match all versions as baseline versions in
   * {@link #getMatchingCondition(RequestContext)}. Once the highest of
   * potentially multiple matches is selected, we enforce the strict match
   * for fixed versions.
   * <p>For example, given controller methods for "1.2+" and "1.5", and
   * a request for "1.6", both are matched, allowing "1.5" to be selected, but
   * that is then rejected as not acceptable since it is not an exact match.
   *
   * @param request the current request
   * @throws NotAcceptableApiVersionException if the matched condition has a
   * fixed version that is not equal to the request version
   */
  public void handleMatch(RequestContext request) {
    if (this.version != null && !this.baselineVersion) {
      Comparable<?> version = (Comparable<?>) request.getAttribute(VERSION_ATTRIBUTE_NAME);
      Assert.state(version != null, "No API version attribute");
      if (!this.version.equals(version)) {
        throw new NotAcceptableApiVersionException(version.toString());
      }
    }
  }

  private static final class NoOpApiVersionStrategy implements ApiVersionStrategy {

    @Nullable
    @Override
    public String resolveVersion(RequestContext request) {
      return null;
    }

    @Override
    public String parseVersion(String version) {
      return version;
    }

    @Override
    public void validateVersion(@Nullable Comparable<?> requestVersion, RequestContext request) {
    }

    @Nullable
    @Override
    public Comparable<?> getDefaultVersion() {
      return null;
    }
  }

}
