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

package cn.taketoday.http.server;

import java.util.List;

import cn.taketoday.core.MultiValueMap;

/**
 * Structured representation of a URI path parsed via {@link #parsePath(String)}
 * into a sequence of {@link Separator} and {@link PathSegment} elements.
 *
 * <p>Each {@link PathSegment} exposes its content in decoded form and with path
 * parameters removed. This makes it safe to match one path segment at a time
 * without the risk of decoded reserved characters altering the structure of
 * the path.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface PathContainer {

  /**
   * The original path from which this instance was parsed.
   */
  String value();

  /**
   * The contained path elements, either {@link Separator} or {@link PathSegment}.
   */
  List<Element> elements();

  /**
   * Extract a sub-path from the given offset into the elements list.
   *
   * @param index the start element index (inclusive)
   * @return the sub-path
   */
  default PathContainer subPath(int index) {
    return subPath(index, elements().size());
  }

  /**
   * Extract a sub-path from the given start offset into the element list
   * (inclusive) and to the end offset (exclusive).
   *
   * @param startIndex the start element index (inclusive)
   * @param endIndex the end element index (exclusive)
   * @return the sub-path
   */
  default PathContainer subPath(int startIndex, int endIndex) {
    return DefaultPathContainer.subPath(this, startIndex, endIndex);
  }

  /**
   * Parse the path value into a sequence of {@code "/"} {@link Separator Separator}
   * and {@link PathSegment PathSegment} elements.
   *
   * @param path the encoded, raw path value to parse
   * @return the parsed path
   */
  static PathContainer parsePath(String path) {
    return DefaultPathContainer.createFromUrlPath(path, Options.HTTP_PATH);
  }

  /**
   * Parse the path value into a sequence of {@link Separator Separator} and
   * {@link PathSegment PathSegment} elements.
   *
   * @param path the encoded, raw path value to parse
   * @param options to customize parsing
   * @return the parsed path
   */
  static PathContainer parsePath(String path, Options options) {
    return DefaultPathContainer.createFromUrlPath(path, options);
  }

  /**
   * A path element, either separator or path segment.
   */
  interface Element {

    /**
     * The unmodified, original value of this element.
     */
    String value();
  }

  /**
   * Path separator element.
   */
  interface Separator extends Element { }

  /**
   * Path segment element.
   */
  interface PathSegment extends Element {

    /**
     * Return the path segment value, decoded and sanitized, for path matching.
     */
    String valueToMatch();

    /**
     * Expose {@link #valueToMatch()} as a character array.
     */
    char[] valueToMatchAsChars();

    /**
     * Path parameters associated with this path segment.
     *
     * @return an unmodifiable map containing the parameters
     */
    MultiValueMap<String, String> parameters();
  }

  /**
   * Options to customize parsing based on the type of input path.
   */
  record Options(char separator, boolean decodeAndParseSegments) {

    /**
     * Options for HTTP URL paths.
     * <p>Separator '/' with URL decoding and parsing of path parameters.
     */
    public final static Options HTTP_PATH = Options.create('/', true);

    /**
     * Options for a message route.
     * <p>Separator '.' with neither URL decoding nor parsing of path parameters.
     * Escape sequences for the separator character in segment values are still
     * decoded.
     */
    public final static Options MESSAGE_ROUTE = Options.create('.', false);

    public boolean shouldDecodeAndParseSegments() {
      return this.decodeAndParseSegments;
    }

    /**
     * Create an {@link Options} instance with the given settings.
     *
     * @param separator the separator for parsing the path into segments;
     * currently this must be slash or dot.
     * @param decodeAndParseSegments whether to URL decode path segment
     * values and parse path parameters. If set to false, only escape
     * sequences for the separator char are decoded.
     */
    public static Options create(char separator, boolean decodeAndParseSegments) {
      return new Options(separator, decodeAndParseSegments);
    }
  }

}
