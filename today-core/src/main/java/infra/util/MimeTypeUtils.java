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

package infra.util;

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
import java.util.Random;
import java.util.function.BiPredicate;

import infra.lang.Assert;
import infra.lang.Nullable;

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

  @Nullable
  private static volatile Random random;

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
    return cachedMimeTypes.get(mimeType);
  }

  private static MimeType parseMimeTypeInternal(String mimeType) {
    int index = mimeType.indexOf(';');
    String fullType = (index >= 0 ? mimeType.substring(0, index) : mimeType).trim();
    if (fullType.isEmpty()) {
      throw new InvalidMimeTypeException(mimeType, "'mimeType' must not be empty");
    }

    // java.net.HttpURLConnection returns a *; q=.2 Accept header
    final String wildcardType = MimeType.WILDCARD_TYPE;
    if (wildcardType.equals(fullType)) {
      fullType = "*/*";
    }
    int subIndex = fullType.indexOf('/');
    if (subIndex == -1) {
      throw new InvalidMimeTypeException(mimeType, "does not contain '/'");
    }
    if (subIndex == fullType.length() - 1) {
      throw new InvalidMimeTypeException(mimeType, "does not contain subtype after '/'");
    }
    String type = fullType.substring(0, subIndex);
    String subtype = fullType.substring(subIndex + 1);
    if (wildcardType.equals(type) && !wildcardType.equals(subtype)) {
      throw new InvalidMimeTypeException(mimeType, "wildcard type is legal only in '*/*' (all mime types)");
    }

    LinkedHashMap<String, String> parameters = null;
    do {
      int nextIndex = index + 1;
      boolean quoted = false;
      while (nextIndex < mimeType.length()) {
        char ch = mimeType.charAt(nextIndex);
        if (ch == ';') {
          if (!quoted) {
            break;
          }
        }
        else if (ch == '"') {
          quoted = !quoted;
        }
        nextIndex++;
      }
      String parameter = mimeType.substring(index + 1, nextIndex).trim();
      if (!parameter.isEmpty()) {
        if (parameters == null) {
          parameters = new LinkedHashMap<>(4);
        }
        int eqIndex = parameter.indexOf('=');
        if (eqIndex >= 0) {
          String attribute = parameter.substring(0, eqIndex).trim();
          String value = parameter.substring(eqIndex + 1).trim();
          parameters.put(attribute, value);
        }
      }
      index = nextIndex;
    }
    while (index < mimeType.length());

    try {
      return new MimeType(type, subtype, parameters);
    }
    catch (UnsupportedCharsetException ex) {
      throw new InvalidMimeTypeException(mimeType, "unsupported charset '%s'".formatted(ex.getCharsetName()));
    }
    catch (IllegalArgumentException ex) {
      throw new InvalidMimeTypeException(mimeType, ex.getMessage());
    }
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
            tokens.add(mimeTypes.substring(startIndex, i));
            startIndex = i + 1;
          }
        }
        case '\\' -> i++;
      }
      i++;
    }
    tokens.add(mimeTypes.substring(startIndex));
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

}
