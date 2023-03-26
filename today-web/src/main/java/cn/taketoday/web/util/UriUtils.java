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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;

/**
 * Utility methods for URI encoding and decoding based on RFC 3986.
 *
 * <p>There are two types of encode methods:
 * <ul>
 * <li>{@code "encodeXyz"} -- these encode a specific URI component (e.g. path,
 * query) by percent encoding illegal characters, which includes non-US-ASCII
 * characters, and also characters that are otherwise illegal within the given
 * URI component type, as defined in RFC 3986. The effect of this method, with
 * regards to encoding, is comparable to using the multi-argument constructor
 * of {@link URI}.
 * <li>{@code "encode"} and {@code "encodeUriVariables"} -- these can be used
 * to encode URI variable values by percent encoding all characters that are
 * either illegal, or have any reserved meaning, anywhere within a URI.
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 * @since 4.0
 */
public abstract class UriUtils {

  /**
   * Encode the given URI scheme with the given encoding.
   *
   * @param scheme the scheme to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded scheme
   */
  public static String encodeScheme(String scheme, String encoding) {
    return encode(scheme, encoding, HierarchicalUriComponents.Type.SCHEME);
  }

  /**
   * Encode the given URI scheme with the given encoding.
   *
   * @param scheme the scheme to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded scheme
   */
  public static String encodeScheme(String scheme, Charset charset) {
    return encode(scheme, charset, HierarchicalUriComponents.Type.SCHEME);
  }

  /**
   * Encode the given URI authority with the given encoding.
   *
   * @param authority the authority to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded authority
   */
  public static String encodeAuthority(String authority, String encoding) {
    return encode(authority, encoding, HierarchicalUriComponents.Type.AUTHORITY);
  }

  /**
   * Encode the given URI authority with the given encoding.
   *
   * @param authority the authority to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded authority
   */
  public static String encodeAuthority(String authority, Charset charset) {
    return encode(authority, charset, HierarchicalUriComponents.Type.AUTHORITY);
  }

  /**
   * Encode the given URI user info with the given encoding.
   *
   * @param userInfo the user info to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded user info
   */
  public static String encodeUserInfo(String userInfo, String encoding) {
    return encode(userInfo, encoding, HierarchicalUriComponents.Type.USER_INFO);
  }

  /**
   * Encode the given URI user info with the given encoding.
   *
   * @param userInfo the user info to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded user info
   */
  public static String encodeUserInfo(String userInfo, Charset charset) {
    return encode(userInfo, charset, HierarchicalUriComponents.Type.USER_INFO);
  }

  /**
   * Encode the given URI host with the given encoding.
   *
   * @param host the host to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded host
   */
  public static String encodeHost(String host, String encoding) {
    return encode(host, encoding, HierarchicalUriComponents.Type.HOST_IPV4);
  }

  /**
   * Encode the given URI host with the given encoding.
   *
   * @param host the host to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded host
   */
  public static String encodeHost(String host, Charset charset) {
    return encode(host, charset, HierarchicalUriComponents.Type.HOST_IPV4);
  }

  /**
   * Encode the given URI port with the given encoding.
   *
   * @param port the port to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded port
   */
  public static String encodePort(String port, String encoding) {
    return encode(port, encoding, HierarchicalUriComponents.Type.PORT);
  }

  /**
   * Encode the given URI port with the given encoding.
   *
   * @param port the port to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded port
   */
  public static String encodePort(String port, Charset charset) {
    return encode(port, charset, HierarchicalUriComponents.Type.PORT);
  }

  /**
   * Encode the given URI path with the given encoding.
   *
   * @param path the path to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded path
   */
  public static String encodePath(String path, String encoding) {
    return encode(path, encoding, HierarchicalUriComponents.Type.PATH);
  }

  /**
   * Encode the given URI path with the given encoding.
   *
   * @param path the path to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded path
   */
  public static String encodePath(String path, Charset charset) {
    return encode(path, charset, HierarchicalUriComponents.Type.PATH);
  }

  /**
   * Encode the given URI path segment with the given encoding.
   *
   * @param segment the segment to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded segment
   */
  public static String encodePathSegment(String segment, String encoding) {
    return encode(segment, encoding, HierarchicalUriComponents.Type.PATH_SEGMENT);
  }

  /**
   * Encode the given URI path segment with the given encoding.
   *
   * @param segment the segment to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded segment
   */
  public static String encodePathSegment(String segment, Charset charset) {
    return encode(segment, charset, HierarchicalUriComponents.Type.PATH_SEGMENT);
  }

  /**
   * Encode the given URI query with the given encoding.
   *
   * @param query the query to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded query
   */
  public static String encodeQuery(String query, String encoding) {
    return encode(query, encoding, HierarchicalUriComponents.Type.QUERY);
  }

  /**
   * Encode the given URI query with the given encoding.
   *
   * @param query the query to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded query
   */
  public static String encodeQuery(String query, Charset charset) {
    return encode(query, charset, HierarchicalUriComponents.Type.QUERY);
  }

  /**
   * Encode the given URI query parameter with the given encoding.
   *
   * @param queryParam the query parameter to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded query parameter
   */
  public static String encodeQueryParam(String queryParam, String encoding) {
    return encode(queryParam, encoding, HierarchicalUriComponents.Type.QUERY_PARAM);
  }

  /**
   * Encode the given URI query parameter with the given encoding.
   *
   * @param queryParam the query parameter to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded query parameter
   */
  public static String encodeQueryParam(String queryParam, Charset charset) {
    return encode(queryParam, charset, HierarchicalUriComponents.Type.QUERY_PARAM);
  }

  /**
   * Encode the query parameters from the given {@code MultiValueMap} with UTF-8.
   * <p>This can be used with {@link UriComponentsBuilder#queryParams(MultiValueMap)}
   * when building a URI from an already encoded template.
   * <pre class="code">{@code
   * MultiValueMap<String, String> params = new DefaultMultiValueMap<>(2);
   * // add to params...
   *
   * ServletUriComponentsBuilder.fromCurrentRequest()
   *         .queryParams(UriUtils.encodeQueryParams(params))
   *         .build(true)
   *         .toUriString();
   * }</pre>
   *
   * @param params the parameters to encode
   * @return a new {@code MultiValueMap} with the encoded names and values
   */
  public static MultiValueMap<String, String> encodeQueryParams(MultiValueMap<String, String> params) {
    Charset charset = StandardCharsets.UTF_8;
    MultiValueMap<String, String> result = MultiValueMap.fromLinkedHashMap(params.size());
    for (Map.Entry<String, List<String>> entry : params.entrySet()) {
      for (String value : entry.getValue()) {
        result.add(encodeQueryParam(entry.getKey(), charset), encodeQueryParam(value, charset));
      }
    }
    return result;
  }

  /**
   * Encode the given URI fragment with the given encoding.
   *
   * @param fragment the fragment to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded fragment
   */
  public static String encodeFragment(String fragment, String encoding) {
    return encode(fragment, encoding, HierarchicalUriComponents.Type.FRAGMENT);
  }

  /**
   * Encode the given URI fragment with the given encoding.
   *
   * @param fragment the fragment to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded fragment
   */
  public static String encodeFragment(String fragment, Charset charset) {
    return encode(fragment, charset, HierarchicalUriComponents.Type.FRAGMENT);
  }

  /**
   * Variant of {@link #encode(String, Charset)} with a String charset.
   *
   * @param source the String to be encoded
   * @param encoding the character encoding to encode to
   * @return the encoded String
   */
  public static String encode(String source, String encoding) {
    return encode(source, encoding, HierarchicalUriComponents.Type.URI);
  }

  /**
   * Encode all characters that are either illegal, or have any reserved
   * meaning, anywhere within a URI, as defined in
   * <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>.
   * This is useful to ensure that the given String will be preserved as-is
   * and will not have any o impact on the structure or meaning of the URI.
   *
   * @param source the String to be encoded
   * @param charset the character encoding to encode to
   * @return the encoded String
   */
  public static String encode(String source, Charset charset) {
    return encode(source, charset, HierarchicalUriComponents.Type.URI);
  }

  /**
   * Convenience method to apply {@link #encode(String, Charset)} to all
   * given URI variable values.
   *
   * @param uriVariables the URI variable values to be encoded
   * @return the encoded String
   */
  public static Map<String, String> encodeUriVariables(Map<String, ?> uriVariables) {
    Map<String, String> result = CollectionUtils.newLinkedHashMap(uriVariables.size());
    uriVariables.forEach((key, value) -> {
      String stringValue = (value != null ? value.toString() : "");
      result.put(key, encode(stringValue, StandardCharsets.UTF_8));
    });
    return result;
  }

  /**
   * Convenience method to apply {@link #encode(String, Charset)} to all
   * given URI variable values.
   *
   * @param uriVariables the URI variable values to be encoded
   * @return the encoded String
   */
  public static Object[] encodeUriVariables(Object... uriVariables) {
    return Arrays.stream(uriVariables)
            .map(value -> {
              String stringValue = (value != null ? value.toString() : "");
              return encode(stringValue, StandardCharsets.UTF_8);
            })
            .toArray();
  }

  private static String encode(String scheme, String encoding, HierarchicalUriComponents.Type type) {
    return HierarchicalUriComponents.encodeUriComponent(scheme, encoding, type);
  }

  private static String encode(String scheme, Charset charset, HierarchicalUriComponents.Type type) {
    return HierarchicalUriComponents.encodeUriComponent(scheme, charset, type);
  }

  /**
   * Decode the given encoded URI component.
   * <p>See {@link StringUtils#uriDecode(String, Charset)} for the decoding rules.
   *
   * @param source the encoded String
   * @param encoding the character encoding to use
   * @return the decoded value
   * @throws IllegalArgumentException when the given source contains invalid encoded sequences
   * @see StringUtils#uriDecode(String, Charset)
   * @see java.net.URLDecoder#decode(String, String)
   */
  public static String decode(String source, String encoding) {
    return StringUtils.uriDecode(source, Charset.forName(encoding));
  }

  /**
   * Decode the given encoded URI component.
   * <p>See {@link StringUtils#uriDecode(String, Charset)} for the decoding rules.
   *
   * @param source the encoded String
   * @param charset the character encoding to use
   * @return the decoded value
   * @throws IllegalArgumentException when the given source contains invalid encoded sequences
   * @see StringUtils#uriDecode(String, Charset)
   * @see java.net.URLDecoder#decode(String, String)
   */
  public static String decode(String source, Charset charset) {
    return StringUtils.uriDecode(source, charset);
  }

  /**
   * Extract the file extension from the given URI path.
   *
   * @param path the URI path (e.g. "/products/index.html")
   * @return the extracted file extension (e.g. "html")
   */
  @Nullable
  public static String extractFileExtension(String path) {
    int end = path.indexOf('?');
    int fragmentIndex = path.indexOf('#');
    if (fragmentIndex != -1 && (end == -1 || fragmentIndex < end)) {
      end = fragmentIndex;
    }
    if (end == -1) {
      end = path.length();
    }
    int begin = path.lastIndexOf('/', end) + 1;
    int paramIndex = path.indexOf(';', begin);
    end = (paramIndex != -1 && paramIndex < end ? paramIndex : end);
    int extIndex = path.lastIndexOf('.', end);
    if (extIndex != -1 && extIndex >= begin) {
      return path.substring(extIndex + 1, end);
    }
    return null;
  }

}
