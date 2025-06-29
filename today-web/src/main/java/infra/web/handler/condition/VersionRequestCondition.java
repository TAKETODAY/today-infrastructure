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
import infra.util.StringUtils;
import infra.web.HandlerMapping;
import infra.web.RequestContext;
import infra.web.accept.ApiVersionStrategy;
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

  @Nullable
  private final String versionValue;

  @Nullable
  private final Object version;

  private final boolean baselineVersion;

  private final Set<String> content;

  /**
   * Constructor with the version, if set on the {@code @RequestMapping}, and
   * the {@code ApiVersionStrategy}, if API versioning is enabled.
   */
  public VersionRequestCondition(@Nullable String version, @Nullable ApiVersionStrategy strategy) {
    if (StringUtils.hasText(version)) {
      Assert.isTrue(strategy != null, "ApiVersionStrategy is required for mapping by version");
      this.baselineVersion = version.endsWith("+");
      this.versionValue = updateVersion(version, this.baselineVersion);
      this.version = strategy.parseVersion(this.versionValue);
      this.content = Set.of(version);
    }
    else {
      this.versionValue = null;
      this.version = null;
      this.baselineVersion = false;
      this.content = Collections.emptySet();
    }
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
    Comparable<?> requestVersion = (Comparable<?>) request.getAttribute(HandlerMapping.API_VERSION_ATTRIBUTE);

    if (this.version == null || requestVersion == null) {
      return this;
    }

    // Always use a baseline match here in order to select the highest version (baseline or fixed)
    // The fixed version match is enforced at the end in handleMatch()

    int result = compareVersions(this.version, requestVersion);
    return (result <= 0 ? this : null);
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
      Comparable<?> version = (Comparable<?>) request.getAttribute(HandlerMapping.API_VERSION_ATTRIBUTE);
      Assert.state(version != null, "No API version attribute");
      if (!this.version.equals(version)) {
        throw new NotAcceptableApiVersionException(version.toString());
      }
    }
  }

}
