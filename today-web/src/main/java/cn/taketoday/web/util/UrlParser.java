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

package cn.taketoday.web.util;

import java.net.IDN;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Implementation of the URL parser from the Living URL standard.
 *
 * <p>All comments in this class refer to parts of the
 * <a href="https://url.spec.whatwg.org/#url-parsing">parsing algorithm</a>.
 * This implementation differs from the one defined in the specification in
 * these areas:
 * <ul>
 * <li>Support for URI templates has been added, through the
 * {@link State#URL_TEMPLATE} state</li>
 * <li>Consequentially, the {@linkplain UrlRecord#port() URL port} has been
 * changed from an integer to a string,</li>
 * <li>To ensure that trailing slashes are significant, this implementation
 * prepends a '/' to each segment.</li>
 * </ul>
 * All of these modifications have been indicated through comments that start
 * with {@code EXTRA}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://url.spec.whatwg.org/#url-parsing">URL parsing</a>
 * @since 4.0
 */
final class UrlParser {

  private static final int EOF = -1;

  private static final int MAX_PORT = 65535;

  private static final Logger logger = LoggerFactory.getLogger(UrlParser.class);

  private final StringBuilder input;

  @Nullable
  private final UrlRecord base;

  @Nullable
  private Charset encoding;

  @Nullable
  private final Consumer<String> validationErrorHandler;

  private int pointer;

  private final StringBuilder buffer;

  @Nullable
  private State state;

  @Nullable
  private State previousState;

  @Nullable
  private State stateOverride;

  private boolean atSignSeen;

  private boolean passwordTokenSeen;

  private boolean insideBrackets;

  private UrlParser(String input, @Nullable UrlRecord base, @Nullable Charset encoding, @Nullable Consumer<String> validationErrorHandler) {
    this.input = new StringBuilder(input);
    this.base = base;
    this.encoding = encoding;
    this.validationErrorHandler = validationErrorHandler;
    this.buffer = new StringBuilder(this.input.length() / 2);
  }

  /**
   * Parse the given input into a URL record.
   *
   * @param input the scalar value string
   * @param base the optional base URL to resolve relative URLs against. If
   * {@code null}, relative URLs cannot be parsed.
   * @param encoding the optional encoding to use. If {@code null}, no
   * encoding is performed.
   * @param validationErrorHandler optional consumer for non-fatal URL
   * validation messages
   * @return a URL record, as defined in the
   * <a href="https://url.spec.whatwg.org/#concept-url">living URL
   * specification</a>
   * @throws InvalidUrlException if the {@code input} does not contain a
   * parsable URL
   */
  public static UrlRecord parse(String input, @Nullable UrlRecord base,
          @Nullable Charset encoding, @Nullable Consumer<String> validationErrorHandler) throws InvalidUrlException {

    Assert.notNull(input, "Input is required");

    UrlParser parser = new UrlParser(input, base, encoding, validationErrorHandler);
    return parser.basicUrlParser(null, null);
  }

  /**
   * The basic URL parser takes a scalar value string input, with an optional
   * null or base URL base (default null), an optional encoding
   * {@code encoding}
   * (default UTF-8), an optional URL {@code url}, and an optional state
   * override {@code state override}.
   */
  private UrlRecord basicUrlParser(@Nullable UrlRecord url, @Nullable State stateOverride) {
    // If url is not given:
    if (url == null) {
      // Set url to a new URL.
      url = new UrlRecord();
    }
    sanitizeInput();
    // Let state be state override if given, or scheme start state otherwise.
    this.state = stateOverride != null ? stateOverride : State.SCHEME_START;
    this.stateOverride = stateOverride;

    // Keep running the following state machine by switching on state.
    // If after a run pointer points to the EOF code point, go to the next step.
    // Otherwise, increase pointer by 1 and continue with the state machine.
    while (this.pointer <= this.input.length()) {
      int c;
      if (this.pointer < this.input.length()) {
        c = this.input.charAt(this.pointer);
      }
      else {
        c = EOF;
      }
      if (logger.isTraceEnabled()) {
        String cStr = c != EOF ? Character.toString(c) : "EOF";
        logger.trace("current: %s ptr: %d Buffer: %s State: %s".formatted(cStr, this.pointer, this.buffer, this.state));
      }
      this.state.handle(c, url, this);
      this.pointer++;
    }
    return url;
  }

  void sanitizeInput() {
    boolean strip = true;
    for (int i = 0; i < this.input.length(); i++) {
      char ch = this.input.charAt(i);
      if ((strip && (ch == ' ' || isC0Control(ch)))
              || (ch == '\t' || isNewline(ch))) {
        if (validate()) {
          // If input contains any leading (or trailing) C0 control or space, invalid-URL-unit validation error.
          // If input contains any ASCII tab or newline, invalid-URL-unit validation error.
          validationError("Code point \"%s\" is not a URL unit.".formatted(ch));
        }
        // Remove any leading (and trailing) C0 control or space from input.
        // Remove all ASCII tab or newline from input.
        this.input.deleteCharAt(i);
        i--;
      }
      else {
        strip = false;
      }
    }
    for (int i = this.input.length() - 1; i >= 0; i--) {
      char ch = this.input.charAt(i);
      if (ch == ' ' || isC0Control(ch)) {
        if (validate()) {
          // If input contains any (leading or) trailing C0 control or space, invalid-URL-unit validation error.
          validationError("Code point \"%s\" is not a URL unit.".formatted(ch));
        }
        // Remove any (leading and) trailing C0 control or space from input.
        this.input.deleteCharAt(i);
      }
      else {
        break;
      }
    }
  }

  private void setState(State newState) {
    if (logger.isDebugEnabled()) {
      String c;
      if (this.pointer < this.input.length()) {
        c = Character.toString(this.input.charAt(this.pointer));
      }
      else {
        c = "EOF";
      }
      logger.debug("Changing state from %s to %s (cur: %s prev: %s)".formatted(this.state, newState, c, this.previousState));
    }
    // EXTRA: we keep the previous state, to ensure that the parser can escape from malformed URI templates
    this.previousState = this.state;
    this.state = newState;
  }

  private static List<String> tokenize(String str, String delimiters) {
    StringTokenizer st = new StringTokenizer(str, delimiters);
    List<String> tokens = new ArrayList<>();
    while (st.hasMoreTokens()) {
      tokens.add(st.nextToken());
    }
    return tokens;
  }

  private static String domainToAscii(String domain, boolean beStrict) {
    // Let result be the result of running Unicode ToASCII (https://www.unicode.org/reports/tr46/#ToASCII) with domain_name set to domain, UseSTD3ASCIIRules set to beStrict, CheckHyphens set to false, CheckBidi set to true, CheckJoiners set to true, Transitional_Processing set to false, and VerifyDnsLength set to beStrict. [UTS46]
    int flag = 0;
    if (beStrict) {
      flag |= IDN.USE_STD3_ASCII_RULES;
    }
    // Implementation note: implementing Unicode ToASCII is beyond the scope of this parser, we use java.net.IDN.toASCII
    return IDN.toASCII(domain, flag);
  }

  private boolean validate() {
    return this.validationErrorHandler != null;
  }

  private void validationError(@Nullable String additionalInfo) {
    if (this.validationErrorHandler != null) {
      StringBuilder message = new StringBuilder("URL validation error for URL [");
      message.append(this.input);
      message.append("]@");
      message.append(this.pointer);
      if (additionalInfo != null) {
        message.append(". ");
        message.append(additionalInfo);
      }
      this.validationErrorHandler.accept(message.toString());
    }
  }

  private void failure(@Nullable String additionalInfo) {
    StringBuilder message = new StringBuilder("URL parsing failure for URL [");
    message.append(this.input);
    message.append("] @ ");
    message.append(this.pointer);
    if (additionalInfo != null) {
      message.append(". ");
      message.append(additionalInfo);
    }
    throw new InvalidUrlException(message.toString());
  }

  private static boolean isC0Control(int ch) {
    return ch >= 0 && ch <= 0x1F;
  }

  private static boolean isNewline(int ch) {
    return ch == '\r' || ch == '\n';
  }

  private static boolean isAsciiAlpha(int ch) {
    return (ch >= 'A' && ch <= 'Z') ||
            (ch >= 'a' && ch <= 'z');
  }

  private static boolean containsOnlyAsciiDigits(CharSequence string) {
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      if (!isAsciiDigit(ch)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isAsciiDigit(int ch) {
    return (ch >= '0' && ch <= '9');
  }

  private static boolean isAsciiAlphaNumeric(int ch) {
    return isAsciiAlpha(ch) || isAsciiDigit(ch);
  }

  private static boolean isAsciiHexDigit(int ch) {
    return isAsciiDigit(ch) ||
            (ch >= 'A' && ch <= 'F') ||
            (ch >= 'a' && ch <= 'f');
  }

  private static boolean isForbiddenDomain(int ch) {
    return isForbiddenHost(ch) || isC0Control(ch) || ch == '%' || ch == 0x7F;
  }

  private static boolean isForbiddenHost(int ch) {
    return ch == 0x00 || ch == '\t' || isNewline(ch) || ch == ' ' || ch == '#' || ch == '/' || ch == ':' ||
            ch == '<' || ch == '>' || ch == '?' || ch == '@' || ch == '[' || ch == '\\' || ch == ']' || ch == '^' ||
            ch == '|';
  }

  private static boolean isNonCharacter(int ch) {
    return (ch >= 0xFDD0 && ch <= 0xFDEF) || ch == 0xFFFE || ch == 0xFFFF || ch == 0x1FFFE || ch == 0x1FFFF ||
            ch == 0x2FFFE || ch == 0x2FFFF || ch == 0x3FFFE || ch == 0x3FFFF || ch == 0x4FFFE || ch == 0x4FFFF ||
            ch == 0x5FFFE || ch == 0x5FFFF || ch == 0x6FFFE || ch == 0x6FFFF || ch == 0x7FFFE || ch == 0x7FFFF ||
            ch == 0x8FFFE || ch == 0x8FFFF || ch == 0x9FFFE || ch == 0x9FFFF || ch == 0xAFFFE || ch == 0xAFFFF ||
            ch == 0xBFFFE || ch == 0xBFFFF || ch == 0xCFFFE || ch == 0xCFFFF || ch == 0xDFFFE || ch == 0xDFFFF ||
            ch == 0xEFFFE || ch == 0xEFFFF || ch == 0xFFFFE || ch == 0xFFFFF || ch == 0x10FFFE || ch == 0x10FFFF;
  }

  private static boolean isUrlCodePoint(int ch) {
    return isAsciiAlphaNumeric(ch) ||
            ch == '!' || ch == '$' || ch == '&' || ch == '\'' || ch == '(' || ch == ')' || ch == '*' || ch == '+'
            || ch == ',' || ch == '-' || ch == '.' || ch == '/' || ch == ':' || ch == ';' || ch == '=' || ch == '?'
            || ch == '@' || ch == '_' || ch == '~' ||
            (ch >= 0x00A0 && ch <= 0x10FFFD && !Character.isSurrogate((char) ch) && !isNonCharacter(ch));
  }

  private static boolean isSpecialScheme(String scheme) {
    return "ftp".equals(scheme) ||
            "file".equals(scheme) ||
            "http".equals(scheme) ||
            "https".equals(scheme) ||
            "ws".equals(scheme) ||
            "wss".equals(scheme);
  }

  private static int defaultPort(@Nullable String scheme) {
    if (scheme != null) {
      return switch (scheme) {
        case "ftp" -> 21;
        case "http", "ws" -> 80;
        case "https", "wss" -> 443;
        default -> -1;
      };
    }
    else {
      return -1;
    }
  }

  private void append(char ch) {
    this.buffer.append(ch);
  }

  private void append(int ch) {
    this.buffer.append((char) ch);
  }

  private void prepend(String s) {
    this.buffer.insert(0, s);
  }

  private void emptyBuffer() {
    this.buffer.setLength(0);
  }

  private int remaining(int deltaPos) {
    int pos = this.pointer + deltaPos;
    if (pos < this.input.length()) {
      return this.input.charAt(pos);
    }
    else {
      return EOF;
    }
  }

  private String percentEncode(int c, HierarchicalUriComponents.Type type) {
    return percentEncode(Character.toString(c), type);
  }

  private String percentEncode(String source, HierarchicalUriComponents.Type type) {
    if (this.encoding != null) {
      return HierarchicalUriComponents.encodeUriComponent(source, this.encoding, type);
    }
    else {
      return source;
    }
  }

  /**
   * A single-dot URL path segment is a URL path segment that is "." or an ASCII case-insensitive match for "%2e".
   */
  private static boolean isSingleDotPathSegment(StringBuilder b) {
    int len = b.length();
    if (len == 1) {
      char ch0 = b.charAt(0);
      return ch0 == '.';
    }
    else if (len == 3) {
      //  ASCII case-insensitive match for "%2e".
      char ch0 = b.charAt(0);
      char ch1 = b.charAt(1);
      char ch2 = b.charAt(2);
      return ch0 == '%' && ch1 == '2' && (ch2 == 'e' || ch2 == 'E');
    }
    else {
      return false;
    }
  }

  /**
   * A double-dot URL path segment is a URL path segment that is "/.." or an ASCII case-insensitive match for "/.%2e", "/%2e.", or "/%2e%2e".
   */
  private static boolean isDoubleDotPathSegment(StringBuilder b) {
    int len = b.length();
    if (len == 3) {
      char ch0 = b.charAt(0);
      char ch1 = b.charAt(1);
      char ch2 = b.charAt(2);
      return ch0 == '/' && ch1 == '.' && ch2 == '.';
    }
    else if (len == 5) {
      char ch0 = b.charAt(0);
      char ch1 = b.charAt(1);
      char ch2 = b.charAt(2);
      char ch3 = b.charAt(3);
      char ch4 = b.charAt(4);
      // case-insensitive match for "/.%2e" or "/%2e."
      return ch0 == '/' &&
              (ch1 == '.' && ch2 == '%' && ch3 == '2' && (ch4 == 'e' || ch4 == 'E')
                      || (ch1 == '%' && ch2 == '2' && (ch3 == 'e' || ch3 == 'E') && ch4 == '.'));
    }
    else if (len == 7) {
      char ch0 = b.charAt(0);
      char ch1 = b.charAt(1);
      char ch2 = b.charAt(2);
      char ch3 = b.charAt(3);
      char ch4 = b.charAt(4);
      char ch5 = b.charAt(5);
      char ch6 = b.charAt(6);
      // case-insensitive match for "/%2e%2e".
      return ch0 == '/' && ch1 == '%' && ch2 == '2' && (ch3 == 'e' || ch3 == 'E')
              && ch4 == '%' && ch5 == '2' && (ch6 == 'e' || ch6 == 'E');
    }
    else {
      return false;
    }
  }

  private static boolean isWindowsDriveLetter(CharSequence s, boolean normalized) {
    if (s.length() != 2) {
      return false;
    }
    char ch0 = s.charAt(0);
    if (!isAsciiAlpha(ch0)) {
      return false;
    }
    else {
      char ch1 = s.charAt(1);
      if (normalized) {
        return ch1 == ':';
      }
      else {
        return ch1 == ':' || ch1 == '|';
      }
    }
  }

  private enum State {

    SCHEME_START {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is an ASCII alpha, append c, lowercased, to buffer, and set state to scheme state.
        if (isAsciiAlpha(c)) {
          p.append(Character.toLowerCase((char) c));
          p.setState(SCHEME);
        }
        // EXTRA: if c is '{', then append c to buffer, set previous state to scheme state, and state to url template state.
        //
        else if (p.previousState != URL_TEMPLATE && c == '{') {
          p.append(c);
          p.previousState = SCHEME;
          p.state = URL_TEMPLATE;
        }
        // Otherwise, if state override is not given, set state to no scheme state and decrease pointer by 1.
        else if (p.stateOverride == null) {
          p.setState(NO_SCHEME);
          p.pointer--;
        }
        // Otherwise, return failure.
        else {
          p.failure(null);
        }
      }
    },
    SCHEME {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is an ASCII alphanumeric, U+002B (+), U+002D (-), or U+002E (.), append c, lowercased, to buffer.
        if (isAsciiAlphaNumeric(c) || (c == '+' || c == '-' || c == '.')) {
          p.append(Character.toLowerCase((char) c));
        }
        // EXTRA: if c is '{', then append c to buffer, set state to url template state.
        else if (p.previousState != URL_TEMPLATE && c == '{') {
          p.append(c);
          p.setState(URL_TEMPLATE);
        }
        // Otherwise, if c is U+003A (:), then:
        else if (c == ':') {
          // If state override is given, then:
          if (p.stateOverride != null) {
            boolean urlSpecialScheme = url.isSpecial();
            String bufferString = p.buffer.toString();
            boolean bufferSpecialScheme = isSpecialScheme(bufferString);
            // If url’s scheme is a special scheme and buffer is not a special scheme, then return.
            if (urlSpecialScheme && !bufferSpecialScheme) {
              return;
            }
            // If url’s scheme is not a special scheme and buffer is a special scheme, then return.
            if (!urlSpecialScheme && bufferSpecialScheme) {
              return;
            }
            // If url includes credentials or has a non-null port, and buffer is "file", then return.
            if ((url.includesCredentials() || url.port() != null) && "file".equals(bufferString)) {
              return;
            }
            // If url’s scheme is "file" and its host is an empty host, then return.
            if ("file".equals(url.scheme()) && (url.host() == null || url.host() == EmptyHost.INSTANCE)) {
              return;
            }
          }
          // Set url’s scheme to buffer.
          url.scheme = p.buffer.toString();
          // If state override is given, then:
          if (p.stateOverride != null) {
            // If url’s port is url’s scheme’s default port, then set url’s port to null.
            if (url.port instanceof IntPort intPort &&
                    intPort.value() == defaultPort(url.scheme)) {
              url.port = null;
              // Return.
              return;
            }
          }
          // Set buffer to the empty string.
          p.emptyBuffer();
          // If url’s scheme is "file", then:
          if (url.scheme.equals("file")) {
            // If remaining does not start with "//", special-scheme-missing-following-solidus validation error.
            if (p.validate() && p.remaining(0) != '/' && p.remaining(1) != '/') {
              p.validationError("\"file\" scheme not followed by \"//\".");
            }
            // Set state to file state.
            p.setState(FILE);
          }
          // Otherwise, if url is special, base is non-null, and base’s scheme is url’s scheme:
          else if (url.isSpecial() && p.base != null && p.base.scheme().equals(url.scheme)) {
            // Assert: base is special (and therefore does not have an opaque path).
            Assert.state(!p.base.path().isOpaque(), "Opaque path not expected");
            // Set state to special relative or authority state.
            p.setState(SPECIAL_RELATIVE_OR_AUTHORITY);
          }
          // Otherwise, if url is special, set state to special authority slashes state.
          else if (url.isSpecial()) {
            p.setState(SPECIAL_AUTHORITY_SLASHES);
          }
          // Otherwise, if remaining starts with an U+002F (/), set state to path or authority state and increase pointer by 1.
          else if (p.remaining(0) == '/') {
            p.setState(PATH_OR_AUTHORITY);
            p.pointer++;
          }
          // Otherwise, set url’s path to the empty string and set state to opaque path state.
          else {
            url.path = new PathSegment("");
            p.setState(OPAQUE_PATH);
          }
        }
        // Otherwise, if state override is not given, set buffer to the empty string, state to no scheme state, and start over (from the first code point in input).
        else if (p.stateOverride == null) {
          p.emptyBuffer();
          p.setState(NO_SCHEME);
          p.pointer = -1;
        }
        // Otherwise, return failure.
        else {
          p.failure(null);
        }

      }
    },
    NO_SCHEME {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If base is null, or base has an opaque path and c is not U+0023 (#), missing-scheme-non-relative-URL
        // validation error, return failure.
        if (p.base == null || p.base.path().isOpaque() && c != '#') {
          p.failure("The input is missing a scheme, because it does not begin with an ASCII alpha \"%s\", and no base URL was provided."
                  .formatted(Character.toString(c)));
        }
        // Otherwise, if base has an opaque path and c is U+0023 (#), set url’s scheme to base’s scheme, url’s
        // path to base’s path, url’s query to base’s query, url’s fragment to the empty string, and set state to fragment state.
        else if (p.base.path().isOpaque() && c == '#') {
          url.scheme = p.base.scheme();
          url.path = p.base.path();
          url.query = p.base.query;
          url.fragment = "";
          p.setState(FRAGMENT);
        }
        // Otherwise, if base’s scheme is not "file", set state to relative state and decrease pointer by 1.
        else if (!"file".equals(p.base.scheme())) {
          p.setState(RELATIVE);
          p.pointer--;
        }
        // Otherwise, set state to file state and decrease pointer by 1.
        else {
          p.setState(FILE);
          p.pointer--;
        }
      }
    },
    SPECIAL_RELATIVE_OR_AUTHORITY {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is U+002F (/) and remaining starts with U+002F (/), then set state to special authority ignore slashes state and increase pointer by 1.
        if (c == '/' && p.remaining(1) == '/') {
          p.setState(SPECIAL_AUTHORITY_IGNORE_SLASHES);
          p.pointer++;
        }
        // Otherwise, special-scheme-missing-following-solidus validation error, set state to relative state and decrease pointer by 1.
        else {
          if (p.validate()) {
            p.validationError("The input’s scheme is not followed by \"//\".");
          }
          p.setState(RELATIVE);
          p.pointer--;
        }
      }
    },
    PATH_OR_AUTHORITY {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is U+002F (/), then set state to authority state.
        if (c == '/') {
          p.setState(AUTHORITY);
        }
        // Otherwise, set state to path state, and decrease pointer by 1.
        else {
          p.setState(PATH);
          p.pointer--;
        }
      }
    },
    RELATIVE {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // Assert: base’s scheme is not "file".
        Assert.state(p.base != null && !"file".equals(p.base.scheme()), "Base scheme not provided or supported");
        // Set url’s scheme to base’s scheme.
        url.scheme = p.base.scheme;
        // If c is U+002F (/), then set state to relative slash state.
        if (c == '/') {
          // EXTRA : append '/' to let the path segment start with /
          p.append('/');
          p.setState(RELATIVE_SLASH);
        }
        // Otherwise, if url is special and c is U+005C (\), invalid-reverse-solidus validation error, set state to relative slash state.
        else if (url.isSpecial() && c == '\\') {
          if (p.validate()) {
            p.validationError("URL uses \\ instead of /.");
          }
          // EXTRA : append '/' to let the path segment start with /
          p.append('/');
          p.setState(RELATIVE_SLASH);
        }
        // Otherwise
        else {
          // Set url’s username to base’s username, url’s password to base’s password, url’s host to base’s host,
          // url’s port to base’s port, url’s path to a clone of base’s path, and url’s query to base’s query.
          url.username = p.base.username();
          url.password = p.base.password();
          url.host = p.base.host();
          url.port = p.base.port();
          url.path = p.base.path().clone();
          url.query = p.base.query;
          // If c is U+003F (?), then set url’s query to the empty string, and state to query state.
          if (c == '?') {
            url.query = "";
            p.setState(QUERY);
          }
          // Otherwise, if c is U+0023 (#), set url’s fragment to the empty string and state to fragment state.
          else if (c == '#') {
            url.fragment = "";
            p.setState(FRAGMENT);
          }
          // Otherwise, if c is not the EOF code point:
          else if (c != EOF) {
            // Set url’s query to null.
            url.query = null;
            // Shorten url’s path.
            url.shortenPath();
            // Set state to path state and decrease pointer by 1.
            p.setState(PATH);
            p.pointer--;
          }
        }
      }
    },
    RELATIVE_SLASH {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If url is special and c is U+002F (/) or U+005C (\), then:
        if (url.isSpecial() && (c == '/' || c == '\\')) {
          // If c is U+005C (\), invalid-reverse-solidus validation error.
          if (p.validate() && c == '\\') {
            p.validationError("URL uses \\ instead of /.");
          }
          // Set state to special authority ignore slashes state.
          p.setState(SPECIAL_AUTHORITY_IGNORE_SLASHES);
        }
        // Otherwise, if c is U+002F (/), then set state to authority state.
        else if (c == '/') {
          // EXTRA: empty buffer to remove appended slash, since this is not a path
          p.emptyBuffer();
          p.setState(AUTHORITY);
        }
        // Otherwise, set url’s username to base’s username, url’s password to base’s password, url’s host
        // to base’s host, url’s port to base’s port, state to path state, and then, decrease pointer by 1.
        else {
          Assert.state(p.base != null, "No base URL available");
          url.username = p.base.username();
          url.password = p.base.password();
          url.host = p.base.host();
          url.port = p.base.port();
          p.setState(PATH);
          p.pointer--;
        }

      }
    },
    SPECIAL_AUTHORITY_SLASHES {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is U+002F (/) and remaining starts with U+002F (/), then set state to special authority ignore slashes state and increase pointer by 1.
        if (c == '/' && p.remaining(0) == '/') {
          p.setState(SPECIAL_AUTHORITY_IGNORE_SLASHES);
          p.pointer++;
        }
        // Otherwise, special-scheme-missing-following-solidus validation error, set state to special authority ignore slashes state and decrease pointer by 1.
        else {
          if (p.validate()) {
            p.validationError("Scheme \"%s\" not followed by \"//\".".formatted(url.scheme));
          }
          p.setState(SPECIAL_AUTHORITY_IGNORE_SLASHES);
          p.pointer--;
        }
      }
    },
    SPECIAL_AUTHORITY_IGNORE_SLASHES {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is neither U+002F (/) nor U+005C (\), then set state to authority state and decrease pointer by 1.
        if (c != '/' && c != '\\') {
          p.setState(AUTHORITY);
          p.pointer--;
        }
        // Otherwise, special-scheme-missing-following-solidus validation error.
        else {
          if (p.validate()) {
            p.validationError("Scheme \"%s\" not followed by \"//\".".formatted(url.scheme));
          }
        }
      }
    },
    AUTHORITY {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is U+0040 (@), then:
        if (c == '@') {
          // Invalid-credentials validation error.
          if (p.validate()) {
            p.validationError("Invalid credentials");
          }
          // If atSignSeen is true, then prepend "%40" to buffer.
          if (p.atSignSeen) {
            p.prepend("%40");
          }
          // Set atSignSeen to true.
          p.atSignSeen = true;

          int bufferLen = p.buffer.length();
          StringBuilder username = new StringBuilder(bufferLen);
          StringBuilder password = new StringBuilder(bufferLen);

          // For each codePoint in buffer:
          for (int i = 0; i < bufferLen; i++) {
            int codePoint = p.buffer.codePointAt(i);
            // If codePoint is U+003A (:) and passwordTokenSeen is false, then set passwordTokenSeen to true and continue.
            if (codePoint == ':' && !p.passwordTokenSeen) {
              p.passwordTokenSeen = true;
              continue;
            }
            // Let encodedCodePoints be the result of running UTF-8 percent-encode codePoint using the userinfo percent-encode set.
            String encodedCodePoints = p.percentEncode(codePoint, HierarchicalUriComponents.Type.USER_INFO);
            // If passwordTokenSeen is true, then append encodedCodePoints to url’s password.
            if (p.passwordTokenSeen) {
              password.append(encodedCodePoints);
            }
            // Otherwise, append encodedCodePoints to url’s username.
            else {
              username.append(encodedCodePoints);
            }
          }
          url.username = username.toString();
          url.password = password.toString();
          // Set buffer to the empty string.
          p.emptyBuffer();
        }
        // Otherwise, if one of the following is true:
        // - c is the EOF code point, U+002F (/), U+003F (?), or U+0023 (#)
        // - url is special and c is U+005C (\)
        else if ((c == EOF || c == '/' || c == '?' || c == '#')
                || (url.isSpecial() && c == '\\')) {
          // If atSignSeen is true and buffer is the empty string, host-missing validation error, return failure.
          if (p.atSignSeen && p.buffer.isEmpty()) {
            p.failure("Missing host.");
          }
          // Decrease pointer by buffer’s code point length + 1, set buffer to the empty string, and set state to host state.
          p.pointer -= p.buffer.length() + 1;
          p.emptyBuffer();
          p.setState(HOST);
        }
        // Otherwise, append c to buffer.
        else {
          p.append(c);
        }
      }
    },
    HOST {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If state override is given and url’s scheme is "file", then decrease pointer by 1 and set state to file host state.
        if (p.stateOverride != null && "file".equals(url.scheme())) {
          p.pointer--;
          p.setState(FILE_HOST);
        }
        // Otherwise, if c is U+003A (:) and insideBrackets is false, then:
        else if (c == ':' && !p.insideBrackets) {
          // If buffer is the empty string, host-missing validation error, return failure.
          if (p.buffer.isEmpty()) {
            p.failure("Missing host.");
          }
          // If state override is given and state override is hostname state, then return.
          if (p.stateOverride == HOST) {
            return;
          }
          // Let host be the result of host parsing buffer with url is not special.
          // Set url’s host to host, buffer to the empty string, and state to port state.
          url.host = Host.parse(p.buffer.toString(), false, p.validationErrorHandler);
          p.emptyBuffer();
          p.setState(PORT);
        }
        // Otherwise, if one of the following is true:
        // - c is the EOF code point, U+002F (/), U+003F (?), or U+0023 (#)
        // - url is special and c is U+005C (\)
        else if ((c == EOF || c == '/' || c == '?' || c == '#') ||
                (url.isSpecial() && c == '\\')) {
          // then decrease pointer by 1, and then:
          p.pointer--;
          // If url is special and buffer is the empty string, host-missing validation error, return failure.
          if (url.isSpecial() && p.buffer.isEmpty()) {
            p.failure("The input has a special scheme, but does not contain a host.");
          }
          // Otherwise, if state override is given, buffer is the empty string, and either url includes credentials or url’s port is non-null, return.
          else if (p.stateOverride != null && p.buffer.isEmpty() &&
                  (url.includesCredentials() || url.port() != null)) {
            return;
          }
          // EXTRA: if buffer is not empty
          if (!p.buffer.isEmpty()) {
            // Let host be the result of host parsing buffer with url is not special.
            // Set url’s host to host, buffer to the empty string, and state to path start state.
            url.host = Host.parse(p.buffer.toString(), false, p.validationErrorHandler);
          }
          else {
            url.host = EmptyHost.INSTANCE;
          }
          p.emptyBuffer();
          p.setState(PATH_START);
          // If state override is given, then return.
          if (p.stateOverride != null) {
            return;
          }
        }
        // Otherwise:
        else {
          // If c is U+005B ([), then set insideBrackets to true.
          if (c == '[') {
            p.insideBrackets = true;
          }
          // If c is U+005D (]), then set insideBrackets to false.
          else if (c == ']') {
            p.insideBrackets = false;
          }
          // Append c to buffer.
          p.append(c);
        }
      }
    },
    PORT {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is an ASCII digit, append c to buffer.
        if (isAsciiDigit(c)) {
          p.append(c);
        }
        // EXTRA: if c is '{', then append c to buffer, set state to url template state.
        else if (p.previousState != URL_TEMPLATE && c == '{') {
          p.append(c);
          p.setState(URL_TEMPLATE);
        }
        // Otherwise, if one of the following is true:
        // - c is the EOF code point, U+002F (/), U+003F (?), or U+0023 (#)
        // - url is special and c is U+005C (\)
        // - state override is given
        else if (c == EOF || c == '/' || c == '?' || c == '#'
                || (url.isSpecial() && c == '\\')
                || (p.stateOverride != null)) {
          // If buffer is not the empty string, then:
          if (!p.buffer.isEmpty()) {
            // EXTRA: if buffer contains only ASCII digits, then
            if (containsOnlyAsciiDigits(p.buffer)) {
              try {
                // Let port be the mathematical integer value that is represented by buffer in radix-10 using ASCII digits for digits with values 0 through 9.
                int port = Integer.parseInt(p.buffer, 0, p.buffer.length(), 10);
                // If port is greater than 2^16 − 1, port-out-of-range validation error, return failure.
                if (port > MAX_PORT) {
                  p.failure("Port \"%d\" is out of range".formatted(port));
                }
                int defaultPort = defaultPort(url.scheme);
                // Set url’s port to null, if port is url’s scheme’s default port; otherwise to port.
                if (defaultPort == -1 || port == defaultPort) {
                  url.port = null;
                }
                else {
                  url.port = new IntPort(port);
                }
              }
              catch (NumberFormatException ex) {
                p.failure(ex.getMessage());
              }
            }
            // EXTRA: otherwise, set url's port to buffer
            else {
              url.port = new StringPort(p.buffer.toString());
            }
            // Set buffer to the empty string.
            p.emptyBuffer();
          }
          // If state override is given, then return.
          if (p.stateOverride != null) {
            return;
          }
          // Set state to path start state and decrease pointer by 1.
          p.setState(PATH_START);
          p.pointer--;
        }
        // Otherwise, port-invalid validation error, return failure.
        else {
          p.failure("Invalid port: \"%s\"".formatted(Character.toString(c)));
        }
      }
    },
    FILE {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // Set url’s scheme to "file".
        url.scheme = "file";
        // Set url’s host to the empty string.
        url.host = EmptyHost.INSTANCE;
        // If c is U+002F (/) or U+005C (\), then:
        if (c == '/' || c == '\\') {
          // If c is U+005C (\), invalid-reverse-solidus validation error.
          if (p.validate() && c == '\\') {
            p.validationError("URL uses \\ instead of /.");
          }
          // Set state to file slash state.
          p.setState(FILE_SLASH);
        }
        // Otherwise, if base is non-null and base’s scheme is "file":
        else if (p.base != null && p.base.scheme().equals("file")) {
          // Set url’s host to base’s host, url’s path to a clone of base’s path, and url’s query to base’s query.
          url.host = p.base.host;
          url.path = p.base.path().clone();
          url.query = p.base.query;
          // If c is U+003F (?), then set url’s query to the empty string and state to query state.
          if (c == '?') {
            url.query = "";
            p.setState(QUERY);
          }
          // Otherwise, if c is U+0023 (#), set url’s fragment to the empty string and state to fragment state.
          else if (c == '#') {
            url.fragment = "";
            p.setState(FRAGMENT);
          }
          // Otherwise, if c is not the EOF code point:
          else if (c != EOF) {
            // Set url’s query to null.
            url.query = null;
            // If the code point substring from pointer to the end of input does not start with a Windows drive letter, then shorten url’s path.
            String substring = p.input.substring(p.pointer, Math.min(p.pointer + 2, p.input.length()));
            if (!isWindowsDriveLetter(substring, false)) {
              url.shortenPath();
            }
            // Otherwise:
            else {
              // File-invalid-Windows-drive-letter validation error.
              if (p.validate()) {
                p.validationError("The input is a relative-URL string that starts with a Windows " +
                        "drive letter and the base URL’s scheme is \"file\".");
              }
              // Set url’s path to « ».
              url.path = new PathSegments();
            }
            // Set state to path state and decrease pointer by 1.
            p.setState(PATH);
            p.pointer--;
          }
        }
        // Otherwise, set state to path state, and decrease pointer by 1.
        else {
          p.setState(PATH);
          p.pointer--;
        }
      }
    },
    FILE_SLASH {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is U+002F (/) or U+005C (\), then:
        if (c == '/' || c == '\\') {
          // If c is U+005C (\), invalid-reverse-solidus validation error.
          if (p.validate() && c == '\\') {
            p.validationError("URL uses \\ instead of /.");
          }
          // Set state to file host state.
          p.setState(FILE_HOST);
        }
        // Otherwise:
        else {
          // If base is non-null and base’s scheme is "file", then:
          if (p.base != null && p.base.scheme.equals("file")) {
            // Set url’s host to base’s host.
            url.host = p.base.host;
            // If the code point substring from pointer to the end of input does not start with a Windows drive letter and base’s path[0] is a normalized Windows drive letter, then append base’s path[0] to url’s path.
            String substring = p.input.substring(p.pointer, Math.min(p.pointer + 2, p.input.length()));
            if (!isWindowsDriveLetter(substring, false)
                    && p.base.path instanceof PathSegments basePath
                    && !basePath.isEmpty()
                    && isWindowsDriveLetter(basePath.get(0), false)) {
              url.path.append(basePath.get(0));
            }
          }
          // Set state to path state, and decrease pointer by 1.
          p.setState(PATH);
          p.pointer--;
        }
      }
    },
    FILE_HOST {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is the EOF code point, U+002F (/), U+005C (\), U+003F (?), or U+0023 (#), then decrease pointer by 1 and then:
        if (c == EOF || c == '/' || c == '\\' || c == '?' || c == '#') {
          p.pointer--;
          // If state override is not given and buffer is a Windows drive letter, file-invalid-Windows-drive-letter-host validation error, set state to path state.
          if (p.stateOverride == null && isWindowsDriveLetter(p.buffer, false)) {
            p.validationError("A file: URL’s host is a Windows drive letter.");
            p.setState(PATH);
          }
          // Otherwise, if buffer is the empty string, then:
          else if (p.buffer.isEmpty()) {
            // Set url’s host to the empty string.
            url.host = EmptyHost.INSTANCE;
            // If state override is given, then return.
            if (p.stateOverride != null) {
              return;
            }
            // Set state to path start state.
            p.setState(PATH_START);
          }
          // Otherwise, run these steps:
          else {
            // Let host be the result of host parsing buffer with url is not special.
            Host host = Host.parse(p.buffer.toString(), false, p.validationErrorHandler);
            // If host is "localhost", then set host to the empty string.
            if (host instanceof Domain domain && domain.domain().equals("localhost")) {
              host = EmptyHost.INSTANCE;
            }
            // Set url’s host to host.
            url.host = host;
            // If state override is given, then return.
            if (p.stateOverride != null) {
              return;
            }
            // Set buffer to the empty string and state to path start state.
            p.emptyBuffer();
            p.setState(PATH_START);
          }
        }
        // Otherwise, append c to buffer.
        else {
          p.append(c);
        }
      }
    },
    PATH_START {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If url is special, then:
        if (url.isSpecial()) {
          // If c is U+005C (\), invalid-reverse-solidus validation error.
          if (p.validate() && c == '\\') {
            p.validationError("URL uses \"\\\" instead of \"/\"");
          }
          // Set state to path state.
          p.setState(PATH);
          // If c is neither U+002F (/) nor U+005C (\), then decrease pointer by 1.
          if (c != '/' && c != '\\') {
            p.pointer--;
          }
          else {
            p.append('/');
          }
        }
        // Otherwise, if state override is not given and if c is U+003F (?), set url’s query to the empty string and state to query state.
        else if (p.stateOverride == null && c == '?') {
          url.query = "";
          p.setState(QUERY);
        }
        // Otherwise, if state override is not given and if c is U+0023 (#), set url’s fragment to the empty string and state to fragment state.
        else if (p.stateOverride == null && c == '#') {
          url.fragment = "";
          p.setState(FRAGMENT);
        }
        // Otherwise, if c is not the EOF code point:
        else if (c != EOF) {
          // Set state to path state.
          p.setState(PATH);
          // If c is not U+002F (/), then decrease pointer by 1.
          if (c != '/') {
            p.pointer--;
          }
          // EXTRA: otherwise append '/' to let the path segment start with /
          else {
            p.append('/');
          }
        }
        // Otherwise, if state override is given and url’s host is null, append the empty string to url’s path.
        else if (p.stateOverride != null && url.host() == null) {
          url.path().append("");
        }
      }
    },
    PATH {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If one of the following is true:
        // - c is the EOF code point or U+002F (/)
        // - url is special and c is U+005C (\)
        // - state override is not given and c is U+003F (?) or U+0023 (#)
        // then:
        if (c == EOF || c == '/' ||
                url.isSpecial() && c == '\\' ||
                (p.stateOverride == null && (c == '?' || c == '#'))) {
          // If url is special and c is U+005C (\), invalid-reverse-solidus validation error.
          if (p.validate() && url.isSpecial() && c == '\\') {
            p.validationError("URL uses \"\\\" instead of \"/\"");
          }
          // If buffer is a double-dot URL path segment, then:
          if (isDoubleDotPathSegment(p.buffer)) {
            // Shorten url’s path.
            url.shortenPath();
            // If neither c is U+002F (/), nor url is special and c is U+005C (\), append the empty string to url’s path.
            if (c != '/' && !(url.isSpecial() && c == '\\')) {
              url.path.append("");
            }
          }
          else {
            boolean singlePathSegment = isSingleDotPathSegment(p.buffer);
            // Otherwise, if buffer is a single-dot URL path segment and if neither c is U+002F (/), nor url is special and c is U+005C (\), append the empty string to url’s path.
            if (singlePathSegment && c != '/' && !(url.isSpecial() && c == '\\')) {
              url.path.append("");
            }
            // Otherwise, if buffer is not a single-dot URL path segment, then:
            else if (!singlePathSegment) {
              // If url’s scheme is "file", url’s path is empty, and buffer is a Windows drive letter, then replace the second code point in buffer with U+003A (:).
              if ("file".equals(url.scheme) && url.path.isEmpty() && isWindowsDriveLetter(p.buffer, false)) {
                p.buffer.setCharAt(1, ':');
              }
              // Append buffer to url’s path.
              url.path.append(p.buffer.toString());
            }
          }
          // Set buffer to the empty string.
          p.emptyBuffer();
          if (c == '/' || url.isSpecial() && c == '\\') {
            p.append('/');
          }
          // If c is U+003F (?), then set url’s query to the empty string and state to query state.
          if (c == '?') {
            url.query = "";
            p.setState(QUERY);
          }
          // If c is U+0023 (#), then set url’s fragment to the empty string and state to fragment state.
          if (c == '#') {
            url.fragment = "";
            p.setState(FRAGMENT);
          }
        }
        // EXTRA: Otherwise, if c is '{', then append c to buffer, set state to url template state.
        else if (p.previousState != URL_TEMPLATE && c == '{') {
          p.append(c);
          p.setState(URL_TEMPLATE);
        }
        // Otherwise, basicUrlParser these steps:
        else {
          if (p.validate()) {
            // If c is not a URL code point and not U+0025 (%), invalid-URL-unit validation error.
            if (!isUrlCodePoint(c) && c != '%') {
              p.validationError("Invalid URL Unit: \"%s\"".formatted((char) c));
            }
            // If c is U+0025 (%) and remaining does not start with two ASCII hex digits, invalid-URL-unit validation error.
            else if (c == '%' &&
                    (p.pointer >= p.input.length() - 2 ||
                            !isAsciiHexDigit(p.input.charAt(p.pointer + 1)) ||
                            !isAsciiHexDigit(p.input.charAt(p.pointer + 2)))) {
              p.validationError("Invalid URL Unit: \"%s\"".formatted((char) c));
            }
          }
          // UTF-8 percent-encode c using the path percent-encode set and append the result to buffer.
          String encoded = p.percentEncode(c, HierarchicalUriComponents.Type.PATH_SEGMENT);
          p.buffer.append(encoded);
        }
      }
    },
    OPAQUE_PATH {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // EXTRA: if previous state is URL Template and the buffer is empty, append buffer to url's path and empty the buffer
        if (p.previousState == URL_TEMPLATE && !p.buffer.isEmpty()) {
          url.path.append(p.buffer.toString());
          p.emptyBuffer();
        }
        // If c is U+003F (?), then set url’s query to the empty string and state to query state.
        if (c == '?') {
          url.query = "";
          p.setState(QUERY);
        }
        // Otherwise, if c is U+0023 (#), then set url’s fragment to the empty string and state to fragment state.
        else if (c == '#') {
          url.fragment = "";
          p.setState(FRAGMENT);
        }
        // EXTRA: Otherwise, if c is '{', then append c to buffer, set state to url template state.
        else if (p.previousState != URL_TEMPLATE && c == '{') {
          p.append(c);
          p.setState(URL_TEMPLATE);
        }
        // Otherwise:
        else {
          if (p.validate()) {
            // If c is not the EOF code point, not a URL code point, and not U+0025 (%), invalid-URL-unit validation error.
            if (c != EOF && !isUrlCodePoint(c) && c != '%') {
              p.validationError("Invalid URL Unit: \"%s\"".formatted((char) c));
            }
            // If c is U+0025 (%) and remaining does not start with two ASCII hex digits, invalid-URL-unit validation error.
            else if (c == '%' &&
                    (p.pointer >= p.input.length() - 2 ||
                            !isAsciiHexDigit(p.input.charAt(p.pointer + 1)) ||
                            !isAsciiHexDigit(p.input.charAt(p.pointer + 2)))) {
              p.validationError("Invalid URL Unit: \"%s\"".formatted((char) c));
            }
          }
          // If c is not the EOF code point, UTF-8 percent-encode c using the C0 control percent-encode set and append the result to url’s path.
          if (c != EOF) {
            String encoded = p.percentEncode(c, HierarchicalUriComponents.Type.C0);
            url.path.append(encoded);
          }
        }
      }
    },
    QUERY {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If encoding is not UTF-8 and one of the following is true:
        // - url is not special
        // - url’s scheme is "ws" or "wss"
        //  then set encoding to UTF-8.
        if (p.encoding != null &&
                !StandardCharsets.UTF_8.equals(p.encoding) &&
                (!url.isSpecial() || "ws".equals(url.scheme) || "wss".equals(url.scheme))) {
          p.encoding = StandardCharsets.UTF_8;
        }
        // If one of the following is true:
        // - state override is not given and c is U+0023 (#)
        // - c is the EOF code point
        if ((p.stateOverride == null && c == '#') || c == EOF) {
          // Let queryPercentEncodeSet be the special-query percent-encode set if url is special; otherwise the query percent-encode set.
          // Percent-encode after encoding, with encoding, buffer, and queryPercentEncodeSet, and append the result to url’s query.
          String encoded = p.percentEncode(p.buffer.toString(), HierarchicalUriComponents.Type.QUERY);
          Assert.state(url.query != null, "Url's query should not be null");
          url.query += encoded;
          // Set buffer to the empty string.
          p.emptyBuffer();
          // If c is U+0023 (#), then set url’s fragment to the empty string and state to fragment state.
          if (c == '#') {
            url.fragment = "";
            p.setState(FRAGMENT);
          }
        }
        // EXTRA: Otherwise, if c is '{', then append c to buffer, set state to url template state.
        else if (p.previousState != URL_TEMPLATE && c == '{') {
          p.append(c);
          p.setState(URL_TEMPLATE);
        }
        // Otherwise, if c is not the EOF code point:
        else if (c != EOF) {
          if (p.validate()) {
            // If c is not a URL code point and not U+0025 (%), invalid-URL-unit validation error.
            if (!isUrlCodePoint(c) && c != '%') {
              p.validationError("Invalid URL Unit: \"%s\"".formatted((char) c));
            }
            // If c is U+0025 (%) and remaining does not start with two ASCII hex digits, invalid-URL-unit validation error.
            else if (c == '%' &&
                    (p.pointer >= p.input.length() - 2 ||
                            !isAsciiHexDigit(p.input.charAt(p.pointer + 1)) ||
                            !isAsciiHexDigit(p.input.charAt(p.pointer + 2)))) {
              p.validationError("Invalid URL Unit: \"%s\"".formatted((char) c));
            }
          }
          // Append c to buffer.
          p.append(c);
        }
      }
    },
    FRAGMENT {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        // If c is not the EOF code point, then:
        if (c != EOF) {
          if (p.validate()) {
            // If c is not a URL code point and not U+0025 (%), invalid-URL-unit validation error.
            if (!isUrlCodePoint(c) && c != '%') {
              p.validationError("Invalid URL Unit: \"%s\"".formatted((char) c));
            }
            // If c is U+0025 (%) and remaining does not start with two ASCII hex digits, invalid-URL-unit validation error.
            else if (c == '%' &&
                    (p.pointer >= p.input.length() - 2 ||
                            !isAsciiHexDigit(p.input.charAt(p.pointer + 1)) ||
                            !isAsciiHexDigit(p.input.charAt(p.pointer + 2)))) {
              p.validationError("Invalid URL Unit: \"%s\"".formatted((char) c));
            }
          }
          // UTF-8 percent-encode c using the fragment percent-encode set and append the result to url’s fragment.
          String encoded = p.percentEncode(c, HierarchicalUriComponents.Type.FRAGMENT);
          Assert.state(url.fragment != null, "Url's fragment should not be null");
          url.fragment += encoded;
        }
      }
    },
    URL_TEMPLATE {
      @Override
      public void handle(int c, UrlRecord url, UrlParser p) {
        Assert.state(p.previousState != null, "No previous state set");
        if (c == '}') {
          p.append(c);
          p.setState(p.previousState);
        }
        else if (c == EOF) {
          p.pointer -= p.buffer.length() + 1;
          p.emptyBuffer();
          p.setState(p.previousState);
        }
        else {
          p.append(c);
        }
      }
    };

    public abstract void handle(int c, UrlRecord url, UrlParser p);

  }

  /**
   * A URL is a struct that represents a universal identifier. To disambiguate from a valid URL string it can also be
   * referred to as a
   * <em>URL record</em>.
   */
  static final class UrlRecord {

    private String scheme = "";

    private String username = "";

    private String password = "";

    @Nullable
    private Host host = null;

    @Nullable
    private Port port = null;

    private Path path = new PathSegments();

    @Nullable
    private String query = null;

    @Nullable
    private String fragment = null;

    public UrlRecord() {
    }

    /**
     * A URL is special if its scheme is a special scheme. A URL is not special if its scheme is not a special scheme.
     */
    public boolean isSpecial() {
      return isSpecialScheme(this.scheme);
    }

    /**
     * A URL includes credentials if its username or password is not the empty string.
     */
    public boolean includesCredentials() {
      return !this.username.isEmpty() || !this.password.isEmpty();
    }

    /**
     * A URL has an opaque path if its path is a URL path segment.
     */
    public boolean hasOpaquePath() {
      return path().isOpaque();
    }

    /**
     * A URL’s scheme is an ASCII string that identifies the type of URL and can be used to dispatch a URL for
     * further processing after parsing. It is initially the empty string.
     */
    public String scheme() {
      return this.scheme;
    }

    /**
     * A URL’s username is an ASCII string identifying a username. It is initially the empty string.
     */
    public String username() {
      return this.username;
    }

    /**
     * A URL’s password is an ASCII string identifying a password. It is initially the empty string.
     */
    public String password() {
      return this.password;
    }

    /**
     * A URL’s host is {@code null} or a {@linkplain Host host}. It is initially {@code null}.
     */
    @Nullable
    public Host host() {
      return this.host;
    }

    /**
     * A URL’s port is either null, a string representing a 16-bit unsigned integer  that identifies a networking
     * port, or a string containing a uri template . It is initially {@code null}.
     */
    @Nullable
    public Port port() {
      return this.port;
    }

    /**
     * A URL’s path is a URL {@linkplain Path path}, usually identifying a location. It is initially {@code « »}.
     */
    public Path path() {
      return this.path;
    }

    /**
     * To shorten a url’s path:
     * <ol>
     * <li>Assert: url does not have an opaque path.</li>
     * <li>Let path be url’s path.</li>
     * <li>If url’s scheme is "file", path’s size is 1, and path[0] is a
     * normalized Windows drive letter, then return.</li>
     * <li>Remove path’s last item, if any.</li>
     * </ol>
     */
    public void shortenPath() {
      this.path.shorten(this.scheme);
    }

    /**
     * A URL’s query is either {@code null} or an ASCII string. It is initially {@code null}.
     */
    @Nullable
    public String query() {
      return this.query;
    }

    /**
     * A URL’s fragment is either {@code null}  or an ASCII string that can be used for further processing on the
     * resource the URL’s other components identify. It is initially {@code null}.
     */
    @Nullable
    public String fragment() {
      return this.fragment;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (UrlRecord) obj;
      return Objects.equals(this.scheme, that.scheme) &&
              Objects.equals(this.username, that.username) &&
              Objects.equals(this.password, that.password) &&
              Objects.equals(this.host, that.host) &&
              Objects.equals(this.port, that.port) &&
              Objects.equals(this.path, that.path) &&
              Objects.equals(this.query, that.query) &&
              Objects.equals(this.fragment, that.fragment);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.scheme, this.username, this.password, this.host, this.port, this.path, this.query, this.fragment);
    }

    @Override
    public String toString() {
      return "UrlRecord[scheme=%s, username=%s, password=%s, host=%s, port=%s, path=%s, query=%s, fragment=%s]"
              .formatted(this.scheme, this.username, this.password, this.host, this.port, this.path, this.query, this.fragment);
    }

  }

  /**
   * A host is a domain, an IP address, an opaque host, or an empty host.
   * Typically a host serves as a network address, but it is sometimes used as
   * opaque identifier in URLs where a network address is not necessary.
   */
  sealed interface Host permits Domain, EmptyHost, IpAddressHost, OpaqueHost {

    /**
     * The host parser takes a scalar value string input with an optional
     * boolean isOpaque (default false), and then runs these steps. They return failure or a host.
     */
    static Host parse(String input, boolean isOpaque, @Nullable Consumer<String> validationErrorHandler) {
      // If input starts with U+005B ([), then:
      if (!input.isEmpty() && input.charAt(0) == '[') {
        int last = input.length() - 1;
        // If input does not end with U+005D (]), IPv6-unclosed validation error, return failure.
        if (input.charAt(last) != ']') {
          throw new InvalidUrlException("IPv6 address is missing the closing \"]\").");
        }
        // Return the result of IPv6 parsing input with its leading U+005B ([) and trailing U+005D (]) removed.
        String ipv6Host = input.substring(1, last);
        return new IpAddressHost(Ipv6Address.parse(ipv6Host));
      }
      // If isOpaque is true, then return the result of opaque-host parsing input.
      if (isOpaque) {
        return OpaqueHost.parse(input);
      }
      // Assert: input is not the empty string.
      Assert.state(!input.isEmpty(), "Input should not be empty");

      // Let domain be the result of running UTF-8 decode without BOM on the percent-decoding of input.
      String domain = UriUtils.decode(input, StandardCharsets.UTF_8);
      // Let asciiDomain be the result of running domain to ASCII with domain and false.
      String asciiDomain = domainToAscii(domain, false);

      for (int i = 0; i < asciiDomain.length(); i++) {
        char ch = asciiDomain.charAt(i);
        // If asciiDomain contains a forbidden domain code point, domain-invalid-code-point validation error, return failure.
        if (isForbiddenDomain(ch)) {
          throw new InvalidUrlException("Invalid character \"%s\" in domain \"%s\"".formatted(ch, input));
        }
      }
      // If asciiDomain ends in a number, then return the result of IPv4 parsing asciiDomain.
      if (endsInNumber(asciiDomain)) {
        Ipv4Address address = Ipv4Address.parse(asciiDomain, validationErrorHandler);
        return new IpAddressHost(address);
      }
      // Return asciiDomain.
      else {
        return new Domain(asciiDomain);
      }
    }

    private static boolean endsInNumber(String input) {
      // Let parts be the result of strictly splitting input on U+002E (.).
      List<String> parts = tokenize(input, ".");
      int lastIdx = parts.size() - 1;
      // If the last item in parts is the empty string, then:
      if (parts.get(lastIdx).isEmpty()) {
        // If parts’s size is 1, then return false.
        if (parts.size() == 1) {
          return false;
        }
        // Remove the last item from parts.
        parts.remove(lastIdx);
      }
      // Let last be the last item in parts.
      String last = parts.get(parts.size() - 1);
      // If last is non-empty and contains only ASCII digits, then return true.
      if (!last.isEmpty() && containsOnlyAsciiDigits(last)) {
        return true;
      }
      // If parsing last as an IPv4 number does not return failure, then return true.
      try {
        Ipv4Address.parseIpv4Number(last);
        return true;
      }
      catch (InvalidUrlException ignored) {
      }
      // Return false.
      return false;
    }

  }

  /**
   * A domain is a non-empty ASCII string that identifies a realm within a
   * network. [RFC1034].
   */
  static final class Domain implements Host {

    private final String domain;

    Domain(String domain) {
      this.domain = domain;
    }

    public String domain() {
      return this.domain;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      else if (o instanceof Domain other) {
        return this.domain.equals(other.domain);
      }
      else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return this.domain.hashCode();
    }

    @Override
    public String toString() {
      return this.domain;
    }

  }

  static final class IpAddressHost implements Host {

    private final IpAddress address;

    private final String addressString;

    IpAddressHost(IpAddress address) {
      this.address = address;
      if (address instanceof Ipv6Address) {
        this.addressString = "[" + address + "]";
      }
      else {
        this.addressString = address.toString();
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      else if (obj instanceof IpAddressHost other) {
        return this.address.equals(other.address);
      }
      else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return this.address.hashCode();
    }

    @Override
    public String toString() {
      return this.addressString;
    }
  }

  record OpaqueHost(String domain) implements Host {

    public static OpaqueHost parse(String input) {
      throw new UnsupportedOperationException("Not implemented yet");
    }
  }

  static final class EmptyHost implements Host {

    static final EmptyHost INSTANCE = new EmptyHost();

    private EmptyHost() {
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this || obj != null && obj.getClass() == this.getClass();
    }

    @Override
    public int hashCode() {
      return 1;
    }

    @Override
    public String toString() {
      return "";
    }

  }

  sealed interface IpAddress permits Ipv4Address, Ipv6Address {

  }

  static final class Ipv4Address implements IpAddress {

    private final int address;

    private final String string;

    Ipv4Address(int address) {
      this.address = address;
      this.string = serialize(address);
    }

    /**
     * The IPv4 serializer takes an IPv4 address {@code address} and then runs these steps. They return an ASCII string.
     */
    private static String serialize(int address) {
      //Let output be the empty string.
      StringBuilder output = new StringBuilder();
      //Let n be the value of address.
      int n = address;
      //For each i in the range 1 to 4, inclusive:
      for (int i = 1; i <= 4; i++) {
        // Prepend n % 256, serialized, to output.
        output.insert(0, Integer.toUnsignedString(Integer.remainderUnsigned(n, 256)));
        //If i is not 4, then prepend U+002E (.) to output.
        if (i != 4) {
          output.insert(0, '.');
        }
        //Set n to floor(n / 256).
        n = Math.floorDiv(n, 256);
      }
      //Return output.
      return output.toString();
    }

    public static Ipv4Address parse(String input, @Nullable Consumer<String> validationErrorHandler) {
      // Let parts be the result of strictly splitting input on U+002E (.).
      List<String> parts = tokenize(input, ".");
      int partsSize = parts.size();
      // If the last item in parts is the empty string, then:
      if (parts.get(partsSize - 1).isEmpty()) {
        // IPv4-empty-part validation error.
        if (validationErrorHandler != null) {
          validationErrorHandler.accept("IPv4 address ends with \".\"");
        }
        // If parts’s size is greater than 1, then remove the last item from parts.
        if (partsSize > 1) {
          parts.remove(partsSize - 1);
          partsSize--;
        }
      }
      // If parts’s size is greater than 4, IPv4-too-many-parts validation error, return failure.
      if (partsSize > 4) {
        throw new InvalidUrlException("IPv4 address does not consist of exactly 4 parts.");
      }
      // Let numbers be an empty list.
      List<Integer> numbers = new ArrayList<>(partsSize);
      // For each part of parts:
      for (int i = 0; i < partsSize; i++) {
        String part = parts.get(i);
        // Let result be the result of parsing part.
        ParseIpv4NumberResult result = parseIpv4Number(part);
        if (validationErrorHandler != null && result.validationError()) {
          validationErrorHandler.accept("The IPv4 address contains numbers expressed using hexadecimal or octal digits.");
        }
        // Append result to numbers.
        numbers.add(result.number());
      }
      for (Iterator<Integer> iterator = numbers.iterator(); iterator.hasNext(); ) {
        Integer number = iterator.next();
        // If any item in numbers is greater than 255, IPv4-out-of-range-part validation error.
        if (validationErrorHandler != null && number > 255) {
          validationErrorHandler.accept("An IPv4 address part exceeds 255.");
        }
        if (iterator.hasNext()) {
          // If any but the last item in numbers is greater than 255, then return failure.
          if (number > 255) {
            throw new InvalidUrlException("An IPv4 address part exceeds 255.");
          }
        }
        else {
          // If the last item in numbers is greater than or equal to 256^(5 − numbers’s size), then return failure.
          double limit = Math.pow(256, (5 - numbers.size()));
          if (number >= limit) {
            throw new InvalidUrlException("IPv4 address part %d exceeds %s.'".formatted(number, limit));
          }
        }
      }
      // Let ipv4 be the last item in numbers.
      int ipv4 = numbers.get(numbers.size() - 1);
      // Remove the last item from numbers.
      numbers.remove(numbers.size() - 1);
      // Let counter be 0.
      int counter = 0;
      // For each n of numbers:
      for (Integer n : numbers) {
        // Increment ipv4 by n × 256^(3 − counter).
        int increment = n * (int) Math.pow(256, 3 - counter);
        ipv4 += increment;
        // Increment counter by 1.
        counter++;
      }
      // Return ipv4.
      return new Ipv4Address(ipv4);
    }

    /**
     * The IPv4 number parser takes an ASCII string input and then runs these steps. They return failure or a tuple of a number and a boolean.
     */
    private static ParseIpv4NumberResult parseIpv4Number(String input) {
      // If input is the empty string, then return failure.
      if (input.isEmpty()) {
        throw new InvalidUrlException("Input is empty");
      }
      // Let validationError be false.
      boolean validationError = false;
      // Let R be 10.
      int r = 10;
      int len = input.length();
      // If input contains at least two code points and the first two code points are either "0X" or "0x", then:
      if (len >= 2) {
        char ch0 = input.charAt(0);
        char ch1 = input.charAt(1);
        if (ch0 == '0' && (ch1 == 'X' || ch1 == 'x')) {
          // Set validationError to true.
          validationError = true;
          // Remove the first two code points from input.
          input = input.substring(2);
          // Set R to 16.
          r = 16;
        }
        // Otherwise, if input contains at least two code points and the first code point is U+0030 (0), then:
        else if (ch0 == '0') {
          // Set validationError to true.
          validationError = true;
          // Remove the first code point from input.
          input = input.substring(1);
          // Set R to 8.
          r = 8;
        }
      }
      // If input is the empty string, then return (0, true).
      if (input.isEmpty()) {
        return new ParseIpv4NumberResult(0, true);
      }
      try {
        // Let output be the mathematical integer value that is represented by input in radix-R notation, using ASCII hex digits for digits with values 0 through 15.
        int output = Integer.parseInt(input, r);
        // Return (output, validationError).
        return new ParseIpv4NumberResult(output, validationError);
      }
      catch (NumberFormatException ex) {
        throw new InvalidUrlException("Could not parse \"%s\" as integer: %s".formatted(input, ex.getMessage()), ex);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      else if (o instanceof Ipv4Address other) {
        return this.address == other.address;
      }
      else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return this.address;
    }

    @Override
    public String toString() {
      return this.string;
    }
  }

  static final class Ipv6Address implements IpAddress {

    private final int[] pieces;

    private final String string;

    private Ipv6Address(int[] pieces) {
      Assert.state(pieces.length == 8, "Invalid amount of IPv6 pieces");
      this.pieces = pieces;
      this.string = serialize(pieces);
    }

    /**
     * The IPv6 parser takes a scalar value string input and then runs these steps. They return failure or an IPv6 address.
     */
    public static Ipv6Address parse(String input) {
      // Let address be a new IPv6 address whose IPv6 pieces are all 0.
      int[] address = new int[8];
      // Let pieceIndex be 0.
      int pieceIndex = 0;
      // Let compress be null.
      Integer compress = null;
      // Let pointer be a pointer for input.
      int pointer = 0;
      int inputLength = input.length();
      int c = (inputLength > 0) ? input.charAt(0) : EOF;
      // If c is U+003A (:), then:
      if (c == ':') {
        // If remaining does not start with U+003A (:), IPv6-invalid-compression validation error, return failure.
        if (inputLength > 1 && input.charAt(1) != ':') {
          throw new InvalidUrlException("IPv6 address begins with improper compression.");
        }
        // Increase pointer by 2.
        pointer += 2;
        // Increase pieceIndex by 1 and then set compress to pieceIndex.
        pieceIndex++;
        compress = pieceIndex;
      }
      c = (pointer < inputLength) ? input.charAt(pointer) : EOF;
      // While c is not the EOF code point:
      while (c != EOF) {
        // If pieceIndex is 8, IPv6-too-many-pieces validation error, return failure.
        if (pieceIndex == 8) {
          throw new InvalidUrlException("IPv6 address contains more than 8 pieces.");
        }
        // If c is U+003A (:), then:
        if (c == ':') {
          // If compress is non-null, IPv6-multiple-compression validation error, return failure.
          if (compress != null) {
            throw new InvalidUrlException("IPv6 address is compressed in more than one spot.");
          }
          // Increase pointer and pieceIndex by 1, set compress to pieceIndex, and then continue.
          pointer++;
          pieceIndex++;
          compress = pieceIndex;
          c = (pointer < inputLength) ? input.charAt(pointer) : EOF;
          continue;
        }
        // Let value and length be 0.
        int value = 0;
        int length = 0;
        // While length is less than 4 and c is an ASCII hex digit, set value to value × 0x10 + c interpreted as hexadecimal number, and increase pointer and length by 1.
        while (length < 4 && isAsciiHexDigit(c)) {
          int cHex = Character.digit(c, 16);
          value = (value * 0x10) + cHex;
          pointer++;
          length++;
          c = (pointer < inputLength) ? input.charAt(pointer) : EOF;
        }
        // If c is U+002E (.), then:
        if (c == '.') {
          // If length is 0, IPv4-in-IPv6-invalid-code-point validation error, return failure.
          if (length == 0) {
            throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv4 part is empty.");
          }
          // Decrease pointer by length.
          pointer -= length;
          // If pieceIndex is greater than 6, IPv4-in-IPv6-too-many-pieces validation error, return failure.
          if (pieceIndex > 6) {
            throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv6 address has more than 6 pieces.");
          }
          // Let numbersSeen be 0.
          int numbersSeen = 0;
          c = (pointer < inputLength) ? input.charAt(pointer) : EOF;
          // While c is not the EOF code point:
          while (c != EOF) {
            // Let ipv4Piece be null.
            Integer ipv4Piece = null;
            // If numbersSeen is greater than 0, then:
            if (numbersSeen > 0) {
              // If c is a U+002E (.) and numbersSeen is less than 4, then increase pointer by 1.
              if (c == '.' && numbersSeen < 4) {
                pointer++;
                c = (pointer < inputLength) ? input.charAt(pointer) : EOF;
              }
              // Otherwise, IPv4-in-IPv6-invalid-code-point validation error, return failure.
              else {
                throw new InvalidUrlException("IPv6 address with IPv4 address syntax: " +
                        "IPv4 part is empty or contains a non-ASCII digit.");
              }
            }
            // If c is not an ASCII digit, IPv4-in-IPv6-invalid-code-point validation error, return failure.
            if (!isAsciiDigit(c)) {
              throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv4 part contains a non-ASCII digit.");
            }
            // While c is an ASCII digit:
            while (isAsciiDigit(c)) {
              // Let number be c interpreted as decimal number.
              int number = Character.digit(c, 10);
              // If ipv4Piece is null, then set ipv4Piece to number.
              if (ipv4Piece == null) {
                ipv4Piece = number;
              }
              // Otherwise, if ipv4Piece is 0, IPv4-in-IPv6-invalid-code-point validation error, return failure.
              else if (ipv4Piece == 0) {
                throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv4 part contains a non-ASCII digit.");
              }
              // Otherwise, set ipv4Piece to ipv4Piece × 10 + number.
              else {
                ipv4Piece = ipv4Piece * 10 + number;
              }
              // If ipv4Piece is greater than 255, IPv4-in-IPv6-out-of-range-part validation error, return failure.
              if (ipv4Piece > 255) {
                throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv4 part exceeds 255.");
              }
              // Increase pointer by 1.
              pointer++;
              c = (pointer < inputLength) ? input.charAt(pointer) : EOF;
            }
            // Set address[pieceIndex] to address[pieceIndex] × 0x100 + ipv4Piece.
            address[pieceIndex] = address[pieceIndex] * 0x100 + (ipv4Piece != null ? ipv4Piece : 0);
            // Increase numbersSeen by 1.
            numbersSeen++;
            // If numbersSeen is 2 or 4, then increase pieceIndex by 1.
            if (numbersSeen == 2 || numbersSeen == 4) {
              pieceIndex++;
            }
            c = (pointer < inputLength) ? input.charAt(pointer) : EOF;
          }
          // If numbersSeen is not 4, IPv4-in-IPv6-too-few-parts validation error, return failure.
          if (numbersSeen != 4) {
            throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv4 address contains too few parts.");
          }
          // Break.
          break;
        }
        // Otherwise, if c is U+003A (:):
        else if (c == ':') {
          // Increase pointer by 1.
          pointer++;
          c = (pointer < inputLength) ? input.charAt(pointer) : EOF;
          // If c is the EOF code point, IPv6-invalid-code-point validation error, return failure.
          if (c == EOF) {
            throw new InvalidUrlException("IPv6 address unexpectedly ends.");
          }
        }
        // Otherwise, if c is not the EOF code point, IPv6-invalid-code-point validation error, return failure.
        else if (c != EOF) {
          throw new InvalidUrlException("IPv6 address contains \"%s\", which is neither an ASCII hex digit nor a ':'.".formatted(Character.toString(c)));
        }
        // Set address[pieceIndex] to value.
        address[pieceIndex] = value;
        // Increase pieceIndex by 1.
        pieceIndex++;
      }
      // If compress is non-null, then:
      if (compress != null) {
        // Let swaps be pieceIndex − compress.
        int swaps = pieceIndex - compress;
        // Set pieceIndex to 7.
        pieceIndex = 7;
        // While pieceIndex is not 0 and swaps is greater than 0, swap address[pieceIndex] with address[compress + swaps − 1], and then decrease both pieceIndex and swaps by 1.
        while (pieceIndex != 0 && swaps > 0) {
          int tmp = address[pieceIndex];
          address[pieceIndex] = address[compress + swaps - 1];
          address[compress + swaps - 1] = tmp;
          pieceIndex--;
          swaps--;
        }
      }
      // Otherwise, if compress is null and pieceIndex is not 8, IPv6-too-few-pieces validation error, return failure.
      else if (compress == null && pieceIndex != 8) {
        throw new InvalidUrlException("An uncompressed IPv6 address contains fewer than 8 pieces.");
      }
      // Return address.
      return new Ipv6Address(address);
    }

    /**
     * The IPv6 serializer takes an IPv6 address {@code address} and then runs these steps. They return an ASCII string.
     */
    private static String serialize(int[] address) {
      // Let output be the empty string.
      StringBuilder output = new StringBuilder();
      // Let compress be an index to the first IPv6 piece in the first longest sequences of address’s IPv6 pieces that are 0.
      int compress = longestSequenceOf0Pieces(address);
      // Let ignore0 be false.
      boolean ignore0 = false;
      // For each pieceIndex in the range 0 to 7, inclusive:
      for (int pieceIndex = 0; pieceIndex <= 7; pieceIndex++) {
        // If ignore0 is true and address[pieceIndex] is 0, then continue.
        if (ignore0 && address[pieceIndex] == 0) {
          continue;
        }
        // Otherwise, if ignore0 is true, set ignore0 to false.
        else if (ignore0) {
          ignore0 = false;
        }
        // If compress is pieceIndex, then:
        if (compress == pieceIndex) {
          // Let separator be "::" if pieceIndex is 0, and U+003A (:) otherwise.
          String separator = (pieceIndex == 0) ? "::" : ":";
          // Append separator to output.
          output.append(separator);
          // Set ignore0 to true and continue.
          ignore0 = true;
          continue;
        }
        // Append address[pieceIndex], represented as the shortest possible lowercase hexadecimal number, to output.
        output.append(Integer.toHexString(address[pieceIndex]));
        // If pieceIndex is not 7, then append U+003A (:) to output.
        if (pieceIndex != 7) {
          output.append(':');
        }
      }
      // Return output.
      return output.toString();
    }

    private static int longestSequenceOf0Pieces(int[] pieces) {
      int longestStart = -1;
      int longestLength = -1;
      int start = -1;
      for (int i = 0; i < pieces.length + 1; i++) {
        if (i < pieces.length && pieces[i] == 0) {
          if (start < 0) {
            start = i;
          }
        }
        else if (start >= 0) {
          int length = i - start;
          if (length > longestLength) {
            longestStart = start;
            longestLength = length;
          }
          start = -1;
        }
      }
      // If there is no sequence of address’s IPv6 pieces that are 0 that is longer than 1, then set compress to null.
      if (longestLength > 1) {
        return longestStart;
      }
      else {
        return -1;
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      else if (obj instanceof Ipv6Address other) {
        return Arrays.equals(this.pieces, other.pieces);
      }
      else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(this.pieces);
    }

    @Override
    public String toString() {
      return this.string;
    }
  }

  sealed interface Port permits StringPort, IntPort {

  }

  static final class StringPort implements Port {

    private final String port;

    public StringPort(String port) {
      this.port = port;
    }

    public String value() {
      return this.port;
    }

    @Override
    public String toString() {
      return this.port;
    }
  }

  static final class IntPort implements Port {

    private final int port;

    public IntPort(int port) {
      this.port = port;
    }

    public int value() {
      return this.port;
    }

    @Override
    public String toString() {
      return Integer.toString(this.port);
    }

  }

  sealed interface Path permits PathSegment, PathSegments {

    void append(String s);

    boolean isEmpty();

    void shorten(String scheme);

    boolean isOpaque();

    Path clone();
  }

  static final class PathSegment implements Path {

    private final StringBuilder segment;

    @Nullable
    String segmentString;

    PathSegment(String segment) {
      this.segment = new StringBuilder(segment);
    }

    public String segment() {
      String result = this.segmentString;
      if (result == null) {
        result = this.segment.toString();
        this.segmentString = result;
      }
      return result;
    }

    @Override
    public void append(String s) {
      this.segmentString = null;
      this.segment.append(s);
    }

    @Override
    public boolean isEmpty() {
      return this.segment.isEmpty();
    }

    @Override
    public void shorten(String scheme) {
      throw new IllegalStateException("Opaque path not expected");
    }

    @Override
    public boolean isOpaque() {
      return true;
    }

    @Override
    public Path clone() {
      return new PathSegment(segment());
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      else if (o instanceof PathSegment other) {
        return segment().equals(other.segment());
      }
      else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return segment().hashCode();
    }

    @Override
    public String toString() {
      return segment();
    }
  }

  static final class PathSegments implements Path {

    private final List<PathSegment> segments;

    public PathSegments() {
      this.segments = new ArrayList<>();
    }

    public PathSegments(List<PathSegment> segments) {
      this.segments = new ArrayList<>(segments);
    }

    @Override
    public void append(String segment) {
      this.segments.add(new PathSegment(segment));
    }

    public int size() {
      return this.segments.size();
    }

    public String get(int i) {
      return this.segments.get(i).segment();
    }

    @Override
    public boolean isEmpty() {
      return this.segments.isEmpty();
    }

    @Override
    public void shorten(String scheme) {
      int size = size();
      if ("file".equals(scheme) && size == 1 && isWindowsDriveLetter(get(0), true)) {
        return;
      }
      if (!isEmpty()) {
        this.segments.remove(size - 1);
      }
    }

    @Override
    public boolean isOpaque() {
      return false;
    }

    @Override
    public Path clone() {
      return new PathSegments(this.segments);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      else if (o instanceof PathSegments other) {
        return this.segments.equals(other.segments);
      }
      else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return this.segments.hashCode();
    }

    @Override
    public String toString() {
      StringBuilder output = new StringBuilder();
      for (PathSegment segment : this.segments) {
        output.append(segment);
      }
      return output.toString();
    }

  }

  private record ParseIpv4NumberResult(int number, boolean validationError) {

  }

}
