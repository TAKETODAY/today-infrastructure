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

package cn.taketoday.web.socket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/5/4 19:54
 * @since 3.0.1
 */
public abstract class UpgradeUtils {

  private static final byte[] WS_ACCEPT =
          "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(
                  StandardCharsets.ISO_8859_1);

  /**
   * Checks to see if this is an HTTP request that includes a valid upgrade
   * request to web socket.
   * <p>
   * Note: RFC 2616 does not limit HTTP upgrade to GET requests but the Java
   * WebSocket spec 1.0, section 8.2 implies such a limitation and RFC
   * 6455 section 4.1 requires that a WebSocket Upgrade uses GET.
   *
   * @param context The request to check if it is an HTTP upgrade request for
   * a WebSocket connection
   * @return <code>true</code> if the request includes an HTTP Upgrade request
   * for the WebSocket protocol, otherwise <code>false</code>
   */
  public static boolean isWebSocketUpgradeRequest(RequestContext context) {
    return "GET".equals(context.getMethodValue()) && headerContainsToken(context, HttpHeaders.UPGRADE, "websocket");
  }

  /*
   * This only works for tokens. Quoted strings need more sophisticated
   * parsing.
   */
  static boolean headerContainsToken(List<String> headers, String target) {
    if (headers != null) {
      for (final String header : headers) {
        final String[] tokens = StringUtils.split(header);
        for (String token : tokens) {
          if (target.equalsIgnoreCase(token.trim())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  static boolean headerContainsToken(RequestContext context,
                                     String headerName, String target) {
    return headerContainsToken(context.requestHeaders(), headerName, target);
  }

  static boolean headerContainsToken(HttpHeaders headers,
                                     String headerName, String target) {
    final List<String> value = headers.get(headerName);
    return headerContainsToken(value, target);
  }

  /*
   * This only works for tokens. Quoted strings need more sophisticated
   * parsing.
   */
  static List<String> getTokensFromHeader(HttpHeaders requestHeaders, String headerName) {
    final ArrayList<String> result = new ArrayList<>();
    final List<String> headers = requestHeaders.get(headerName);
    if (headers != null) {
      for (final String header : headers) {
        String[] tokens = header.split(",");
        for (String token : tokens) {
          result.add(token.trim());
        }
      }
    }
    return result;
  }

  static String getWebSocketAccept(String key) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-1");
    }
    catch (NoSuchAlgorithmException e) {
      // Ignore. Impossible if init() has been successfully called first.
      throw new IllegalStateException("Digest algorithm unavailable", e);
    }
    md.update(key.getBytes(StandardCharsets.ISO_8859_1));
    md.update(WS_ACCEPT);
    return Base64.getEncoder().encodeToString(md.digest());
  }

  public static void parseExtensionHeader(List<WebSocketExtension> extensions, String header) {
    // The relevant ABNF for the Sec-WebSocket-Extensions is as follows:
    //      extension-list = 1#extension
    //      extension = extension-token *( ";" extension-param )
    //      extension-token = registered-token
    //      registered-token = token
    //      extension-param = token [ "=" (token | quoted-string) ]
    //             ; When using the quoted-string syntax variant, the value
    //             ; after quoted-string unescaping MUST conform to the
    //             ; 'token' ABNF.
    //
    // The limiting of parameter values to tokens or "quoted tokens" makes
    // the parsing of the header significantly simpler and allows a number
    // of short-cuts to be taken.

    // Step one, split the header into individual extensions using ',' as a
    // separator
    String[] unparsedExtensions = header.split(",");
    for (String unparsedExtension : unparsedExtensions) {
      // Step two, split the extension into the registered name and
      // parameter/value pairs using ';' as a separator
      final String[] unparsedParameters = unparsedExtension.split(";");
      final WebSocketExtension extension = new WebSocketExtension(unparsedParameters[0].trim());

      for (int i = 1; i < unparsedParameters.length; i++) {
        final String unparsedParameter = unparsedParameters[i];
        int equalsPos = unparsedParameter.indexOf('=');
        String name;
        String value;
        if (equalsPos == -1) {
          name = unparsedParameter.trim();
          value = null;
        }
        else {
          name = unparsedParameter.substring(0, equalsPos).trim();
          value = unparsedParameter.substring(equalsPos + 1).trim();
          int len = value.length();
          if (len > 1) {
            if (value.charAt(0) == '\"' && value.charAt(len - 1) == '\"') {
              value = value.substring(1, value.length() - 1);
            }
          }
        }
        // Make sure value doesn't contain any of the delimiters since
        // that would indicate something went wrong
        if (containsDelimiters(name) || containsDelimiters(value)) {
          // An illegal extension parameter was specified with name [{0}] and value [{1}]
          throw new IllegalArgumentException(
                  String.format("An illegal extension parameter was specified with name  [%s] and value [{%s]", name, value));
        }
        if (value != null &&
                (value.indexOf(',') > -1 || value.indexOf(';') > -1 ||
                        value.indexOf('\"') > -1 || value.indexOf('=') > -1)) {
          throw new IllegalArgumentException(value);
        }
        extension.addParameter(name, value);
      }
      extensions.add(extension);
    }
  }

  private static boolean containsDelimiters(String input) {
    if (StringUtils.isNotEmpty(input)) {
      for (char c : input.toCharArray()) {
        switch (c) {
          case ',':
          case ';':
          case '\"':
          case '=':
            return true;
          default:
            // NO_OP
        }
      }
    }
    return false;
  }

}
