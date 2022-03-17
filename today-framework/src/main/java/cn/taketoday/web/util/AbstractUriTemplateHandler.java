/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Abstract base class for {@link UriTemplateHandler} implementations.
 *
 * <p>Support {@link #setBaseUrl} and {@link #setDefaultUriVariables} properties
 * that should be relevant regardless of the URI template expand and encode
 * mechanism used in sub-classes.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractUriTemplateHandler implements UriTemplateHandler {

  @Nullable
  private String baseUrl;

  private final HashMap<String, Object> defaultUriVariables = new HashMap<>();

  /**
   * Configure a base URL to prepend URI templates with. The base URL must
   * have a scheme and host but may optionally contain a port and a path.
   * The base URL must be fully expanded and encoded which can be done via
   * {@link UriComponentsBuilder}.
   *
   * @param baseUrl the base URL.
   */
  public void setBaseUrl(@Nullable String baseUrl) {
    if (baseUrl != null) {
      UriComponents uriComponents = UriComponentsBuilder.fromUriString(baseUrl).build();
      Assert.hasText(uriComponents.getScheme(), "'baseUrl' must have a scheme");
      Assert.hasText(uriComponents.getHost(), "'baseUrl' must have a host");
      Assert.isNull(uriComponents.getQuery(), "'baseUrl' cannot have a query");
      Assert.isNull(uriComponents.getFragment(), "'baseUrl' cannot have a fragment");
    }
    this.baseUrl = baseUrl;
  }

  /**
   * Return the configured base URL.
   */
  @Nullable
  public String getBaseUrl() {
    return this.baseUrl;
  }

  /**
   * Configure default URI variable values to use with every expanded URI
   * template. These default values apply only when expanding with a Map, and
   * not with an array, where the Map supplied to {@link #expand(String, Map)}
   * can override the default values.
   *
   * @param defaultUriVariables the default URI variable values
   */
  public void setDefaultUriVariables(@Nullable Map<String, ?> defaultUriVariables) {
    this.defaultUriVariables.clear();
    if (defaultUriVariables != null) {
      this.defaultUriVariables.putAll(defaultUriVariables);
    }
  }

  /**
   * Return a read-only copy of the configured default URI variables.
   */
  public Map<String, ?> getDefaultUriVariables() {
    return Collections.unmodifiableMap(this.defaultUriVariables);
  }

  @Override
  public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
    if (!getDefaultUriVariables().isEmpty()) {
      Map<String, Object> map = new HashMap<>();
      map.putAll(getDefaultUriVariables());
      map.putAll(uriVariables);
      uriVariables = map;
    }
    URI url = expandInternal(uriTemplate, uriVariables);
    return insertBaseUrl(url);
  }

  @Override
  public URI expand(String uriTemplate, Object... uriVariables) {
    URI url = expandInternal(uriTemplate, uriVariables);
    return insertBaseUrl(url);
  }

  /**
   * Actually expand and encode the URI template.
   */
  protected abstract URI expandInternal(String uriTemplate, Map<String, ?> uriVariables);

  /**
   * Actually expand and encode the URI template.
   */
  protected abstract URI expandInternal(String uriTemplate, Object... uriVariables);

  /**
   * Insert a base URL (if configured) unless the given URL has a host already.
   */
  private URI insertBaseUrl(URI url) {
    try {
      String baseUrl = getBaseUrl();
      if (baseUrl != null && url.getHost() == null) {
        url = new URI(baseUrl + url);
      }
      return url;
    }
    catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Invalid URL after inserting base URL: " + url, ex);
    }
  }

}
