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

package cn.taketoday.http.server;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;

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
      throw new IllegalArgumentException("Unexpected separator: '" + separator + "'");
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
      throw new IllegalArgumentException("fromIndex: " + fromIndex + " should be < toIndex " + toIndex);
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
      return new DefaultPathSegment(value, valueToMatch, MultiValueMap.forUnmodifiable(params));
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
      return "[value='" + this.value + "']";
    }
  }

}

