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

package infra.util;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiPredicate;

import static infra.util.MimeType.MULTIPART_TYPE;

/**
 * Miscellaneous {@link MimeType} utility methods.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Dimitrios Liapis
 * @author Brian Clozel
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-12-08 19:24
 */
public abstract class MimeTypeUtils {

  private static final byte[] BOUNDARY_CHARS = new byte[] {
          '-', '_', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
          'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
          'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
          'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
          'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
  };

  private static final ConcurrentLruCache<String, MimeType> cachedMimeTypes =
          new ConcurrentLruCache<>(64, MimeTypeUtils::parseMimeTypeInternal);

  private static volatile @Nullable Random random;

  static final BitSet TOKEN;

  static {
    // variable names refer to RFC 2616, section 2.2
    BitSet ctl = new BitSet(128);
    for (int i = 0; i <= 31; i++) {
      ctl.set(i);
    }
    ctl.set(127);

    BitSet separators = new BitSet(128);
    separators.set('(');
    separators.set(')');
    separators.set('<');
    separators.set('>');
    separators.set('@');
    separators.set(',');
    separators.set(';');
    separators.set(':');
    separators.set('\\');
    separators.set('\"');
    separators.set('/');
    separators.set('[');
    separators.set(']');
    separators.set('?');
    separators.set('=');
    separators.set('{');
    separators.set('}');
    separators.set(' ');
    separators.set('\t');

    TOKEN = new BitSet(128);
    TOKEN.set(0, 128);
    TOKEN.andNot(ctl);
    TOKEN.andNot(separators);
  }

  /**
   * Parse the given String into a single {@code MimeType}. Recently parsed
   * {@code MimeType} are cached for further retrieval.
   *
   * @param mimeType the string to parse
   * @return the mime type
   * @throws InvalidMimeTypeException if the string cannot be parsed
   */
  public static MimeType parseMimeType(String mimeType) {
    if (StringUtils.isEmpty(mimeType)) {
      throw new InvalidMimeTypeException(mimeType, "'mimeType' must not be empty");
    }
    // do not cache multipart mime types with random boundaries
    if (mimeType.startsWith(MULTIPART_TYPE)) {
      return parseMimeTypeInternal(mimeType);
    }
    return cachedMimeTypes.get(mimeType);
  }

  private static MimeType parseMimeTypeInternal(String mimeType) {
    return new MimeTypeParser(mimeType).parse();
  }

  /**
   * Parse the comma-separated string into a list of {@code MimeType} objects.
   *
   * @param mimeTypes the string to parse
   * @return the list of mime types
   * @throws InvalidMimeTypeException if the string cannot be parsed
   */
  public static List<MimeType> parseMimeTypes(String mimeTypes) {
    if (StringUtils.isEmpty(mimeTypes)) {
      return Collections.emptyList();
    }

    // Avoid using java.util.stream.Stream in hot paths
    List<String> tokenizedTypes = MimeTypeUtils.tokenize(mimeTypes);
    ArrayList<MimeType> result = new ArrayList<>(tokenizedTypes.size());
    for (String type : tokenizedTypes) {
      if (StringUtils.isNotEmpty(type)) {
        result.add(parseMimeType(type));
      }
    }
    return result;
  }

  /**
   * Tokenize the given comma-separated string of {@code MimeType} objects into a
   * {@code List<String>}. Unlike simple tokenization by ",", this method takes
   * into account quoted parameters.
   *
   * @param mimeTypes the string to tokenize
   * @return the list of tokens
   */
  public static List<String> tokenize(String mimeTypes) {
    if (StringUtils.isEmpty(mimeTypes)) {
      return Collections.emptyList();
    }
    // Without quoted parameter values, there is no need to track quotes
    if (mimeTypes.indexOf('"') == -1) {
      return splitByComma(mimeTypes);
    }
    ArrayList<String> tokens = new ArrayList<>();
    boolean inQuotes = false;
    int startIndex = 0;
    int i = 0;
    final int length = mimeTypes.length();
    while (i < length) {
      switch (mimeTypes.charAt(i)) {
        case '"' -> inQuotes = !inQuotes;
        case ',' -> {
          if (!inQuotes) {
            String trimmed = mimeTypes.substring(startIndex, i).trim();
            if (!trimmed.isEmpty()) {
              tokens.add(trimmed);
            }
            startIndex = i + 1;
          }
        }
        case '\\' -> i++;
      }
      i++;
    }
    String trimmed = mimeTypes.substring(startIndex).trim();
    if (!trimmed.isEmpty()) {
      tokens.add(trimmed);
    }
    return tokens;
  }

  private static List<String> splitByComma(String mimeTypes) {
    ArrayList<String> tokens = new ArrayList<>();
    int startIndex = 0;
    int commaIndex;
    while ((commaIndex = mimeTypes.indexOf(',', startIndex)) != -1) {
      String trimmed = mimeTypes.substring(startIndex, commaIndex).trim();
      if (!trimmed.isEmpty()) {
        tokens.add(trimmed);
      }
      startIndex = commaIndex + 1;
    }
    String trimmed = mimeTypes.substring(startIndex).trim();
    if (!trimmed.isEmpty()) {
      tokens.add(trimmed);
    }
    return tokens;
  }

  /**
   * Return a string representation of the given list of {@code MimeType} objects.
   *
   * @param mimeTypes the string to parse
   * @return the list of mime types
   * @throws IllegalArgumentException if the String cannot be parsed
   */
  public static String toString(Collection<? extends MimeType> mimeTypes) {
    StringBuilder builder = new StringBuilder();
    for (Iterator<? extends MimeType> iterator = mimeTypes.iterator(); iterator.hasNext(); ) {
      MimeType mimeType = iterator.next();
      mimeType.appendTo(builder);
      if (iterator.hasNext()) {
        builder.append(", ");
      }
    }
    return builder.toString();
  }

  /**
   * Sorts the given list of {@code MimeType} objects by
   * {@linkplain MimeType#isMoreSpecific(MimeType) specificity}.
   *
   * <p>Because of the computational cost, this method throws an exception
   * when the given list contains too many elements.
   *
   * @param mimeTypes the list of mime types to be sorted
   * @throws IllegalArgumentException if {@code mimeTypes} contains more
   * than 50 elements
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics
   * and Content, section 5.3.2</a>
   * @see MimeType#isMoreSpecific(MimeType)
   */
  public static void sortBySpecificity(List<? extends MimeType> mimeTypes) {
    Assert.notNull(mimeTypes, "'mimeTypes' is required");
    if (mimeTypes.size() > 1) {
      if (mimeTypes.size() > 50) {
        throw new InvalidMimeTypeException(mimeTypes.toString(), "Too many elements");
      }
      bubbleSort(mimeTypes, MimeType::isLessSpecific);
    }
  }

  static <T> void bubbleSort(List<T> list, BiPredicate<? super T, ? super T> swap) {
    int len = list.size();
    for (int i = 0; i < len; i++) {
      for (int j = 1; j < len - i; j++) {
        T prev = list.get(j - 1);
        T cur = list.get(j);
        if (swap.test(prev, cur)) {
          list.set(j, prev);
          list.set(j - 1, cur);
        }
      }
    }
  }

  /**
   * Generate a random MIME boundary as bytes, often used in multipart mime types.
   */
  public static byte[] generateMultipartBoundary() {
    final Random randomToUse = initRandom();
    final byte[] boundary = new byte[randomToUse.nextInt(11) + 30];
    final byte[] boundaryChars = BOUNDARY_CHARS;
    final int length = boundaryChars.length;
    for (int i = 0; i < boundary.length; i++) {
      boundary[i] = boundaryChars[randomToUse.nextInt(length)];
    }
    return boundary;
  }

  /**
   * Generate a random MIME boundary as String, often used in multipart mime
   * types.
   */
  public static String generateMultipartBoundaryString() {
    return new String(generateMultipartBoundary(), StandardCharsets.US_ASCII);
  }

  /**
   * Lazily initialize the {@link SecureRandom} for
   * {@link #generateMultipartBoundary()}.
   */
  private static Random initRandom() {
    Random randomToUse = random;
    if (randomToUse == null) {
      synchronized(MimeTypeUtils.class) {
        randomToUse = random;
        if (randomToUse == null) {
          randomToUse = new SecureRandom();
          random = randomToUse;
        }
      }
    }
    return randomToUse;
  }

  private static final class MimeTypeParser {

    private final String input;

    private int index = 0;

    private int mark = 0;

    private String type = "";

    private String subtype = "";

    private @Nullable Map<String, String> parameters;

    private @Nullable String paramName;

    private @Nullable MimeType parsed;

    private MimeTypeParser(String input) {
      this.input = input;
    }

    /**
     * Parse the entire input as exactly one MIME type: any leftover
     * content the state machine cannot make sense of (for example, a
     * comma-separated second MIME type) is rejected rather than ignored.
     */
    private MimeType parse() {
      ParserState state = ParserState.INITIAL;
      for (; this.index < this.input.length(); this.index++) {
        char c = this.input.charAt(this.index);
        state = state.process(c, this);
      }
      state.onEof(this);
      if (this.parsed == null) {
        throw new InvalidMimeTypeException(this.input, "'mimeType' must not be empty");
      }
      return this.parsed;
    }

    /**
     * Resolve a type for which no '/' was found before a terminator (or
     * the end of input) was reached. The only valid case is the bare
     * {@code *} some clients (for example, {@code java.net.HttpURLConnection})
     * send as shorthand for the {@code *; q=.2}-style wildcard
     * (&#42;/&#42;) Accept header; anything else is rejected.
     */
    private void resolveBareType(String candidate) {
      if (!MimeType.WILDCARD_TYPE.equals(candidate)) {
        throw new InvalidMimeTypeException(this.input, "does not contain '/'");
      }
      this.type = MimeType.WILDCARD_TYPE;
      this.subtype = MimeType.WILDCARD_TYPE;
    }

    private void putParameter(String name, String value) {
      if (this.parameters == null) {
        this.parameters = new LinkedHashMap<>(4);
      }
      if (this.parameters.put(name, value) != null) {
        throw new InvalidMimeTypeException(this.input, "duplicate parameter '%s=%s'".formatted(name, value));
      }
    }

    private void emitMimeType() {
      this.parsed = buildMimeType();
    }

    private MimeType buildMimeType() {
      if (MimeType.WILDCARD_TYPE.equals(this.type) && !MimeType.WILDCARD_TYPE.equals(this.subtype)) {
        throw new InvalidMimeTypeException(this.input, "wildcard type is legal only in '*/*' (all mime types)");
      }
      try {
        return new MimeType(this.type, this.subtype, this.parameters);
      }
      catch (UnsupportedCharsetException ex) {
        throw new InvalidMimeTypeException(this.input, "unsupported charset '" + ex.getCharsetName() + "'");
      }
      catch (IllegalArgumentException ex) {
        throw new InvalidMimeTypeException(this.input, ex.getMessage());
      }
    }
  }

  enum ParserState {

    INITIAL {
      @Override
      ParserState process(char c, MimeTypeParser parser) {
        if (c == ' ' || c == '\t') {
          return this;
        }
        parser.mark = parser.index;
        return TYPE;
      }
    },

    TYPE {
      @Override
      ParserState process(char c, MimeTypeParser parser) {
        if (c == '/') {
          parser.type = parser.input.substring(parser.mark, parser.index);
          if (parser.type.isEmpty()) {
            throw new InvalidMimeTypeException(parser.input, "'type' must not be empty");
          }
          parser.mark = parser.index + 1;
          return SUBTYPE;
        }
        if (c == ';' || c == ' ' || c == '\t') {
          parser.resolveBareType(parser.input.substring(parser.mark, parser.index));
          return WHITESPACE;
        }
        return this;
      }

      @Override
      void onEof(MimeTypeParser parser) {
        parser.resolveBareType(parser.input.substring(parser.mark));
        parser.emitMimeType();
      }
    },

    SUBTYPE {
      @Override
      ParserState process(char c, MimeTypeParser parser) {
        if (c == ';' || c == ' ' || c == '\t') {
          parser.subtype = parser.input.substring(parser.mark, parser.index);
          return WHITESPACE;
        }
        return this;
      }

      @Override
      void onEof(MimeTypeParser parser) {
        parser.subtype = parser.input.substring(parser.mark);
        parser.emitMimeType();
      }
    },

    WHITESPACE {
      @Override
      ParserState process(char c, MimeTypeParser parser) {
        if (c == ' ' || c == '\t' || c == ';') {
          return this;
        }
        parser.mark = parser.index;
        return PARAM_NAME;
      }

      @Override
      void onEof(MimeTypeParser parser) {
        if (!parser.type.isEmpty()) {
          parser.emitMimeType();
        }
      }
    },

    PARAM_NAME {
      @Override
      ParserState process(char c, MimeTypeParser parser) {
        if (c == '=') {
          parser.paramName = parser.input.substring(parser.mark, parser.index);
          return PARAM_VALUE_START;
        }
        if (c == ' ' || c == '\t') {
          parser.paramName = parser.input.substring(parser.mark, parser.index);
          return PARAM_NAME_END;
        }
        return this;
      }
    },

    PARAM_NAME_END {
      @Override
      ParserState process(char c, MimeTypeParser parser) {
        if (c == ' ' || c == '\t') {
          return this;
        }
        if (c == '=') {
          return PARAM_VALUE_START;
        }
        throw new InvalidMimeTypeException(parser.input, "Unexpected character '" + c + "' after parameter name");
      }
    },

    PARAM_VALUE_START {
      @Override
      ParserState process(char c, MimeTypeParser parser) {
        if (c == ' ' || c == '\t') {
          return this;
        }
        if (c == '"') {
          parser.mark = parser.index;
          return PARAM_VALUE_QUOTED;
        }
        parser.mark = parser.index;
        return PARAM_VALUE_TOKEN;
      }
    },

    PARAM_VALUE_TOKEN {
      @Override
      ParserState process(char c, MimeTypeParser parser) {
        if (c == ';' || c == ' ' || c == '\t') {
          extractParameter(parser, parser.input.substring(parser.mark, parser.index));
          return WHITESPACE;
        }
        return this;
      }

      @Override
      void onEof(MimeTypeParser parser) {
        extractParameter(parser, parser.input.substring(parser.mark));
        parser.emitMimeType();
      }
    },

    PARAM_VALUE_QUOTED {
      @Override
      ParserState process(char c, MimeTypeParser parser) {
        if (c == '"') {
          extractParameter(parser, parser.input.substring(parser.mark, parser.index + 1));
          return WHITESPACE;
        }
        if (c == '\\') {
          return PARAM_VALUE_ESCAPED;
        }
        return this;
      }

      @Override
      void onEof(MimeTypeParser parser) {
        extractParameter(parser, parser.input.substring(parser.mark));
        parser.emitMimeType();
      }
    },

    PARAM_VALUE_ESCAPED {
      @Override
      ParserState process(char c, MimeTypeParser parser) {
        return PARAM_VALUE_QUOTED;
      }

      @Override
      void onEof(MimeTypeParser parser) {
        extractParameter(parser, parser.input.substring(parser.mark));
        parser.emitMimeType();
      }
    };

    private static void extractParameter(MimeTypeParser parser, String input) {
      Assert.hasText(parser.paramName, "'paramName' must not be empty");
      parser.putParameter(parser.paramName, input);
    }

    abstract ParserState process(char c, MimeTypeParser parser);

    void onEof(MimeTypeParser parser) {
      // Only specific states need to flush here.
    }
  }

}
