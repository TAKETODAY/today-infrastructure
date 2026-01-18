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

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.StringUtils;

/**
 * Default implementation of {@link PathContainer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DefaultPathContainer extends PathContainer {

  public static final PathContainer EMPTY_PATH = new DefaultPathContainer("", Collections.emptyList());

  private static final HashMap<Character, DefaultSeparator> SEPARATORS = new HashMap<>(2);

  static {
    SEPARATORS.put('/', new DefaultSeparator('/', "%2F"));
    SEPARATORS.put('.', new DefaultSeparator('.', "%2E"));
  }

  private final String path;
  private final List<Element> elements;

  private DefaultPathContainer(String path, List<Element> elements) {
    this.path = path;
    this.elements = Collections.unmodifiableList(elements);
  }

  @Override
  public String value() {
    return this.path;
  }

  @Override
  public List<Element> elements() {
    return this.elements;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PathContainer)) {
      return false;
    }
    return value().equals(((PathContainer) other).value());
  }

  @Override
  public int hashCode() {
    return this.path.hashCode();
  }

  @Override
  public String toString() {
    return value();
  }

  static PathContainer createFromUrlPath(String path, Options options) {
    if (path.isEmpty()) {
      return EMPTY_PATH;
    }
    char separator = options.separator();
    DefaultSeparator separatorElement = SEPARATORS.get(separator);
    if (separatorElement == null) {
      throw new IllegalArgumentException("Unexpected separator: '%s'".formatted(separator));
    }
    ArrayList<Element> elements = new ArrayList<>();
    int begin;
    if (path.charAt(0) == separator) {
      begin = 1;
      elements.add(separatorElement);
    }
    else {
      begin = 0;
    }
    while (begin < path.length()) {
      int end = path.indexOf(separator, begin);
      String segment = (end != -1 ? path.substring(begin, end) : path.substring(begin));
      if (!segment.isEmpty()) {
        elements.add(options.shouldDecodeAndParseSegments()
                ? decodeAndParsePathSegment(segment)
                : DefaultPathSegment.from(segment, separatorElement));
      }
      if (end == -1) {
        break;
      }
      elements.add(separatorElement);
      begin = end + 1;
    }
    return new DefaultPathContainer(path, elements);
  }

  private static PathSegment decodeAndParsePathSegment(String segment) {
    int index = segment.indexOf(';');
    if (index == -1) {
      String valueToMatch = uriDecode(segment);
      return DefaultPathSegment.from(segment, valueToMatch);
    }
    else {
      String valueToMatch = uriDecode(segment.substring(0, index));
      String pathParameterContent = segment.substring(index);
      MultiValueMap<String, String> parameters = parsePathParams(pathParameterContent);
      return DefaultPathSegment.from(segment, valueToMatch, parameters);
    }
  }

  private static String uriDecode(String source) {
    try {
      return StringUtils.uriDecode(source, StandardCharsets.UTF_8);
    }
    catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("Invalid encoded sequence")) {
        return source;
      }
      throw e;
    }
  }

  private static MultiValueMap<String, String> parsePathParams(String input) {
    LinkedMultiValueMap<String, String> result = MultiValueMap.forLinkedHashMap();
    int begin = 1;
    while (begin < input.length()) {
      int end = input.indexOf(';', begin);
      String param = (end != -1 ? input.substring(begin, end) : input.substring(begin));
      parsePathParamValues(param, result);
      if (end == -1) {
        break;
      }
      begin = end + 1;
    }
    return result;
  }

  private static void parsePathParamValues(String input, MultiValueMap<String, String> output) {
    if (StringUtils.hasText(input)) {
      int index = input.indexOf('=');
      if (index != -1) {
        String name = input.substring(0, index);
        name = uriDecode(name);
        if (StringUtils.hasText(name)) {
          String value = input.substring(index + 1);
          for (String v : StringUtils.commaDelimitedListToStringArray(value)) {
            output.add(name, uriDecode(v));
          }
        }
      }
      else {
        String name = uriDecode(input);
        if (StringUtils.hasText(name)) {
          output.add(input, "");
        }
      }
    }
  }

  static PathContainer subPath(PathContainer container, int fromIndex, int toIndex) {
    List<Element> elements = container.elements();
    if (fromIndex == 0 && toIndex == elements.size()) {
      return container;
    }
    if (fromIndex == toIndex) {
      return EMPTY_PATH;
    }

    if (fromIndex < 0 || fromIndex >= elements.size()) {
      throw new IllegalArgumentException("Invalid fromIndex: " + fromIndex);
    }

    if (toIndex < 0 || toIndex > elements.size()) {
      throw new IllegalArgumentException("Invalid toIndex: " + toIndex);
    }

    if (fromIndex >= toIndex) {
      throw new IllegalArgumentException("fromIndex: %d should be < toIndex %d".formatted(fromIndex, toIndex));
    }

    List<Element> subList = elements.subList(fromIndex, toIndex);

    StringBuilder pathBuilder = new StringBuilder();
    for (Element element : subList) {
      pathBuilder.append(element.value());
    }

    String path = pathBuilder.toString();
    return new DefaultPathContainer(path, subList);
  }

  private static class DefaultSeparator implements Separator {

    private final String separator;
    private final String encodedSequence;

    DefaultSeparator(char separator, String encodedSequence) {
      this.separator = String.valueOf(separator);
      this.encodedSequence = encodedSequence;
    }

    @Override
    public String value() {
      return this.separator;
    }

    public String encodedSequence() {
      return this.encodedSequence;
    }
  }

  private static final class DefaultPathSegment implements PathSegment {

    private final String value;
    private final String valueToMatch;

    private final MultiValueMap<String, String> parameters;

    /**
     * Factory for segments without decoding and parsing.
     */
    static DefaultPathSegment from(String value, DefaultSeparator separator) {
      String valueToMatch = value.contains(separator.encodedSequence())
              ? value.replaceAll(separator.encodedSequence(), separator.value())
              : value;
      return from(value, valueToMatch);
    }

    /**
     * Factory for decoded and parsed segments.
     */
    static DefaultPathSegment from(String value, String valueToMatch) {
      return new DefaultPathSegment(value, valueToMatch, MultiValueMap.empty());
    }

    /**
     * Factory for decoded and parsed segments.
     */
    static DefaultPathSegment from(String value, String valueToMatch, MultiValueMap<String, String> params) {
      return new DefaultPathSegment(value, valueToMatch, params.asReadOnly());
    }

    private DefaultPathSegment(String value, String valueToMatch, MultiValueMap<String, String> params) {
      this.value = value;
      this.valueToMatch = valueToMatch;
      this.parameters = params;
    }

    @Override
    public String value() {
      return this.value;
    }

    @Override
    public String valueToMatch() {
      return this.valueToMatch;
    }

    @Override
    public char[] valueToMatchAsChars() {
      return this.valueToMatch.toCharArray();
    }

    @Override
    public MultiValueMap<String, String> parameters() {
      return this.parameters;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof PathSegment)) {
        return false;
      }
      return value().equals(((PathSegment) other).value());
    }

    @Override
    public int hashCode() {
      return this.value.hashCode();
    }

    @Override
    public String toString() {
      return "[value='%s']".formatted(this.value);
    }
  }

}

