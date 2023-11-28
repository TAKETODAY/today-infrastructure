/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.StringUtils;

/**
 * Represents a WebSocket extension as defined in the RFC 6455.
 * WebSocket extensions add protocol features to the WebSocket protocol. The extensions
 * used within a session are negotiated during the handshake phase as follows:
 * <ul>
 * <li>the client may ask for specific extensions in the HTTP handshake request</li>
 * <li>the server responds with the final list of extensions to use in the current session</li>
 * </ul>
 *
 * <p>WebSocket Extension HTTP headers may include parameters and follow
 * <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 section 3.2</a></p>
 *
 * <p>Note that the order of extensions in HTTP headers defines their order of execution,
 * e.g. extensions "foo, bar" will be executed as "bar(foo(message))".</p>
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author TODAY 2021/5/6 16:40
 * @see <a href="https://tools.ietf.org/html/rfc6455#section-9">WebSocket Protocol Extensions, RFC 6455 - Section 9</a>
 * @since 3.0.1
 */
public class WebSocketExtension {

  private final String name;
  private final Map<String, String> parameters;

  /**
   * Create a WebSocketExtension with the given name.
   *
   * @param name the name of the extension
   */
  public WebSocketExtension(String name) {
    this(name, null);
  }

  /**
   * Create a WebSocketExtension with the given name and parameters.
   *
   * @param name the name of the extension
   * @param parameters the parameters
   */
  public WebSocketExtension(String name, @Nullable Map<String, String> parameters) {
    Assert.hasLength(name, "Extension name must not be empty");
    this.name = name;
    if (CollectionUtils.isNotEmpty(parameters)) {
      var map = new LinkedCaseInsensitiveMap<String>(parameters.size(), Locale.ENGLISH);
      map.putAll(parameters);
      this.parameters = Collections.unmodifiableMap(map);
    }
    else {
      this.parameters = Collections.emptyMap();
    }
  }

  /**
   * Return the name of the extension (never {@code null} or empty).
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the parameters of the extension (never {@code null}).
   */
  public Map<String, String> getParameters() {
    return this.parameters;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || !WebSocketExtension.class.isAssignableFrom(other.getClass())) {
      return false;
    }
    WebSocketExtension otherExt = (WebSocketExtension) other;
    return (this.name.equals(otherExt.name) && this.parameters.equals(otherExt.parameters));
  }

  @Override
  public int hashCode() {
    return this.name.hashCode() * 31 + this.parameters.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append(this.name);
    this.parameters.forEach((key, value) -> str.append(';').append(key).append('=').append(value));
    return str.toString();
  }

  /**
   * Parse the given, comma-separated string into a list of {@code WebSocketExtension} objects.
   * <p>This method can be used to parse a "Sec-WebSocket-Extension" header.
   *
   * @param extensions the string to parse
   * @return the list of extensions
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static List<WebSocketExtension> parseExtensions(String extensions) {
    if (StringUtils.hasText(extensions)) {
      String[] tokens = StringUtils.tokenizeToStringArray(extensions, ",");
      List<WebSocketExtension> result = new ArrayList<>(tokens.length);
      for (String token : tokens) {
        result.add(parseExtension(token));
      }
      return result;
    }
    else {
      return Collections.emptyList();
    }
  }

  private static WebSocketExtension parseExtension(String extension) {
    if (extension.contains(",")) {
      throw new IllegalArgumentException("Expected single extension value: [" + extension + "]");
    }
    String[] parts = StringUtils.tokenizeToStringArray(extension, ";");
    String name = parts[0].trim();

    Map<String, String> parameters = null;
    if (parts.length > 1) {
      parameters = CollectionUtils.newLinkedHashMap(parts.length - 1);
      for (int i = 1; i < parts.length; i++) {
        String parameter = parts[i];
        int eqIndex = parameter.indexOf('=');
        if (eqIndex != -1) {
          String attribute = parameter.substring(0, eqIndex);
          String value = parameter.substring(eqIndex + 1);
          parameters.put(attribute, value);
        }
      }
    }

    return new WebSocketExtension(name, parameters);
  }

}
