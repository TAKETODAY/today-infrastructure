/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;

/**
 * Extension of {@link UriComponents} for opaque URIs.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @see <a href="https://tools.ietf.org/html/rfc3986#section-1.2.3">Hierarchical vs Opaque URIs</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
final class OpaqueUriComponents extends UriComponents {
  private static final MultiValueMap<String, String> QUERY_PARAMS_NONE = MultiValueMap.empty();

  @Nullable
  private final String ssp;

  OpaqueUriComponents(@Nullable String scheme, @Nullable String schemeSpecificPart, @Nullable String fragment) {
    super(scheme, fragment);
    this.ssp = schemeSpecificPart;
  }

  @Override
  @Nullable
  public String getSchemeSpecificPart() {
    return this.ssp;
  }

  @Override
  @Nullable
  public String getUserInfo() {
    return null;
  }

  @Override
  @Nullable
  public String getHost() {
    return null;
  }

  @Override
  public int getPort() {
    return -1;
  }

  @Override
  @Nullable
  public String getPath() {
    return null;
  }

  @Override
  public List<String> getPathSegments() {
    return Collections.emptyList();
  }

  @Override
  @Nullable
  public String getQuery() {
    return null;
  }

  @Override
  public MultiValueMap<String, String> getQueryParams() {
    return QUERY_PARAMS_NONE;
  }

  @Override
  public UriComponents encode(Charset charset) {
    return this;
  }

  @Override
  protected UriComponents expandInternal(UriTemplateVariables uriVariables) {
    String expandedScheme = expandUriComponent(getScheme(), uriVariables);
    String expandedSsp = expandUriComponent(getSchemeSpecificPart(), uriVariables);
    String expandedFragment = expandUriComponent(getFragment(), uriVariables);
    return new OpaqueUriComponents(expandedScheme, expandedSsp, expandedFragment);
  }

  @Override
  public UriComponents normalize() {
    return this;
  }

  @Override
  public String toUriString() {
    StringBuilder uriBuilder = new StringBuilder();

    if (getScheme() != null) {
      uriBuilder.append(getScheme());
      uriBuilder.append(':');
    }
    if (this.ssp != null) {
      uriBuilder.append(this.ssp);
    }
    if (getFragment() != null) {
      uriBuilder.append('#');
      uriBuilder.append(getFragment());
    }

    return uriBuilder.toString();
  }

  @Override
  public URI toUri() {
    try {
      return new URI(getScheme(), this.ssp, getFragment());
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
    }
  }

  @Override
  protected void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
    if (getScheme() != null) {
      builder.scheme(getScheme());
    }
    if (getSchemeSpecificPart() != null) {
      builder.schemeSpecificPart(getSchemeSpecificPart());
    }
    if (getFragment() != null) {
      builder.fragment(getFragment());
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OpaqueUriComponents otherComp)) {
      return false;
    }
    return Objects.equals(getScheme(), otherComp.getScheme())
            && Objects.equals(this.ssp, otherComp.ssp)
            && Objects.equals(getFragment(), otherComp.getFragment());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getScheme(), this.ssp, getFragment());
  }

}
