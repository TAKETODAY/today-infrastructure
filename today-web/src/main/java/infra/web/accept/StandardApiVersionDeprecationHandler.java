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

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * {@code ApiVersionDeprecationHandler} based on
 * <a href="https://datatracker.ietf.org/doc/html/rfc9745">RFC 9745</a> and
 * <a href="https://datatracker.ietf.org/doc/html/rfc8594">RFC 8594</a> that
 * provides the option to set the "Deprecation" and "Sunset" response headers,
 * as well as to add "Link" headers with further details about both.
 * <p>To use this handler, create an instance, call {@link #configureVersion}
 * for each deprecated version, and use the returned {@link VersionSpec} to
 * provide the deprecation details to send to clients.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class StandardApiVersionDeprecationHandler implements ApiVersionDeprecationHandler {

  private final ApiVersionParser<?> versionParser;

  private final Map<Comparable<?>, VersionInfo> infos = new HashMap<>();

  /**
   * Create an instance.
   * <p>By default, {@link SemanticApiVersionParser} is used to parse configured
   * API versions, so those can be compared to request versions parsed at runtime.
   * If you have a custom parser, then please use the
   * {@link #StandardApiVersionDeprecationHandler(ApiVersionParser)} constructor.
   */
  public StandardApiVersionDeprecationHandler() {
    this(new SemanticApiVersionParser());
  }

  /**
   * Variant of the default constructor with a custom {@link ApiVersionParser}.
   * This needs to be the same as the parser type used at runtime to parse
   * request versions.
   */
  public StandardApiVersionDeprecationHandler(ApiVersionParser<?> parser) {
    this.versionParser = parser;
  }

  /**
   * Mark the given API version as deprecated, and use the returned
   * {@link VersionSpec} to configure the deprecation details to send to clients.
   *
   * @param version the version to mark as deprecated
   * @return a spec to configure deprecation details
   */
  public VersionSpec configureVersion(String version) {
    Comparable<?> parsedVersion = this.versionParser.parseVersion(version);
    return new VersionSpec(parsedVersion);
  }

  @Override
  public void handleVersion(Comparable<?> requestVersion, RequestContext request) {
    for (VersionInfo info : this.infos.values()) {
      if (info.match(requestVersion, request)) {
        if (info.deprecationDate() != null) {
          request.setHeader("Deprecation", info.deprecationDate());
        }
        if (info.deprecationLink() != null) {
          request.addHeader(HttpHeaders.LINK, info.deprecationLink());
        }
        if (info.sunsetDate() != null) {
          request.setHeader("Sunset", info.sunsetDate());
        }
        if (info.sunsetLink() != null) {
          request.addHeader(HttpHeaders.LINK, info.sunsetLink());
        }
      }
    }
  }

  @Override
  public String toString() {
    return "StandardApiVersionDeprecationHandler " + this.infos.values();
  }

  /**
   * A spec to configure deprecation details for an API version.
   */
  public final class VersionSpec {

    private final Comparable<?> version;

    private VersionSpec(Comparable<?> version) {
      this.version = version;
      StandardApiVersionDeprecationHandler.this.infos.put(version, new VersionInfo(version));
    }

    /**
     * Set a predicate to filter which requests to send deprecation info to.
     * <p>By default, all requests with the deprecated version are handled.
     * This predicate to narrow the requests that should expose deprecation
     * information.
     *
     * @param predicate a predicate to check the request with
     * @return the same spec instance
     */
    public VersionSpec setRequestPredicate(Predicate<RequestContext> predicate) {
      return map(info -> info.withRequestPredicate(predicate));
    }

    /**
     * Specify a deprecation date for the "Deprecation" response header.
     *
     * @param date the deprecation date
     * @return the same spec instance
     */
    public VersionSpec setDeprecationDate(ZonedDateTime date) {
      return map(info -> info.withDeprecationDate(date));
    }

    /**
     * Specify a URL for the "Link" response header with
     * {@code rel="deprecation"} and {@code type="text/html"}.
     *
     * @param uri the link value
     * @return the same spec instance
     */
    public VersionSpec setDeprecationLink(URI uri) {
      return setDeprecationLink(uri, MediaType.TEXT_HTML);
    }

    /**
     * Variation of {@link #setDeprecationLink(URI)} for use with a media type
     * other than "text/html".
     *
     * @param uri the link value
     * @param mediaType the media type to use
     * @return the same spec instance
     */
    public VersionSpec setDeprecationLink(URI uri, MediaType mediaType) {
      return map(info -> info.withDeprecationLink(uri, mediaType));
    }

    /**
     * Specify a deprecation date for the "Sunset" response header.
     *
     * @param date the sunset date
     * @return the same spec instance
     */
    public VersionSpec setSunsetDate(ZonedDateTime date) {
      return map(info -> info.withSunsetDate(date));
    }

    /**
     * Specify a URL for the "Link" response header with
     * {@code rel="sunset"} and {@code type="text/html"}.
     *
     * @param uri the link value
     * @return the same spec instance
     */
    public VersionSpec setSunsetLink(URI uri) {
      return setSunsetLink(uri, MediaType.TEXT_HTML);
    }

    /**
     * Variation of {@link #setSunsetLink(URI)} for use with a media type
     * other than "text/html".
     *
     * @param uri the link value
     * @param mediaType the media type to use
     * @return the same spec instance
     */
    public VersionSpec setSunsetLink(URI uri, MediaType mediaType) {
      return map(info -> info.withSunsetLink(uri, mediaType));
    }

    private VersionSpec map(Function<VersionInfo, VersionInfo> function) {
      StandardApiVersionDeprecationHandler.this.infos.compute(this.version, (version, versionInfo) -> {
        Assert.state(versionInfo != null, "No VersionInfo");
        return function.apply(versionInfo);
      });
      return this;
    }

  }

  private record VersionInfo(Comparable<?> version,
          Predicate<RequestContext> requestPredicate,
          @Nullable String deprecationDate, @Nullable String deprecationLink,
          @Nullable String sunsetDate, @Nullable String sunsetLink) {

    VersionInfo(Comparable<?> version) {
      this(version, request -> true, null, null, null, null);
    }

    public VersionInfo withRequestPredicate(Predicate<RequestContext> predicate) {
      return new VersionInfo(version(), predicate,
              deprecationDate(), deprecationLink(), sunsetDate(), sunsetLink());
    }

    public VersionInfo withDeprecationDate(ZonedDateTime deprecationDate) {
      return new VersionInfo(version(), requestPredicate(),
              "@" + deprecationDate.toInstant().getEpochSecond(), deprecationLink(),
              sunsetDate(), sunsetLink());
    }

    public VersionInfo withDeprecationLink(URI uri, MediaType mediaType) {
      return new VersionInfo(version(), requestPredicate(),
              deprecationDate(), String.format("<%s>; rel=\"deprecation\"; type=\"%s\"", uri, mediaType),
              sunsetDate(), sunsetLink());
    }

    public VersionInfo withSunsetDate(ZonedDateTime sunsetDate) {
      return new VersionInfo(version(), requestPredicate(),
              deprecationDate(), deprecationLink(),
              sunsetDate.format(DateTimeFormatter.RFC_1123_DATE_TIME), sunsetLink());
    }

    public VersionInfo withSunsetLink(URI uri, MediaType mediaType) {
      return new VersionInfo(version(), requestPredicate(),
              deprecationDate(), deprecationLink(),
              sunsetDate(), String.format("<%s>; rel=\"sunset\"; type=\"%s\"", uri, mediaType));
    }

    boolean match(Comparable<?> requestVersion, RequestContext request) {
      return (version().equals(requestVersion) && requestPredicate().test(request));
    }
  }

}
