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
package cn.taketoday.http;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.InvalidMediaTypeException;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.StringUtils;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.US;

/**
 * A data structure representing HTTP request or response headers, mapping String header names
 * to a list of String values, also offering accessors for common application-level data types.
 *
 * <p>In addition to the regular methods defined by {@link Map}, this class offers many common
 * convenience methods, for example:
 * <ul>
 * <li>{@link #getFirst(String)} returns the first value associated with a given header name</li>
 * <li>{@link #add(String, String)} adds a header value to the list of values for a header name</li>
 * <li>{@link #set(String, String)} sets the header value to a single string value</li>
 * </ul>
 *
 * <p>Note that {@code HttpHeaders} generally treats header names in a case-insensitive manner.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Josh Long
 * @author Sam Brannen
 * @author TODAY 2020-01-28 17:15
 * @since 3.0
 */
public abstract class HttpHeaders
        implements /*Iterable<String>,*/ MultiValueMap<String, String>, Serializable {

  /**
   * The HTTP {@code Accept} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">Section 5.3.2 of RFC 7231</a>
   */
  public static final String ACCEPT = "Accept";
  /**
   * The HTTP {@code Accept-Charset} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.3">Section 5.3.3 of RFC 7231</a>
   */
  public static final String ACCEPT_CHARSET = "Accept-Charset";
  /**
   * The HTTP {@code Accept-Encoding} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.4">Section 5.3.4 of RFC 7231</a>
   */
  public static final String ACCEPT_ENCODING = "Accept-Encoding";
  /**
   * The HTTP {@code Accept-Language} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">Section 5.3.5 of RFC 7231</a>
   */
  public static final String ACCEPT_LANGUAGE = "Accept-Language";
  /**
   * The HTTP {@code Accept-Patch} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc5789#section-3.1">Section 3.1 of RFC 5789</a>
   */
  public static final String ACCEPT_PATCH = "Accept-Patch";
  /**
   * The HTTP {@code Accept-Ranges} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.3">Section 5.3.5 of RFC 7233</a>
   */
  public static final String ACCEPT_RANGES = "Accept-Ranges";
  /**
   * The CORS {@code Access-Control-Allow-Credentials} response header field name.
   *
   * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
   */
  public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
  /**
   * The CORS {@code Access-Control-Allow-Headers} response header field name.
   *
   * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
   */
  public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
  /**
   * The CORS {@code Access-Control-Allow-Methods} response header field name.
   *
   * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
   */
  public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
  /**
   * The CORS {@code Access-Control-Allow-Origin} response header field name.
   *
   * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
   */
  public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  /**
   * The CORS {@code Access-Control-Expose-Headers} response header field name.
   *
   * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
   */
  public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
  /**
   * The CORS {@code Access-Control-Max-Age} response header field name.
   *
   * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
   */
  public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
  /**
   * The CORS {@code Access-Control-Request-Headers} request header field name.
   *
   * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
   */
  public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
  /**
   * The CORS {@code Access-Control-Request-Method} request header field name.
   *
   * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
   */
  public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
  /**
   * The HTTP {@code Age} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.1">Section 5.1 of RFC 7234</a>
   */
  public static final String AGE = "Age";
  /**
   * The HTTP {@code Allow} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.1">Section 7.4.1 of RFC 7231</a>
   */
  public static final String ALLOW = "Allow";
  /**
   * The HTTP {@code Authorization} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.2">Section 4.2 of RFC 7235</a>
   */
  public static final String AUTHORIZATION = "Authorization";
  /**
   * The HTTP {@code Cache-Control} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">Section 5.2 of RFC 7234</a>
   */
  public static final String CACHE_CONTROL = "Cache-Control";
  /**
   * The HTTP {@code Connection} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.1">Section 6.1 of RFC 7230</a>
   */
  public static final String CONNECTION = "Connection";
  /**
   * The HTTP {@code Content-Encoding} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.2.2">Section 3.1.2.2 of RFC 7231</a>
   */
  public static final String CONTENT_ENCODING = "Content-Encoding";
  /**
   * The HTTP {@code Content-Disposition} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc6266">RFC 6266</a>
   */
  public static final String CONTENT_DISPOSITION = "Content-Disposition";
  /**
   * The HTTP {@code Content-Language} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.3.2">Section 3.1.3.2 of RFC 7231</a>
   */
  public static final String CONTENT_LANGUAGE = "Content-Language";
  /**
   * The HTTP {@code Content-Length} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.2">Section 3.3.2 of RFC 7230</a>
   */
  public static final String CONTENT_LENGTH = "Content-Length";
  /**
   * The HTTP {@code Content-Location} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.4.2">Section 3.1.4.2 of RFC 7231</a>
   */
  public static final String CONTENT_LOCATION = "Content-Location";
  /**
   * The HTTP {@code Content-Range} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7233#section-4.2">Section 4.2 of RFC 7233</a>
   */
  public static final String CONTENT_RANGE = "Content-Range";
  /**
   * The HTTP {@code Content-Type} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">Section 3.1.1.5 of RFC 7231</a>
   */
  public static final String CONTENT_TYPE = "Content-Type";
  /**
   * The HTTP {@code Cookie} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc2109#section-4.3.4">Section 4.3.4 of RFC 2109</a>
   */
  public static final String COOKIE = "Cookie";
  /**
   * The HTTP {@code Date} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">Section 7.1.1.2 of RFC 7231</a>
   */
  public static final String DATE = "Date";
  /**
   * The HTTP {@code ETag} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of RFC 7232</a>
   */
  public static final String ETAG = "ETag";
  /**
   * The HTTP {@code Expect} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.1.1">Section 5.1.1 of RFC 7231</a>
   */
  public static final String EXPECT = "Expect";
  /**
   * The HTTP {@code Expires} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.3">Section 5.3 of RFC 7234</a>
   */
  public static final String EXPIRES = "Expires";
  /**
   * The HTTP {@code From} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.1">Section 5.5.1 of RFC 7231</a>
   */
  public static final String FROM = "From";
  /**
   * The HTTP {@code Host} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.4">Section 5.4 of RFC 7230</a>
   */
  public static final String HOST = "Host";
  /**
   * The HTTP {@code If-Match} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.1">Section 3.1 of RFC 7232</a>
   */
  public static final String IF_MATCH = "If-Match";
  /**
   * The HTTP {@code If-Modified-Since} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.3">Section 3.3 of RFC 7232</a>
   */
  public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
  /**
   * The HTTP {@code If-None-Match} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.2">Section 3.2 of RFC 7232</a>
   */
  public static final String IF_NONE_MATCH = "If-None-Match";
  /**
   * The HTTP {@code If-Range} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7233#section-3.2">Section 3.2 of RFC 7233</a>
   */
  public static final String IF_RANGE = "If-Range";
  /**
   * The HTTP {@code If-Unmodified-Since} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.4">Section 3.4 of RFC 7232</a>
   */
  public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
  /**
   * The HTTP {@code Last-Modified} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.2">Section 2.2 of RFC 7232</a>
   */
  public static final String LAST_MODIFIED = "Last-Modified";
  /**
   * The HTTP {@code Link} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc5988">RFC 5988</a>
   */
  public static final String LINK = "Link";
  /**
   * The HTTP {@code Location} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">Section 7.1.2 of RFC 7231</a>
   */
  public static final String LOCATION = "Location";
  /**
   * The HTTP {@code Max-Forwards} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.1.2">Section 5.1.2 of RFC 7231</a>
   */
  public static final String MAX_FORWARDS = "Max-Forwards";
  /**
   * The HTTP {@code Origin} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454</a>
   */
  public static final String ORIGIN = "Origin";
  /**
   * The HTTP {@code Pragma} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.4">Section 5.4 of RFC 7234</a>
   */
  public static final String PRAGMA = "Pragma";
  /**
   * The HTTP {@code Proxy-Authenticate} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.3">Section 4.3 of RFC 7235</a>
   */
  public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
  /**
   * The HTTP {@code Proxy-Authorization} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.4">Section 4.4 of RFC 7235</a>
   */
  public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
  /**
   * The HTTP {@code Range} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7233#section-3.1">Section 3.1 of RFC 7233</a>
   */
  public static final String RANGE = "Range";
  /**
   * The HTTP {@code Referer} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.2">Section 5.5.2 of RFC 7231</a>
   */
  public static final String REFERER = "Referer";
  /**
   * The HTTP {@code Retry-After} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">Section 7.1.3 of RFC 7231</a>
   */
  public static final String RETRY_AFTER = "Retry-After";
  /**
   * The HTTP {@code Server} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">Section 7.4.2 of RFC 7231</a>
   */
  public static final String SERVER = "Server";
  /**
   * The HTTP {@code Set-Cookie} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc2109#section-4.2.2">Section 4.2.2 of RFC 2109</a>
   */
  public static final String SET_COOKIE = "Set-Cookie";
  /**
   * The HTTP {@code Set-Cookie2} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc2965">RFC 2965</a>
   */
  public static final String SET_COOKIE2 = "Set-Cookie2";
  /**
   * The HTTP {@code TE} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7230#section-4.3">Section 4.3 of RFC 7230</a>
   */
  public static final String TE = "TE";
  /**
   * The HTTP {@code Trailer} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7230#section-4.4">Section 4.4 of RFC 7230</a>
   */
  public static final String TRAILER = "Trailer";
  /**
   * The HTTP {@code Transfer-Encoding} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.1">Section 3.3.1 of RFC 7230</a>
   */
  public static final String TRANSFER_ENCODING = "Transfer-Encoding";
  /**
   * The HTTP {@code Upgrade} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.7">Section 6.7 of RFC 7230</a>
   */
  public static final String UPGRADE = "Upgrade";
  /**
   * The HTTP {@code User-Agent} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.3">Section 5.5.3 of RFC 7231</a>
   */
  public static final String USER_AGENT = "User-Agent";
  /**
   * The HTTP {@code Vary} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.4">Section 7.1.4 of RFC 7231</a>
   */
  public static final String VARY = "Vary";
  /**
   * The HTTP {@code Via} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.7.1">Section 5.7.1 of RFC 7230</a>
   */
  public static final String VIA = "Via";
  /**
   * The HTTP {@code Warning} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.5">Section 5.5 of RFC 7234</a>
   */
  public static final String WARNING = "Warning";
  /**
   * The HTTP {@code WWW-Authenticate} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.1">Section 4.1 of RFC 7235</a>
   */
  public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

  public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

  // X-Requested-With

  public static final String X_REQUESTED_WITH = "X-Requested-With";

  // WebSocket

  public static final String SEC_WEBSOCKET_ORIGIN = "Sec-WebSocket-Origin";
  public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";
  public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
  public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
  public static final String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

  public static final String SEC_WEBSOCKET_LOCATION = "Sec-WebSocket-Location";

  public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
  public static final String WEBSOCKET_LOCATION = "WebSocket-Location";
  public static final String WEBSOCKET_PROTOCOL = "WebSocket-Protocol";
  public static final String SEC_WEBSOCKET_KEY1 = "Sec-WebSocket-Key1";
  public static final String SEC_WEBSOCKET_KEY2 = "Sec-WebSocket-Key2";

  //  ====================
  // Header values

  public static final String NONE = "none";
  public static final String GZIP = "gzip";
  public static final String BYTES = "bytes";
  public static final String CLOSE = "close";
  public static final String PUBLIC = "public";
  public static final String BASE64 = "base64";
  public static final String BINARY = "binary";
  public static final String CHUNKED = "chunked";
  public static final String CHARSET = "charset";
  public static final String MAX_AGE = "max-age";
  public static final String DEFLATE = "deflate";
  public static final String PRIVATE = "private";
  public static final String BOUNDARY = "boundary";
  public static final String IDENTITY = "identity";
  public static final String NO_CACHE = "no-cache";
  public static final String NO_STORE = "no-store";
  public static final String S_MAXAGE = "s-maxage";
  public static final String TRAILERS = "trailers";
  public static final String COMPRESS = "compress";
  public static final String MAX_STALE = "max-stale";
  public static final String MIN_FRESH = "min-fresh";
  public static final String WEBSOCKET = "WebSocket";
  public static final String KEEP_ALIVE = "keep-alive";
  public static final String GZIP_DEFLATE = "gzip,deflate";
  public static final String CONTINUE = "100-continue";
  public static final String NO_TRANSFORM = "no-transform";
  public static final String ONLY_IF_CACHED = "only-if-cached";
  public static final String XML_HTTP_REQUEST = "XMLHttpRequest";
  public static final String MUST_REVALIDATE = "must-revalidate";
  public static final String PROXY_REVALIDATE = "proxy-revalidate";
  public static final String QUOTED_PRINTABLE = "quoted-printable";
  public static final String MULTIPART_FORM_DATA = "multipart/form-data";
  public static final String INLINE_FILE_NAME = "inline;filename=\"";
  public static final String ATTACHMENT_FILE_NAME = "attachment;filename=\"";
  public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  public static final String APPLICATION_FORCE_DOWNLOAD = "application/force-download;";
  public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

  /**
   * Pattern matching ETag multiple field values in headers such as "If-Match",
   * "If-None-Match".
   *
   * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of
   * RFC 7232</a>
   */
  public static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");
  public static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.ENGLISH);
  public static final ZoneId GMT = ZoneId.of("GMT");

  /**
   * Date formats with time zone as specified in the HTTP RFC to use for
   * formatting.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section
   * 7.1.1.1 of RFC 7231</a>
   */
  public static final DateTimeFormatter DATE_FORMATTER = ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", US).withZone(GMT);

  /**
   * Date formats with time zone as specified in the HTTP RFC to use for parsing.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section
   * 7.1.1.1 of RFC 7231</a>
   */
  public static final DateTimeFormatter[] DATE_PARSERS = new DateTimeFormatter[] { //
          DateTimeFormatter.RFC_1123_DATE_TIME, //
          ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", US), //
          ofPattern("EEE MMM dd HH:mm:ss yyyy", US).withZone(GMT)
  };

  /**
   * An empty {@code HttpHeaders} instance (immutable).
   *
   * @since 4.0
   */
  public static final HttpHeaders EMPTY = new ReadOnlyHttpHeaders(new DefaultMultiValueMap<>());

  /**
   * Get the list of header values for the given header name, if any.
   *
   * @param headerName the header name
   * @return the list of header values, or an empty list
   */
  public List<String> getOrEmpty(String headerName) {
    List<String> values = get(headerName);
    return (values != null ? values : Collections.emptyList());
  }

  /**
   * Set the list of acceptable {@linkplain MediaType media types}, as specified
   * by the {@code Accept} header.
   */
  public void setAccept(Collection<MediaType> acceptableMediaTypes) {
    set(ACCEPT, MediaType.toString(acceptableMediaTypes));
  }

  /**
   * Return the list of acceptable {@linkplain MediaType media types}, as
   * specified by the {@code Accept} header.
   * <p>
   * Returns an empty list when the acceptable media types are unspecified.
   */
  public List<MediaType> getAccept() {
    return MediaType.parseMediaTypes(get(ACCEPT));
  }

  /**
   * Set the acceptable language ranges, as specified by the
   * {@literal Accept-Language} header.
   */
  public void setAcceptLanguage(Collection<Locale.LanguageRange> languages) {
    Assert.notNull(languages, "LanguageRange List must not be null");

    List<String> values = languages.stream().map(range -> {
      if (range.getWeight() == Locale.LanguageRange.MAX_WEIGHT) {
        return range.getRange();
      }
      DecimalFormat decimal = new DecimalFormat("0.0", DECIMAL_FORMAT_SYMBOLS);
      return range.getRange() + ";q=" + decimal.format(range.getWeight());
    }).collect(Collectors.toList());
    set(ACCEPT_LANGUAGE, toCommaDelimitedString(values));
  }

  /**
   * Return the language ranges from the {@literal "Accept-Language"} header.
   * <p>
   * If you only need sorted, preferred locales only use
   * {@link #getAcceptLanguageAsLocales()} or if you need to filter based on a
   * list of supported locales you can pass the returned list to
   * {@link Locale#filter(List, Collection)}.
   *
   * @throws IllegalArgumentException if the value cannot be converted to a language range
   */
  public List<Locale.LanguageRange> getAcceptLanguage() {
    String value = getFirst(ACCEPT_LANGUAGE);
    return StringUtils.isNotEmpty(value)
           ? Locale.LanguageRange.parse(value) : Collections.emptyList();
  }

  /**
   * Variant of {@link #setAcceptLanguage(Collection)} using {@link Locale}'s.
   */
  public void setAcceptLanguageAsLocales(Collection<Locale> locales) {
    setAcceptLanguage(locales.stream()
            .map(locale -> new Locale.LanguageRange(locale.toLanguageTag()))
            .collect(Collectors.toList()));
  }

  /**
   * A variant of {@link #getAcceptLanguage()} that converts each
   * {@link java.util.Locale.LanguageRange} to a {@link Locale}.
   *
   * @return the locales or an empty list
   * @throws IllegalArgumentException if the value cannot be converted to a locale
   */
  public List<Locale> getAcceptLanguageAsLocales() {
    List<Locale.LanguageRange> ranges = getAcceptLanguage();
    if (ranges.isEmpty()) {
      return Collections.emptyList();
    }
    return ranges.stream()
            .map(range -> Locale.forLanguageTag(range.getRange()))
            .filter(locale -> StringUtils.isNotEmpty(locale.getDisplayName()))
            .collect(Collectors.toList());
  }

  /**
   * Set the list of acceptable {@linkplain MediaType media types} for
   * {@code PATCH} methods, as specified by the {@code Accept-Patch} header.
   *
   * @since 4.0
   */
  public void setAcceptPatch(Collection<MediaType> mediaTypes) {
    set(ACCEPT_PATCH, MediaType.toString(mediaTypes));
  }

  /**
   * Return the list of acceptable {@linkplain MediaType media types} for
   * {@code PATCH} methods, as specified by the {@code Accept-Patch} header.
   * <p>Returns an empty list when the acceptable media types are unspecified.
   *
   * @since 4.0
   */
  public List<MediaType> getAcceptPatch() {
    return MediaType.parseMediaTypes(get(ACCEPT_PATCH));
  }

  /**
   * Set the (new) value of the {@code Access-Control-Allow-Credentials} response
   * header.
   */
  public void setAccessControlAllowCredentials(boolean allowCredentials) {
    set(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.toString(allowCredentials));
  }

  /**
   * Return the value of the {@code Access-Control-Allow-Credentials} response
   * header.
   */
  public boolean getAccessControlAllowCredentials() {
    return Boolean.parseBoolean(getFirst(ACCESS_CONTROL_ALLOW_CREDENTIALS));
  }

  /**
   * Set the (new) value of the {@code Access-Control-Allow-Headers} response
   * header.
   */
  public void setAccessControlAllowHeaders(Collection<String> allowedHeaders) {
    set(ACCESS_CONTROL_ALLOW_HEADERS, toCommaDelimitedString(allowedHeaders));
  }

  /**
   * Return the value of the {@code Access-Control-Allow-Headers} response header.
   */
  public List<String> getAccessControlAllowHeaders() {
    return getValuesAsList(ACCESS_CONTROL_ALLOW_HEADERS);
  }

  /**
   * Set the (new) value of the {@code Access-Control-Allow-Methods} response
   * header.
   */
  public void setAccessControlAllowMethods(Collection<?> allowedMethods) {
    set(ACCESS_CONTROL_ALLOW_METHODS, toCommaDelimitedString(allowedMethods));
  }

  /**
   * Return the value of the {@code Access-Control-Allow-Methods} response header.
   */
  public List<HttpMethod> getAccessControlAllowMethods() {
    ArrayList<HttpMethod> result = new ArrayList<>();
    String value = getFirst(ACCESS_CONTROL_ALLOW_METHODS);
    if (value != null) {
      String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
      for (String token : tokens) {
        result.add(HttpMethod.valueOf(token));
      }
    }
    return result;
  }

  /**
   * Set the (new) value of the {@code Access-Control-Allow-Origin} response
   * header.
   */
  public void setAccessControlAllowOrigin(String allowedOrigin) {
    setOrRemove(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
  }

  /**
   * Return the value of the {@code Access-Control-Allow-Origin} response header.
   */
  public String getAccessControlAllowOrigin() {
    return getFieldValues(ACCESS_CONTROL_ALLOW_ORIGIN);
  }

  /**
   * Set the (new) value of the {@code Access-Control-Expose-Headers} response
   * header.
   */
  public void setAccessControlExposeHeaders(Collection<String> exposedHeaders) {
    set(ACCESS_CONTROL_EXPOSE_HEADERS, toCommaDelimitedString(exposedHeaders));
  }

  /**
   * Return the value of the {@code Access-Control-Expose-Headers} response
   * header.
   */
  public List<String> getAccessControlExposeHeaders() {
    return getValuesAsList(ACCESS_CONTROL_EXPOSE_HEADERS);
  }

  /**
   * Set the (new) value of the {@code Access-Control-Max-Age} response header.
   */
  public void setAccessControlMaxAge(Duration maxAge) {
    set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge.getSeconds()));
  }

  /**
   * Set the (new) value of the {@code Access-Control-Max-Age} response header.
   */
  public void setAccessControlMaxAge(long maxAge) {
    set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge));
  }

  /**
   * Return the value of the {@code Access-Control-Max-Age} response header.
   * <p>
   * Returns -1 when the max age is unknown.
   */
  public long getAccessControlMaxAge() {
    String value = getFirst(ACCESS_CONTROL_MAX_AGE);
    return (value != null ? Long.parseLong(value) : -1);
  }

  /**
   * Set the (new) value of the {@code Access-Control-Request-Headers} request
   * header.
   */
  public void setAccessControlRequestHeaders(Collection<String> requestHeaders) {
    set(ACCESS_CONTROL_REQUEST_HEADERS, toCommaDelimitedString(requestHeaders));
  }

  /**
   * Return the value of the {@code Access-Control-Request-Headers} request
   * header.
   */
  public List<String> getAccessControlRequestHeaders() {
    return getValuesAsList(ACCESS_CONTROL_REQUEST_HEADERS);
  }

  /**
   * Set the (new) value of the {@code Access-Control-Request-Method} request
   * header.
   */
  public void setAccessControlRequestMethod(HttpMethod requestMethod) {
    setOrRemove(ACCESS_CONTROL_REQUEST_METHOD, (requestMethod != null ? requestMethod.name() : null));
  }

  /**
   * Return the value of the {@code Access-Control-Request-Method} request header.
   */
  @Nullable
  public HttpMethod getAccessControlRequestMethod() {
    String first = getFirst(ACCESS_CONTROL_REQUEST_METHOD);
    if (StringUtils.isEmpty(first)) {
      return null;
    }
    return HttpMethod.valueOf(first);
  }

  /**
   * Set the list of acceptable {@linkplain Charset charsets}, as specified by the
   * {@code Accept-Charset} header.
   */
  public void setAcceptCharset(Collection<Charset> acceptableCharsets) {
    StringJoiner joiner = new StringJoiner(", ");
    for (Charset charset : acceptableCharsets) {
      joiner.add(charset.name().toLowerCase(Locale.ENGLISH));
    }
    set(ACCEPT_CHARSET, joiner.toString());
  }

  /**
   * Return the list of acceptable {@linkplain Charset charsets}, as specified by
   * the {@code Accept-Charset} header.
   */
  public List<Charset> getAcceptCharset() {
    String value = getFirst(ACCEPT_CHARSET);
    if (value != null) {
      String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
      ArrayList<Charset> result = new ArrayList<>(tokens.length);
      for (String token : tokens) {
        int paramIdx = token.indexOf(';');
        String charsetName;
        if (paramIdx == -1) {
          charsetName = token;
        }
        else {
          charsetName = token.substring(0, paramIdx);
        }
        if (!charsetName.equals("*")) {
          result.add(Charset.forName(charsetName));
        }
      }
      return result;
    }
    else {
      return Collections.emptyList();
    }
  }

  /**
   * Set the set of allowed {@link HttpMethod HTTP methods}, as specified by
   * the {@code Allow} header.
   */
  public void setAllow(Collection<HttpMethod> allowedMethods) {
    set(ALLOW, StringUtils.collectionToString(allowedMethods)); // special case
  }

  /**
   * Return the set of allowed {@link HttpMethod HTTP methods}, as specified by
   * the {@code Allow} header.
   * <p>
   * Returns an empty set when the allowed methods are unspecified.
   */
  public Set<HttpMethod> getAllow() {
    String value = getFirst(ALLOW);
    if (StringUtils.isNotEmpty(value)) {
      String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
      ArrayList<HttpMethod> result = new ArrayList<>(tokens.length);
      for (String token : tokens) {
        result.add(HttpMethod.from(token));
      }
      return EnumSet.copyOf(result);
    }
    else {
      return EnumSet.noneOf(HttpMethod.class);
    }
  }

  /**
   * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
   * Basic Authentication
   *
   * @param encodedCredentials the encoded credentials
   * @throws IllegalArgumentException if supplied credentials string is {@code null} or blank
   * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
   */
  public void setBasicAuth(String encodedCredentials) {
    Assert.hasText(encodedCredentials, "'encodedCredentials' must not be null or blank");
    set(AUTHORIZATION, "Basic " + encodedCredentials);
  }

  /**
   * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
   * Basic Authentication based on the given username and password.
   * <p>Note that this method only supports characters in the
   * {@link StandardCharsets#ISO_8859_1 ISO-8859-1} character set.
   *
   * @param username the username
   * @param password the password
   * @throws IllegalArgumentException if either {@code user} or
   * {@code password} contain characters that cannot be encoded to ISO-8859-1
   * @see #setBasicAuth(String)
   * @see #setBasicAuth(String, String, Charset)
   * @see #encodeBasicAuth(String, String, Charset)
   * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
   */
  public void setBasicAuth(String username, String password) {
    setBasicAuth(username, password, null);
  }

  /**
   * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
   * Basic Authentication based on the given username and password.
   *
   * @param username the username
   * @param password the password
   * @param charset the charset to use to convert the credentials into an octet
   * sequence. Defaults to {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}.
   * @throws IllegalArgumentException if {@code username} or {@code password}
   * contains characters that cannot be encoded to the given charset
   * @see #setBasicAuth(String)
   * @see #setBasicAuth(String, String)
   * @see #encodeBasicAuth(String, String, Charset)
   * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
   */
  public void setBasicAuth(String username, String password, Charset charset) {
    setBasicAuth(encodeBasicAuth(username, password, charset));
  }

  /**
   * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
   * the given Bearer token.
   *
   * @param token the Base64 encoded token
   * @see <a href="https://tools.ietf.org/html/rfc6750">RFC 6750</a>
   */
  public void setBearerAuth(String token) {
    set(AUTHORIZATION, "Bearer " + token);
  }

  /**
   * Set a configured {@link CacheControl} instance as the new value of the
   * {@code Cache-Control} header.
   */
  public void setCacheControl(CacheControl cacheControl) {
    setOrRemove(CACHE_CONTROL, cacheControl.getHeaderValue());
  }

  /**
   * Set the (new) value of the {@code Cache-Control} header.
   */
  public void setCacheControl(String cacheControl) {
    setOrRemove(CACHE_CONTROL, cacheControl);
  }

  /**
   * Return the value of the {@code Cache-Control} header.
   */
  public String getCacheControl() {
    return getFieldValues(CACHE_CONTROL);
  }

  /**
   * Set the (new) value of the {@code Connection} header.
   */
  public void setConnection(String connection) {
    set(CONNECTION, connection);
  }

  /**
   * Set the (new) value of the {@code Connection} header.
   */
  public void setConnection(Collection<String> connection) {
    set(CONNECTION, toCommaDelimitedString(connection));
  }

  /**
   * Return the value of the {@code Connection} header.
   */
  public List<String> getConnection() {
    return getValuesAsList(CONNECTION);
  }

  /**
   * Set the {@code Content-Disposition} header when creating a
   * {@code "multipart/form-data"} request.
   * <p>
   * Applications typically would not set this header directly but rather prepare
   * a {@code MultiValueMap<String, Object>}, containing an Object or a
   * Resource for each part, and then pass that to the
   * {@code RestTemplate} or {@code WebClient}.
   *
   * @param name the control name
   * @param filename the filename (may be {@code null})
   * @see #getContentDisposition()
   */
  public void setContentDispositionFormData(String name, String filename) {
    Assert.notNull(name, "Name must not be null");
    ContentDisposition.Builder disposition = ContentDisposition.builder("form-data").name(name);
    if (filename != null) {
      disposition.filename(filename);
    }
    setContentDisposition(disposition.build());
  }

  /**
   * Set the {@literal Content-Disposition} header.
   * <p>
   * This could be used on a response to indicate if the content is expected to be
   * displayed inline in the browser or as an attachment to be saved locally.
   * <p>
   * It can also be used for a {@code "multipart/form-data"} request. For more
   * details see notes on {@link #setContentDispositionFormData}.
   *
   * @see #getContentDisposition()
   */
  public void setContentDisposition(ContentDisposition contentDisposition) {
    set(CONTENT_DISPOSITION, contentDisposition.toString());
  }

  public void setContentDisposition(String contentDisposition) {
    set(CONTENT_DISPOSITION, contentDisposition);
  }

  /**
   * Return a parsed representation of the {@literal Content-Disposition} header.
   *
   * @see #setContentDisposition(ContentDisposition)
   */
  public ContentDisposition getContentDisposition() {
    String contentDisposition = getFirst(CONTENT_DISPOSITION);
    return contentDisposition != null
           ? ContentDisposition.parse(contentDisposition)
           : ContentDisposition.empty();
  }

  /**
   * Set the {@link Locale} of the content language, as specified by the
   * {@literal Content-Language} header.
   * <p>
   * Use {@code put(CONTENT_LANGUAGE, list)} if you need to set multiple content
   * languages.
   * </p>
   */
  public void setContentLanguage(Locale locale) {
    setOrRemove(CONTENT_LANGUAGE, (locale != null ? locale.toLanguageTag() : null));
  }

  /**
   * Return the first {@link Locale} of the content languages, as specified by the
   * {@literal Content-Language} header.
   * <p>
   * Returns {@code null} when the content language is unknown.
   * <p>
   * Use {@code getValuesAsList(CONTENT_LANGUAGE)} if you need to get multiple
   * content languages.
   * </p>
   */
  @Nullable
  public Locale getContentLanguage() {
    return getValuesAsList(CONTENT_LANGUAGE)
            .stream()
            .findFirst()
            .map(Locale::forLanguageTag)
            .orElse(null);
  }

  /**
   * Set the length of the body in bytes, as specified by the
   * {@code Content-Length} header.
   */
  public void setContentLength(long contentLength) {
    set(CONTENT_LENGTH, Long.toString(contentLength));
  }

  /**
   * Return the length of the body in bytes, as specified by the
   * {@code Content-Length} header.
   * <p>
   * Returns -1 when the content-length is unknown.
   */
  public long getContentLength() {
    String value = getFirst(CONTENT_LENGTH);
    return (value != null ? Long.parseLong(value) : -1);
  }

  /**
   * Set the {@linkplain MediaType media type} of the body, as specified by the
   * {@code Content-Type} header.
   */
  public void setContentType(MediaType mediaType) {
    if (mediaType != null) {
      Assert.isTrue(!mediaType.isWildcardType(), "Content-Type cannot contain wildcard type '*'");
      Assert.isTrue(!mediaType.isWildcardSubtype(), "Content-Type cannot contain wildcard subtype '*'");
      set(CONTENT_TYPE, mediaType.toString());
    }
    else {
      remove(CONTENT_TYPE);
    }
  }

  /**
   * Return the {@linkplain MediaType media type} of the body, as specified by the
   * {@code Content-Type} header.
   * <p>
   * Returns {@code null} when the content-type is unknown.
   *
   * @throws InvalidMediaTypeException if the media type value cannot be parsed
   */
  @Nullable
  public MediaType getContentType() {
    String value = getFirst(CONTENT_TYPE);
    return (StringUtils.isNotEmpty(value) ? MediaType.parseMediaType(value) : null);
  }

  /**
   * Set the date and time at which the message was created, as specified by the
   * {@code Date} header.
   */
  public void setDate(ZonedDateTime date) {
    setZonedDateTime(DATE, date);
  }

  /**
   * Set the date and time at which the message was created, as specified by the
   * {@code Date} header.
   */
  public void setDate(Instant date) {
    setInstant(DATE, date);
  }

  /**
   * Set the date and time at which the message was created, as specified by the
   * {@code Date} header.
   * <p>
   * The date should be specified as the number of milliseconds since January 1,
   * 1970 GMT.
   */
  public void setDate(long date) {
    setDate(DATE, date);
  }

  /**
   * Return the date and time at which the message was created, as specified by
   * the {@code Date} header.
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT.
   * Returns -1 when the date is unknown.
   *
   * @throws IllegalArgumentException if the value cannot be converted to a date
   */
  public long getDate() {
    return getFirstDate(DATE);
  }

  /**
   * Set the (new) entity tag of the body, as specified by the {@code ETag}
   * header.
   */
  public void setETag(String etag) {
    if (etag != null) {
      Assert.isTrue(etag.startsWith("\"") || etag.startsWith("W/"), "Invalid ETag: does not start with W/ or \"");
      Assert.isTrue(etag.endsWith("\""), "Invalid ETag: does not end with \"");
      set(ETAG, etag);
    }
    else {
      remove(ETAG);
    }
  }

  /**
   * Return the entity tag of the body, as specified by the {@code ETag} header.
   */
  @Nullable
  public String getETag() {
    return getFirst(ETAG);
  }

  /**
   * Set the duration after which the message is no longer valid, as specified by
   * the {@code Expires} header.
   */
  public void setExpires(ZonedDateTime expires) {
    setZonedDateTime(EXPIRES, expires);
  }

  /**
   * Set the date and time at which the message is no longer valid, as specified
   * by the {@code Expires} header.
   */
  public void setExpires(Instant expires) {
    setInstant(EXPIRES, expires);
  }

  /**
   * Set the date and time at which the message is no longer valid, as specified
   * by the {@code Expires} header.
   * <p>
   * The date should be specified as the number of milliseconds since January 1,
   * 1970 GMT.
   */
  public void setExpires(long expires) {
    setDate(EXPIRES, expires);
  }

  /**
   * Return the date and time at which the message is no longer valid, as
   * specified by the {@code Expires} header.
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT.
   * Returns -1 when the date is unknown.
   *
   * @see #getFirstZonedDateTime(String)
   */
  public long getExpires() {
    return getFirstDate(EXPIRES, false);
  }

  /**
   * Set the (new) value of the {@code Host} header.
   * <p>
   * If the given {@linkplain InetSocketAddress#getPort() port} is {@code 0}, the
   * host header will only contain the
   * {@linkplain InetSocketAddress#getHostString() host name}.
   */
  public void setHost(InetSocketAddress host) {
    if (host != null) {
      String value = host.getHostString();
      int port = host.getPort();
      if (port != 0) {
        value = value + ':' + port;
      }
      set(HOST, value);
    }
    else {
      remove(HOST);
    }
  }

  /**
   * Return the value of the {@code Host} header, if available.
   * <p>
   * If the header value does not contain a port, the
   * {@linkplain InetSocketAddress#getPort() port} in the returned address will be
   * {@code 0}.
   */
  @Nullable
  public InetSocketAddress getHost() {
    String value = getFirst(HOST);
    if (value == null) {
      return null;
    }

    String host = null;
    int port = 0;
    int separator = StringUtils.matchesFirst(value, '[')
                    ? value.indexOf(':', value.indexOf(']'))
                    : value.lastIndexOf(':');
    if (separator != -1) {
      host = value.substring(0, separator);
      String portString = value.substring(separator + 1);
      try {
        port = Integer.parseInt(portString);
      }
      catch (NumberFormatException ex) {
        // ignore
      }
    }

    if (host == null) {
      host = value;
    }
    return InetSocketAddress.createUnresolved(host, port);
  }

  /**
   * Set the (new) value of the {@code If-Match} header.
   */
  public void setIfMatch(String ifMatch) {
    set(IF_MATCH, ifMatch);
  }

  /**
   * Set the (new) value of the {@code If-Match} header.
   */
  public void setIfMatch(Collection<String> ifMatchList) {
    set(IF_MATCH, toCommaDelimitedString(ifMatchList));
  }

  /**
   * Return the value of the {@code If-Match} header.
   */
  public List<String> getIfMatch() {
    return getETagValuesAsList(IF_MATCH);
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setIfModifiedSince(ZonedDateTime ifModifiedSince) {
    setZonedDateTime(IF_MODIFIED_SINCE, ifModifiedSince.withZoneSameInstant(GMT));
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setIfModifiedSince(Instant ifModifiedSince) {
    setInstant(IF_MODIFIED_SINCE, ifModifiedSince);
  }

  /**
   * Set the (new) value of the {@code If-Modified-Since} header.
   * <p>
   * The date should be specified as the number of milliseconds since January 1,
   * 1970 GMT.
   */
  public void setIfModifiedSince(long ifModifiedSince) {
    setDate(IF_MODIFIED_SINCE, ifModifiedSince);
  }

  /**
   * Return the value of the {@code If-Modified-Since} header.
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT.
   * Returns -1 when the date is unknown.
   *
   * @see #getFirstZonedDateTime(String)
   */
  public long getIfModifiedSince() {
    return getFirstDate(IF_MODIFIED_SINCE, false);
  }

  /**
   * Set the (new) value of the {@code If-None-Match} header.
   */
  public void setIfNoneMatch(String ifNoneMatch) {
    set(IF_NONE_MATCH, ifNoneMatch);
  }

  /**
   * Set the (new) values of the {@code If-None-Match} header.
   */
  public void setIfNoneMatch(Collection<String> ifNoneMatchList) {
    set(IF_NONE_MATCH, toCommaDelimitedString(ifNoneMatchList));
  }

  /**
   * Return the value of the {@code If-None-Match} header.
   */
  public List<String> getIfNoneMatch() {
    return getETagValuesAsList(IF_NONE_MATCH);
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setIfUnmodifiedSince(ZonedDateTime ifUnmodifiedSince) {
    setZonedDateTime(IF_UNMODIFIED_SINCE, ifUnmodifiedSince.withZoneSameInstant(GMT));
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setIfUnmodifiedSince(Instant ifUnmodifiedSince) {
    setInstant(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
  }

  /**
   * Set the (new) value of the {@code If-Unmodified-Since} header.
   * <p>
   * The date should be specified as the number of milliseconds since January 1,
   * 1970 GMT.
   */
  public void setIfUnmodifiedSince(long ifUnmodifiedSince) {
    setDate(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
  }

  /**
   * Return the value of the {@code If-Unmodified-Since} header.
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT.
   * Returns -1 when the date is unknown.
   *
   * @see #getFirstZonedDateTime(String)
   */
  public long getIfUnmodifiedSince() {
    return getFirstDate(IF_UNMODIFIED_SINCE, false);
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setLastModified(ZonedDateTime lastModified) {
    setZonedDateTime(LAST_MODIFIED, lastModified.withZoneSameInstant(GMT));
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setLastModified(Instant lastModified) {
    setInstant(LAST_MODIFIED, lastModified);
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   * <p>
   * The date should be specified as the number of milliseconds since January 1,
   * 1970 GMT.
   */
  public void setLastModified(long lastModified) {
    setDate(LAST_MODIFIED, lastModified);
  }

  /**
   * Return the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT.
   * Returns -1 when the date is unknown.
   *
   * @see #getFirstZonedDateTime(String)
   */
  public long getLastModified() {
    return getFirstDate(LAST_MODIFIED, false);
  }

  /**
   * Set the (new) location of a resource, as specified by the {@code Location}
   * header.
   */
  public void setLocation(URI location) {
    setOrRemove(LOCATION, (location != null ? location.toASCIIString() : null));
  }

  /**
   * Return the (new) location of a resource as specified by the {@code Location}
   * header.
   * <p>
   * Returns {@code null} when the location is unknown.
   */
  @Nullable
  public URI getLocation() {
    String value = getFirst(LOCATION);
    return (value != null ? URI.create(value) : null);
  }

  /**
   * Set the (new) value of the {@code Origin} header.
   */
  public void setOrigin(String origin) {
    setOrRemove(ORIGIN, origin);
  }

  /**
   * Return the value of the {@code Origin} header.
   */
  @Nullable
  public String getOrigin() {
    return getFirst(ORIGIN);
  }

  /**
   * Set the (new) value of the {@code Pragma} header.
   */
  public void setPragma(String pragma) {
    setOrRemove(PRAGMA, pragma);
  }

  /**
   * Return the value of the {@code Pragma} header.
   */
  @Nullable
  public String getPragma() {
    return getFirst(PRAGMA);
  }

  /**
   * Sets the (new) value of the {@code Range} header.
   */
  public void setRange(Collection<HttpRange> ranges) {
    String value = HttpRange.toString(ranges);
    set(RANGE, value);
  }

  /**
   * Return the value of the {@code Range} header.
   * <p>Returns an empty list when the range is unknown.
   */
  public List<HttpRange> getRange() {
    String value = getFirst(RANGE);
    return HttpRange.parseRanges(value);
  }

  /**
   * Set the (new) value of the {@code Upgrade} header.
   */
  public void setUpgrade(String upgrade) {
    setOrRemove(UPGRADE, upgrade);
  }

  /**
   * Return the value of the {@code Upgrade} header.
   */
  @Nullable
  public String getUpgrade() {
    return getFirst(UPGRADE);
  }

  /**
   * Set the request header names (e.g. "Accept-Language") for which the response
   * is subject to content negotiation and variances based on the value of those
   * request headers.
   *
   * @param requestHeaders the request header names
   */
  public void setVary(Collection<String> requestHeaders) {
    set(VARY, StringUtils.collectionToString(requestHeaders, ", "));
  }

  /**
   * Set the request header names (e.g. "Accept-Language") for which the response
   * is subject to content negotiation and variances based on the value of those
   * request headers.
   *
   * @param requestHeaders the request header names
   */
  public void setVary(String... requestHeaders) {
    set(VARY, StringUtils.arrayToString(requestHeaders, ", "));
  }

  /**
   * Return the request header names subject to content negotiation.
   */
  public List<String> getVary() {
    return getValuesAsList(VARY);
  }

  /**
   * Set the given date under the given header name after formatting it as a
   * string using the RFC-1123 date-time formatter. The equivalent of
   * {@link #set(String, String)} but for date headers.
   */
  public void setZonedDateTime(String headerName, ZonedDateTime date) {
    set(headerName, DATE_FORMATTER.format(date));
  }

  /**
   * Set the given date under the given header name after formatting it as a
   * string using the RFC-1123 date-time formatter. The equivalent of
   * {@link #set(String, String)} but for date headers.
   */
  public void setInstant(String headerName, Instant date) {
    setZonedDateTime(headerName, ZonedDateTime.ofInstant(date, GMT));
  }

  /**
   * Set the given date under the given header name after formatting it as a
   * string using the RFC-1123 date-time formatter. The equivalent of
   * {@link #set(String, String)} but for date headers.
   *
   * @see #setZonedDateTime(String, ZonedDateTime)
   */
  public void setDate(String headerName, long date) {
    setInstant(headerName, Instant.ofEpochMilli(date));
  }

  /**
   * Parse the first header value for the given header name as a date, return -1
   * if there is no value, or raise {@link IllegalArgumentException} if the value
   * cannot be parsed as a date.
   *
   * @param headerName the header name
   * @return the parsed date header, or -1 if none
   * @see #getFirstZonedDateTime(String)
   */
  public long getFirstDate(String headerName) {
    return getFirstDate(headerName, true);
  }

  /**
   * Parse the first header value for the given header name as a date, return -1
   * if there is no value or also in case of an invalid value (if
   * {@code rejectInvalid=false}), or raise {@link IllegalArgumentException} if
   * the value cannot be parsed as a date.
   *
   * @param headerName the header name
   * @param rejectInvalid whether to reject invalid values with an
   * {@link IllegalArgumentException} ({@code true}) or rather return
   * -1 in that case ({@code false})
   * @return the parsed date header, or -1 if none (or invalid)
   * @see #getFirstZonedDateTime(String, boolean)
   */
  public long getFirstDate(String headerName, boolean rejectInvalid) {
    ZonedDateTime zonedDateTime = getFirstZonedDateTime(headerName, rejectInvalid);
    return (zonedDateTime != null ? zonedDateTime.toInstant().toEpochMilli() : -1);
  }

  /**
   * Parse the first header value for the given header name as a date, return
   * {@code null} if there is no value, or raise {@link IllegalArgumentException}
   * if the value cannot be parsed as a date.
   *
   * @param headerName the header name
   * @return the parsed date header, or {@code null} if none
   */
  @Nullable
  public ZonedDateTime getFirstZonedDateTime(String headerName) {
    return getFirstZonedDateTime(headerName, true);
  }

  /**
   * Parse the first header value for the given header name as a date, return
   * {@code null} if there is no value or also in case of an invalid value (if
   * {@code rejectInvalid=false}), or raise {@link IllegalArgumentException} if
   * the value cannot be parsed as a date.
   *
   * @param headerName the header name
   * @param rejectInvalid whether to reject invalid values with an
   * {@link IllegalArgumentException} ({@code true}) or rather return
   * {@code null} in that case ({@code false})
   * @return the parsed date header, or {@code null} if none (or invalid)
   */
  @Nullable
  public ZonedDateTime getFirstZonedDateTime(String headerName, boolean rejectInvalid) {
    String headerValue = getFirst(headerName);
    if (headerValue == null) {
      // No header value sent at all
      return null;
    }
    if (headerValue.length() >= 3) {
      // Short "0" or "-1" like values are never valid HTTP date headers...
      // Let's only bother with DateTimeFormatter parsing for long enough values.

      // See https://stackoverflow.com/questions/12626699/if-modified-since-http-header-passed-by-ie9-includes-length
      int parametersIndex = headerValue.indexOf(';');
      if (parametersIndex != -1) {
        headerValue = headerValue.substring(0, parametersIndex);
      }

      for (DateTimeFormatter dateFormatter : DATE_PARSERS) {
        try {
          return ZonedDateTime.parse(headerValue, dateFormatter);
        }
        catch (DateTimeParseException ex) {
          // ignore
        }
      }
    }
    if (rejectInvalid) {
      throw new IllegalArgumentException(
              "Cannot parse date value \"" + headerValue + "\" for \"" + headerName + "\" header");
    }
    return null;
  }

  /**
   * Return all values of a given header name, even if this header is set multiple
   * times.
   *
   * @param headerName the header name
   * @return all associated values
   */
  public List<String> getValuesAsList(String headerName) {
    List<String> values = get(headerName);
    if (values != null) {
      ArrayList<String> result = new ArrayList<>();
      for (String value : values) {
        if (value != null) {
          Collections.addAll(result, StringUtils.tokenizeToStringArray(value, ","));
        }
      }
      return result;
    }
    return Collections.emptyList();
  }

  /**
   * Remove the well-known {@code "Content-*"} HTTP headers.
   * <p>
   * Such headers should be cleared from the response if the intended body can't
   * be written due to errors.
   */
  public void clearContentHeaders() {
    remove(CONTENT_TYPE);
    remove(CONTENT_RANGE);
    remove(CONTENT_LENGTH);
    remove(CONTENT_LANGUAGE);
    remove(CONTENT_LOCATION);
    remove(CONTENT_ENCODING);
    remove(CONTENT_DISPOSITION);
  }

  /**
   * Retrieve a combined result from the field values of the ETag header.
   *
   * @param headerName the header name
   * @return the combined result
   */
  public List<String> getETagValuesAsList(String headerName) {
    List<String> values = get(headerName);
    if (values != null) {
      ArrayList<String> result = new ArrayList<>();
      for (String value : values) {
        if (value != null) {
          Matcher matcher = ETAG_HEADER_VALUE_PATTERN.matcher(value);
          while (matcher.find()) {
            if ("*".equals(matcher.group())) {
              result.add(matcher.group());
            }
            else {
              result.add(matcher.group(1));
            }
          }
          if (result.isEmpty()) {
            throw new IllegalArgumentException("Could not parse header '" + headerName + "' with value '" + value + "'");
          }
        }
      }
      return result;
    }
    return Collections.emptyList();
  }

  /**
   * Retrieve a combined result from the field values of multi-valued headers.
   *
   * @param headerName the header name
   * @return the combined result
   */
  @Nullable
  public String getFieldValues(String headerName) {
    List<String> headerValues = get(headerName);
    return (headerValues != null ? toCommaDelimitedString(headerValues) : null);
  }

  /**
   * Turn the given list of header values into a comma-delimited result.
   *
   * @param headerValues the list of header values
   * @return a combined result with comma delimitation
   */
  protected String toCommaDelimitedString(Collection<?> headerValues) {
    StringJoiner joiner = new StringJoiner(", ");
    for (Object val : headerValues) {
      if (val != null) {
        joiner.add(val.toString());
      }
    }
    return joiner.toString();
  }

  /**
   * Set the given header value, or remove the header if {@code null}.
   *
   * @param headerName the header name
   * @param headerValue the header value, or {@code null} for none
   */
  public void setOrRemove(String headerName, @Nullable String headerValue) {
    if (headerValue != null) {
      set(headerName, headerValue);
    }
    else {
      remove(headerName);
    }
  }

  // abstract for subclasses

  /**
   * Return the first header value for the given header name, if any.
   *
   * @param headerName the header name
   * @return the first header value, or {@code null} if none
   */
  @Nullable
  @Override
  public abstract String getFirst(String headerName);

  /**
   * Add the given, single header value under the given name.
   *
   * @param headerName the header name
   * @param headerValue the header value
   * @throws UnsupportedOperationException if adding headers is not supported
   * @see #addAll(String, List)
   * @see #set(String, String)
   */
  @Override
  public abstract void add(String headerName, String headerValue);

  @Override
  public void addAll(String key, List<? extends String> values) {
    for (String value : values) {
      add(key, value);
    }
  }

  @Override
  public void addAll(String key, Enumeration<? extends String> values) {
    if (values.hasMoreElements()) {
      add(key, values.nextElement());
    }
  }

  @Override
  public void addAll(MultiValueMap<String, String> values) {
    values.forEach(this::addAll);
  }

  @Override
  public abstract void set(String headerName, @Nullable String headerValue);

  @Override
  public void setAll(Map<String, String> values) {
    values.forEach(this::set);
  }

  @Nullable
  @Override
  public abstract List<String> get(Object headerName);

  @Nullable
  @Override
  public abstract List<String> remove(Object headerName);

  @Override
  public String toString() {
    return formatHeaders(this);
  }

  // static

  /**
   * Helps to format HTTP header values, as HTTP header values themselves can
   * contain comma-separated values, can become confusing with regular
   * {@link Map} formatting that also uses commas between entries.
   *
   * @param headers the headers to format
   * @return the headers to a String
   */
  public static String formatHeaders(MultiValueMap<String, String> headers) {
    return headers.entrySet().stream()
            .map(entry -> {
              List<String> values = entry.getValue();
              return entry.getKey() + ":"
                      + (values.size() == 1
                         ? "\"" + values.get(0) + "\""
                         : values.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
            })
            .collect(Collectors.joining(", ", "[", "]"));
  }

  /**
   * Encode the given username and password into Basic Authentication credentials.
   * <p>The encoded credentials returned by this method can be supplied to
   * {@link #setBasicAuth(String)} to set the Basic Authentication header.
   *
   * @param username the username
   * @param password the password
   * @param charset the charset to use to convert the credentials into an octet
   * sequence. Defaults to {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}.
   * @throws IllegalArgumentException if {@code username} or {@code password}
   * contains characters that cannot be encoded to the given charset
   * @see #setBasicAuth(String)
   * @see #setBasicAuth(String, String)
   * @see #setBasicAuth(String, String, Charset)
   * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
   */
  public static String encodeBasicAuth(String username, String password, Charset charset) {
    Assert.notNull(username, "Username must not be null");
    Assert.doesNotContain(username, ":", "Username must not contain a colon");
    Assert.notNull(password, "Password must not be null");
    if (charset == null) {
      charset = StandardCharsets.ISO_8859_1;
    }

    CharsetEncoder encoder = charset.newEncoder();
    if (!encoder.canEncode(username) || !encoder.canEncode(password)) {
      throw new IllegalArgumentException(
              "Username or password contains characters that cannot be encoded to " + charset.displayName());
    }

    String credentialsString = username + ":" + password;
    byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(charset));
    return new String(encodedBytes, charset);
  }

  public static DefaultHttpHeaders create() {
    return new DefaultHttpHeaders();
  }

  /**
   * Construct a new {@code HttpHeaders} instance backed by an existing map.
   * <p>This constructor is available as an optimization for adapting to existing
   * headers map structures, primarily for internal use within the framework.
   *
   * @param headers the original map
   * @return the adapted multi-value map (wrapping the original map)
   * @since 4.0
   */
  public static DefaultHttpHeaders from(MultiValueMap<String, String> headers) {
    return new DefaultHttpHeaders(headers);
  }

  /**
   * Apply a read-only {@code HttpHeaders} wrapper around the given headers, if necessary.
   * <p>Also caches the parsed representations of the "Accept" and "Content-Type" headers.
   *
   * @param headers the headers to expose
   * @return a read-only variant of the headers, or the original headers as-is
   * (in case it happens to be a read-only {@code HttpHeaders} instance already)
   * @since 4.0
   */
  public static HttpHeaders readOnlyHttpHeaders(MultiValueMap<String, String> headers) {
    return (headers instanceof HttpHeaders ?
            readOnlyHttpHeaders((HttpHeaders) headers) : new ReadOnlyHttpHeaders(headers));
  }

  /**
   * Apply a read-only {@code HttpHeaders} wrapper around the given headers, if necessary.
   * <p>Also caches the parsed representations of the "Accept" and "Content-Type" headers.
   *
   * @param headers the headers to expose
   * @return a read-only variant of the headers, or the original headers as-is
   * @since 4.0
   */
  public static HttpHeaders readOnlyHttpHeaders(HttpHeaders headers) {
    Assert.notNull(headers, "HttpHeaders must not be null");
    if (headers instanceof ReadOnlyHttpHeaders) {
      return headers;
    }
    if (headers instanceof DefaultHttpHeaders defaults) {
      return new ReadOnlyHttpHeaders(defaults.headers);
    }
    return new ReadOnlyHttpHeaders(headers);
  }

  /**
   * Remove any read-only wrapper that may have been previously applied around
   * the given headers via {@link #readOnlyHttpHeaders(HttpHeaders)}.
   *
   * @param headers the headers to expose
   * @return a writable variant of the headers, or the original headers as-is
   * @since 4.0
   */
  public static HttpHeaders writableHttpHeaders(HttpHeaders headers) {
    Assert.notNull(headers, "HttpHeaders must not be null");
    if (headers instanceof ReadOnlyHttpHeaders readOnly) {
      return new DefaultHttpHeaders(readOnly.headers);
    }
    return headers;
  }

  // Package-private: used in ResponseCookie
  static String formatDate(long date) {
    Instant instant = Instant.ofEpochMilli(date);
    ZonedDateTime time = ZonedDateTime.ofInstant(instant, GMT);
    return DATE_FORMATTER.format(time);
  }

}
