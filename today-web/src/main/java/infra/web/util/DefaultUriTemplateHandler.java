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

package infra.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link UriTemplateHandler} based on the use of
 * {@link UriComponentsBuilder} for expanding and encoding variables.
 *
 * <p>There are also several properties to customize how URI template handling
 * is performed, including a {@link #setBaseUrl baseUrl} to be used as a prefix
 * for all URI templates and a couple of encoding related options &mdash;
 * {@link #setParsePath parsePath} and {@link #setStrictEncoding strictEncoding}
 * respectively.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultUriTemplateHandler extends AbstractUriTemplateHandler {

  private boolean parsePath;

  private boolean strictEncoding;

  /**
   * Whether to parse the path of a URI template string into path segments.
   * <p>If set to {@code true} the URI template path is immediately decomposed
   * into path segments any URI variables expanded into it are then subject to
   * path segment encoding rules. In effect URI variables in the path have any
   * "/" characters percent encoded.
   * <p>By default this is set to {@code false} in which case the path is kept
   * as a full path and expanded URI variables will preserve "/" characters.
   *
   * @param parsePath whether to parse the path into path segments
   */
  public void setParsePath(boolean parsePath) {
    this.parsePath = parsePath;
  }

  /**
   * Whether the handler is configured to parse the path into path segments.
   */
  public boolean shouldParsePath() {
    return this.parsePath;
  }

  /**
   * Whether to encode characters outside the unreserved set as defined in
   * <a href="https://tools.ietf.org/html/rfc3986#section-2">RFC 3986 Section 2</a>.
   * This ensures a URI variable value will not contain any characters with a
   * reserved purpose.
   * <p>By default this is set to {@code false} in which case only characters
   * illegal for the given URI component are encoded. For example when expanding
   * a URI variable into a path segment the "/" character is illegal and
   * encoded. The ";" character however is legal and not encoded even though
   * it has a reserved purpose.
   * <p><strong>Note:</strong> this property supersedes the need to also set
   * the {@link #setParsePath parsePath} property.
   *
   * @param strictEncoding whether to perform strict encoding
   */
  public void setStrictEncoding(boolean strictEncoding) {
    this.strictEncoding = strictEncoding;
  }

  /**
   * Whether to strictly encode any character outside the unreserved set.
   */
  public boolean isStrictEncoding() {
    return this.strictEncoding;
  }

  @Override
  protected URI expandInternal(String uriTemplate, Map<String, ?> uriVariables) {
    UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
    UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
    return createUri(uriComponents);
  }

  @Override
  protected URI expandInternal(String uriTemplate, Object... uriVariables) {
    UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
    UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
    return createUri(uriComponents);
  }

  /**
   * Create a {@code UriComponentsBuilder} from the URI template string.
   * This implementation also breaks up the path into path segments depending
   * on whether {@link #setParsePath parsePath} is enabled.
   */
  protected UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {
    UriComponentsBuilder builder = UriComponentsBuilder.forURIString(uriTemplate);
    if (shouldParsePath() && !isStrictEncoding()) {
      List<String> pathSegments = builder.build().getPathSegments();
      builder.replacePath(null);
      for (String pathSegment : pathSegments) {
        builder.pathSegment(pathSegment);
      }
    }
    return builder;
  }

  protected UriComponents expandAndEncode(UriComponentsBuilder builder, Map<String, ?> uriVariables) {
    if (!isStrictEncoding()) {
      return builder.buildAndExpand(uriVariables).encode();
    }
    else {
      Map<String, ?> encodedUriVars = UriUtils.encodeUriVariables(uriVariables);
      return builder.buildAndExpand(encodedUriVars);
    }
  }

  protected UriComponents expandAndEncode(UriComponentsBuilder builder, Object[] uriVariables) {
    if (!isStrictEncoding()) {
      return builder.buildAndExpand(uriVariables).encode();
    }
    else {
      Object[] encodedUriVars = UriUtils.encodeUriVariables(uriVariables);
      return builder.buildAndExpand(encodedUriVars);
    }
  }

  private URI createUri(UriComponents uriComponents) {
    try {
      // Avoid further encoding (in the case of strictEncoding=true)
      return new URI(uriComponents.toUriString());
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
    }
  }

}
