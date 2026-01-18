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

package infra.http.server;

import java.util.List;

import infra.util.MultiValueMap;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class PathContainer {

  /**
   * The original path from which this instance was parsed.
   */
  public abstract String value();

  /**
   * The contained path elements, either {@link Separator} or {@link PathSegment}.
   */
  public abstract List<Element> elements();

  /**
   * Extract a sub-path from the given offset into the elements list.
   *
   * @param index the start element index (inclusive)
   * @return the sub-path
   */
  public PathContainer subPath(int index) {
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
  public PathContainer subPath(int startIndex, int endIndex) {
    return infra.http.server.DefaultPathContainer.subPath(this, startIndex, endIndex);
  }

  /**
   * Parse the path value into a sequence of {@code "/"} {@link Separator Separator}
   * and {@link PathSegment PathSegment} elements.
   *
   * @param path the encoded, raw path value to parse
   * @return the parsed path
   */
  public static PathContainer parsePath(String path) {
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
  public static PathContainer parsePath(String path, Options options) {
    return DefaultPathContainer.createFromUrlPath(path, options);
  }

  /**
   * empty path ''
   */
  public static PathContainer empty() {
    return DefaultPathContainer.EMPTY_PATH;
  }

  /**
   * A path element, either separator or path segment.
   */
  public interface Element {

    /**
     * The unmodified, original value of this element.
     */
    String value();
  }

  /**
   * Path separator element.
   */
  public interface Separator extends Element { }

  /**
   * Path segment element.
   */
  public interface PathSegment extends Element {

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
   *
   * @param separator separator
   * @param decodeAndParseSegments decodeAndParseSegments
   */
  public record Options(char separator, boolean decodeAndParseSegments) {

    /**
     * Options for HTTP URL paths.
     * <p>Separator '/' with URL decoding and parsing of path parameters.
     */
    public static final Options HTTP_PATH = Options.create('/', true);

    /**
     * Options for a message route.
     * <p>Separator '.' with neither URL decoding nor parsing of path parameters.
     * Escape sequences for the separator character in segment values are still
     * decoded.
     */
    public static final Options MESSAGE_ROUTE = Options.create('.', false);

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
