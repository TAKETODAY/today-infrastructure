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
package cn.taketoday.util;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;

/**
 * @author TODAY 2018-06-26 21:19:09
 */
public abstract class StringUtils {

  public static final String TOP_PATH = "..";
  public static final String CURRENT_PATH = ".";
  public static final String FOLDER_SEPARATOR = "/";
  public static final String WINDOWS_FOLDER_SEPARATOR = "\\";
  public static final char EXTENSION_SEPARATOR = Constant.PACKAGE_SEPARATOR;

  private static final int caseDiff = ('a' - 'A');
  private static final BitSet dontNeedEncoding;
  private static final Random random = new Random();

  static {

    /* The list of characters that are not encoded has been determined as follows:
     *
     * RFC 2396 states: ----- Data characters that are allowed in a URI but do not
     * have a reserved purpose are called unreserved. These include upper and lower
     * case letters, decimal digits, and a limited set of punctuation marks and
     * symbols.
     *
     * unreserved = alphanum | mark
     *
     * mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
     *
     * Unreserved characters can be escaped without changing the semantics of the
     * URI, but this should not be done unless the URI is being used in a context
     * that does not allow the unescaped character to appear. -----
     *
     * It appears that both Netscape and Internet Explorer escape all special
     * characters from this list with the exception of "-", "_", ".", "*". While it
     * is not clear why they are escaping the other characters, perhaps it is safest
     * to assume that there might be contexts in which the others are unsafe if not
     * escaped. Therefore, we will use the same list. It is also noteworthy that
     * this is consistent with O'Reilly's "HTML: The Definitive Guide" (page 164).
     *
     * As a last note, Intenet Explorer does not encode the "@" character which is
     * clearly not unreserved according to the RFC. We are being consistent with the
     * RFC in this matter, as is Netscape. */
    dontNeedEncoding = new BitSet(256);
    int i;
    for (i = 'a'; i <= 'z'; i++) {
      dontNeedEncoding.set(i);
    }
    for (i = 'A'; i <= 'Z'; i++) {
      dontNeedEncoding.set(i);
    }
    for (i = '0'; i <= '9'; i++) {
      dontNeedEncoding.set(i);
    }
    dontNeedEncoding.set(' '); // encoding a space to a + is done in the encode() method

    dontNeedEncoding.set('-');
    dontNeedEncoding.set('_');
    dontNeedEncoding.set('.');
    dontNeedEncoding.set('*');
  }

  public static boolean isEmpty(CharSequence str) {
    return str == null || str.length() == 0;
  }

  public static boolean isNotEmpty(CharSequence str) {
    return !isEmpty(str);
  }

  /**
   * Split with {@link Constant#SPLIT_REGEXP}
   *
   * @param source source string
   * @return if source is null this will returns
   * {@link Constant#EMPTY_STRING_ARRAY}
   */
  @NonNull
  public static String[] split(String source) {
    if (source == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    return toStringArray(splitAsList(source));
  }

  /**
   * Split with {@link Constant#SPLIT_REGEXP}
   *
   * @param source source string
   * @return if source is null this will returns
   * {@link Collections#emptyList()}
   * @see Collections#emptyList()
   * @since 4.0
   */
  @NonNull
  public static List<String> splitAsList(String source) {
    if (source == null) {
      return Collections.emptyList();
    }
    if (source.isEmpty()) {
      return Collections.singletonList(source);
    }
    final ArrayList<String> splitList = new ArrayList<>();

    int idx = 0;
    int start = 0;
    final char[] chars = source.toCharArray();
    for (final char c : chars) {
      if (isSplitable(c)) {
        splitList.add(new String(chars, start, idx - start));
        start = idx + 1;
      }
      idx++;
    }
    if (idx != start && idx == source.length()) { // 最后一次分割
      splitList.add(new String(chars, start, idx - start));
    }
    else if (splitList.isEmpty()) {
      return Collections.singletonList(source);
    }
    return splitList;
  }

  private static boolean isSplitable(final char c) {
    return c == ',' || c == ';';
  }

  /**
   * Split a {@code String} at the first occurrence of the delimiter.
   * Does not include the delimiter in the result.
   *
   * @param toSplit the string to split (potentially {@code null} or empty)
   * @param delimiter to split the string up with (potentially {@code null} or empty)
   * @return a two element array with index 0 being before the delimiter, and
   * index 1 being after the delimiter (neither element includes the delimiter);
   * or {@code null} if the delimiter wasn't found in the given input {@code String}
   */
  @Nullable
  public static String[] split(@Nullable String toSplit, @Nullable String delimiter) {
    if (isEmpty(toSplit) || isEmpty(delimiter)) {
      return null;
    }
    int offset = toSplit.indexOf(delimiter);
    if (offset < 0) {
      return null;
    }

    String beforeDelimiter = toSplit.substring(0, offset);
    String afterDelimiter = toSplit.substring(offset + delimiter.length());
    return new String[] { beforeDelimiter, afterDelimiter };
  }

  /**
   * Decodes an {@code application/x-www-form-urlencoded} string using
   * a specific default charset {@link Constant#DEFAULT_CHARSET}.
   * The supplied charset is used to determine
   * what characters are represented by any consecutive sequences of the
   * form "<i>{@code %xy}</i>".
   * <p>
   * <em><strong>Note:</strong> The <a href=
   * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
   * World Wide Web Consortium Recommendation</a> states that
   * UTF-8 should be used. Not doing so may introduce
   * incompatibilities.</em>
   *
   * @param s the {@code String} to decode
   * @return the newly decoded {@code String}
   * @throws NullPointerException if {@code s} or {@code charset} is {@code null}
   * @throws IllegalArgumentException if the implementation encounters illegal
   * characters
   * @implNote This implementation will throw an {@link java.lang.IllegalArgumentException}
   * when illegal strings are encountered.
   * @since 3.0
   */
  public static String decodeURL(String s) {
    return decodeURL(s, Constant.DEFAULT_CHARSET);
  }

  /**
   * Decodes an {@code application/x-www-form-urlencoded} string using
   * a specific {@linkplain java.nio.charset.Charset Charset}.
   * The supplied charset is used to determine
   * what characters are represented by any consecutive sequences of the
   * form "<i>{@code %xy}</i>".
   * <p>
   * <em><strong>Note:</strong> The <a href=
   * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
   * World Wide Web Consortium Recommendation</a> states that
   * UTF-8 should be used. Not doing so may introduce
   * incompatibilities.</em>
   *
   * @param s the {@code String} to decode
   * @param charset the given charset
   * @return the newly decoded {@code String}
   * @throws NullPointerException if {@code s} or {@code charset} is {@code null}
   * @throws IllegalArgumentException if the implementation encounters illegal
   * characters
   * @implNote This implementation will throw an {@link java.lang.IllegalArgumentException}
   * when illegal strings are encountered.
   * @since 3.0
   */
  public static String decodeURL(String s, Charset charset) {
    Assert.notNull(charset, "Charset cannot be null");
    boolean needToChange = false;
    int numChars = s.length();
    StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
    int i = 0;

    char c;
    byte[] bytes = null;
    while (i < numChars) {
      c = s.charAt(i);
      switch (c) {
        case '+':
          sb.append(' ');
          i++;
          needToChange = true;
          break;
        case '%':
          /*
           * Starting with this instance of %, process all
           * consecutive substrings of the form %xy. Each
           * substring %xy will yield a byte. Convert all
           * consecutive  bytes obtained this way to whatever
           * character(s) they represent in the provided
           * encoding.
           */
          try {
            // (numChars-i)/3 is an upper bound for the number
            // of remaining bytes
            if (bytes == null)
              bytes = new byte[(numChars - i) / 3];
            int pos = 0;

            while (((i + 2) < numChars) && (c == '%')) {
              int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
              if (v < 0)
                throw new IllegalArgumentException(
                        "URLDecoder: Illegal hex characters in escape (%) pattern - negative value");
              bytes[pos++] = (byte) v;
              i += 3;
              if (i < numChars)
                c = s.charAt(i);
            }

            // A trailing, incomplete byte encoding such as
            // "%x" will cause an exception to be thrown
            if ((i < numChars) && (c == '%'))
              throw new IllegalArgumentException(
                      "URLDecoder: Incomplete trailing escape (%) pattern");

            sb.append(new String(bytes, 0, pos, charset));
          }
          catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "URLDecoder: Illegal hex characters in escape (%) pattern - "
                            + e.getMessage(), e);
          }
          needToChange = true;
          break;
        default:
          sb.append(c);
          i++;
          break;
      }
    }

    return (needToChange ? sb.toString() : s);
  }

  /**
   * Translates a string into {@code application/x-www-form-urlencoded}
   * format using a default Charset {@link Constant#DEFAULT_CHARSET}.
   * This method uses the supplied charset to obtain the bytes for unsafe
   * characters.
   * <p>
   * <em><strong>Note:</strong> The <a href=
   * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
   * World Wide Web Consortium Recommendation</a> states that
   * UTF-8 should be used. Not doing so may introduce incompatibilities.</em>
   *
   * @param s {@code String} to be translated.
   * @return the translated {@code String}.
   * @throws NullPointerException if {@code s} or {@code charset} is {@code null}.
   */
  public static String encodeURL(String s) {
    return encodeURL(s, Constant.DEFAULT_CHARSET);
  }

  /**
   * Translates a string into {@code application/x-www-form-urlencoded}
   * format using a specific {@linkplain java.nio.charset.Charset Charset}.
   * This method uses the supplied charset to obtain the bytes for unsafe
   * characters.
   * <p>
   * <em><strong>Note:</strong> The <a href=
   * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
   * World Wide Web Consortium Recommendation</a> states that
   * UTF-8 should be used. Not doing so may introduce incompatibilities.</em>
   *
   * @param s {@code String} to be translated.
   * @param charset the given charset
   * @return the translated {@code String}.
   * @throws NullPointerException if {@code s} or {@code charset} is {@code null}.
   * @since 3.0
   */
  public static String encodeURL(String s, Charset charset) {
    Assert.notNull(charset, "Charset cannot be null");
    boolean needToChange = false;
    final int length = s.length();
    final StringBuilder out = new StringBuilder(length);
    final CharArrayWriter charArrayWriter = new CharArrayWriter();

    final BitSet dontNeedEncoding = StringUtils.dontNeedEncoding;
    final int caseDiff = StringUtils.caseDiff;

    for (int i = 0; i < length; ) {
      int c = s.charAt(i);
      // System.out.println("Examining character: " + c);
      if (dontNeedEncoding.get(c)) {
        if (c == ' ') {
          c = '+';
          needToChange = true;
        }
        // System.out.println("Storing: " + c);
        out.append((char) c);
        i++;
        continue;
      }
      // convert to external encoding before hex conversion
      do {
        charArrayWriter.write(c);
        /* If this character represents the start of a Unicode surrogate pair, then pass
         * in two characters. It's not clear what should be done if a bytes reserved in
         * the surrogate pairs range occurs outside of a legal surrogate pair. For now,
         * just treat it as if it were any other character. */
        if (c >= 0xD800 && c <= 0xDBFF && (i + 1) < length) {
          // System.out.println(Integer.toHexString(c) + " is high surrogate");
          int d = s.charAt(i + 1);
          // System.out.println("\tExamining " + Integer.toHexString(d));
          if (d >= 0xDC00 && d <= 0xDFFF) {
            // System.out.println("\t" + Integer.toHexString(d) + " is low surrogate");
            charArrayWriter.write(d);
            i++;
          }
        }
        i++;
      }
      while (i < length && !dontNeedEncoding.get((c = s.charAt(i))));

      charArrayWriter.flush();
      byte[] ba = new String(charArrayWriter.toCharArray()).getBytes(charset);
      for (final byte b : ba) {
        out.append('%');
        char ch = Character.forDigit((b >> 4) & 0xF, 16);
        // converting to use uppercase letter as part of
        // the hex value if ch is a letter.
        if (Character.isLetter(ch)) {
          ch -= caseDiff;
        }
        out.append(ch);
        ch = Character.forDigit(b & 0xF, 16);
        if (Character.isLetter(ch)) {
          ch -= caseDiff;
        }
        out.append(ch);
      }
      charArrayWriter.reset();
      needToChange = true;
    }
    return (needToChange ? out.toString() : s);
  }

  //---------------------------------------------------------------------
  // Convenience methods for working with String arrays
  //---------------------------------------------------------------------

  /**
   * Tokenize the given {@code String} into a {@code String} array via a
   * {@link StringTokenizer}.
   * <p>Trims tokens and omits empty tokens.
   * <p>The given {@code delimiters} string can consist of any number of
   * delimiter characters. Each of those characters can be used to separate
   * tokens. A delimiter is always a single character; for multi-character
   * delimiters, consider using {@link #delimitedListToStringArray}.
   *
   * @param str the {@code String} to tokenize (potentially {@code null} or empty)
   * @param delimiters the delimiter characters, assembled as a {@code String}
   * (each of the characters is individually considered as a delimiter)
   * @return an array of the tokens
   * @see java.util.StringTokenizer
   * @see String#trim()
   * @see #delimitedListToStringArray
   */
  @NonNull
  public static String[] tokenizeToStringArray(final String str, final String delimiters) {
    return tokenizeToStringArray(str, delimiters, true, true);
  }

  /**
   * Tokenize the given {@code String} into a {@code String} array via a
   * {@link StringTokenizer}.
   * <p>The given {@code delimiters} string can consist of any number of
   * delimiter characters. Each of those characters can be used to separate
   * tokens. A delimiter is always a single character; for multi-character
   * delimiters, consider using {@link #delimitedListToStringArray}.
   *
   * @param str the {@code String} to tokenize (potentially {@code null} or empty)
   * @param delimiters the delimiter characters, assembled as a {@code String}
   * (each of the characters is individually considered as a delimiter)
   * @param trimTokens trim the tokens via {@link String#trim()}
   * @param ignoreEmptyTokens omit empty tokens from the result array
   * (only applies to tokens that are empty after trimming; StringTokenizer
   * will not consider subsequent delimiters as token in the first place).
   * @return an array of the tokens
   * @see java.util.StringTokenizer
   * @see String#trim()
   * @see #delimitedListToStringArray
   */
  @NonNull
  public static String[] tokenizeToStringArray(
          String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {
    if (str == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    StringTokenizer st = new StringTokenizer(str, delimiters);
    ArrayList<String> tokens = new ArrayList<>();
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (trimTokens) {
        token = token.trim();
      }
      if (!ignoreEmptyTokens || token.length() > 0) {
        tokens.add(token);
      }
    }
    return toStringArray(tokens);
  }

  /**
   * Copy the given {@link Collection} into a {@code String} array.
   * <p>The {@code Collection} must contain {@code String} elements only.
   *
   * @param collection the {@code Collection} to copy
   * (potentially {@code null} or empty)
   * @return the resulting {@code String} array
   * @since 4.0
   */
  public static String[] toStringArray(@Nullable Collection<String> collection) {
    if (collection == null) {
      return null;
    }
    return collection.toArray(new String[collection.size()]);
  }

  /**
   * Copy the given {@link Enumeration} into a {@code String} array.
   * <p>The {@code Enumeration} must contain {@code String} elements only.
   *
   * @param enumeration the {@code Enumeration} to copy
   * (potentially {@code null} or empty)
   * @return the resulting {@code String} array
   * @since 4.0
   */
  public static String[] toStringArray(@Nullable Enumeration<String> enumeration) {
    return enumeration == null
            ? Constant.EMPTY_STRING_ARRAY
            : toStringArray(Collections.list(enumeration));
  }

  /**
   * Append the given {@code String} to the given {@code String} array,
   * returning a new array consisting of the input array contents plus
   * the given {@code String}.
   *
   * @param array the array to append to (can be {@code null})
   * @param str the {@code String} to append
   * @return the new array (never {@code null})
   * @since 4.0
   */
  public static String[] addStringToArray(@Nullable String[] array, String str) {
    if (ObjectUtils.isEmpty(array)) {
      return new String[] { str };
    }

    String[] newArr = new String[array.length + 1];
    System.arraycopy(array, 0, newArr, 0, array.length);
    newArr[array.length] = str;
    return newArr;
  }

  /**
   * Sort the given {@code String} array if necessary.
   *
   * @param array the original array (potentially empty)
   * @since 4.0
   */
  public static void sortArray(String[] array) {
    if (ObjectUtils.isNotEmpty(array)) {
      Arrays.sort(array);
    }
  }

  /**
   * Trim the elements of the given {@code String} array, calling
   * {@code String.trim()} on each non-null element.
   *
   * @param array the original {@code String} array (potentially empty)
   * @return the resulting array (of the same size) with trimmed elements
   * @since 4.0
   */
  public static String[] trimArrayElements(String[] array) {
    if (ObjectUtils.isEmpty(array)) {
      return array;
    }

    String[] result = new String[array.length];
    for (int i = 0; i < array.length; i++) {
      String element = array[i];
      result[i] = (element != null ? element.trim() : null);
    }
    return result;
  }

  /**
   * Remove duplicate strings from the given array.
   *
   * @param array the {@code String} array (potentially empty)
   * @return an array without duplicates, in natural sort order
   * @since 4.0
   */
  public static String[] removeDuplicateStrings(String[] array) {
    if (ObjectUtils.isEmpty(array)) {
      return array;
    }

    LinkedHashSet<String> set = new LinkedHashSet<>();
    CollectionUtils.addAll(set, array);
    return toStringArray(set);
  }

  /**
   * Use default delimiter:',' append array to a string
   *
   * @param array Input array object
   */
  public static String arrayToString(Object[] array) {
    return arrayToString(array, ",");
  }

  /**
   * Array to string
   *
   * @param array Input array object
   * @param delimiter Delimiter string
   */
  public static String arrayToString(final Object[] array, final String delimiter) {
    if (array == null) {
      return null;
    }
    final int length = array.length;
    if (length == 1) {
      return array[0].toString();
    }

    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < length; i++) {
      builder.append(array[i]);
      if (i != length - 1) {
        builder.append(delimiter);
      }
    }
    return builder.toString();
  }

  /**
   * {@link Collection} to string
   *
   * @param collection Input {@link Collection} object
   * @since 2.1.7
   */
  public static <T> String collectionToString(final Collection<T> collection) {
    return collectionToString(collection, ",");
  }

  /**
   * {@link Collection} to string
   *
   * @param coll Input {@link Collection} object
   * @param delimiter Delimiter string
   * @since 2.1.7
   */
  public static <T> String collectionToString(final Collection<T> coll, final String delimiter) {
    if (coll == null) {
      return null;
    }
    final int length = coll.size();
    if (length == 1) {
      final T target = coll instanceof List ? ((List<T>) coll).get(0) : coll.iterator().next();
      return target != null ? target.toString() : null;
    }

    final StringBuilder builder = new StringBuilder();

    int i = 0;
    for (T target : coll) {
      builder.append(target);
      if (i++ != length - 1) {
        builder.append(delimiter);
      }
    }

    return builder.toString();
  }

  /**
   * Use {@link UUID} to get random uuid string
   *
   * @return Random uuid string
   */
  public static String getUUIDString() {
    return UUID.randomUUID().toString();
  }

  /**
   * Normalize the path by suppressing sequences like "path/.." and inner simple
   * dots.
   * <p>
   * The result is convenient for path comparison. For other uses, notice that
   * Windows separators ("\") are replaced by simple slashes.
   *
   * @param path the original path
   * @return the normalized path
   */
  public static String cleanPath(String path) {
    if (isEmpty(path)) {
      return path;
    }
    String pathToUse = replace(path, WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);

    // Shortcut if there is no work to do
    if (pathToUse.indexOf('.') == -1) {
      return pathToUse;
    }

    // Strip prefix from path to analyze, to not treat it as part of the
    // first path element. This is necessary to correctly parse paths like
    // "file:core/../core/io/Resource.class", where the ".." should just
    // strip the first "core" directory while keeping the "file:" prefix.
    int prefixIndex = pathToUse.indexOf(':');
    String prefix = Constant.BLANK;
    if (prefixIndex != -1) {
      prefix = pathToUse.substring(0, prefixIndex + 1);
      if (prefix.contains(FOLDER_SEPARATOR)) {
        prefix = Constant.BLANK;
      }
      else {
        pathToUse = pathToUse.substring(prefixIndex + 1);
      }
    }
    if (matchesFirst(pathToUse, Constant.PATH_SEPARATOR)) {
      prefix = prefix + FOLDER_SEPARATOR;
      pathToUse = pathToUse.substring(1);
    }

    String[] pathArray = delimitedListToStringArray(pathToUse, FOLDER_SEPARATOR);
    LinkedList<String> pathElements = new LinkedList<>();
    int tops = 0;

    for (int i = pathArray.length - 1; i >= 0; i--) {
      String element = pathArray[i];
/*          if (CURRENT_PATH.equals(element)) {
     // Points to current directory - drop it.
}
else */
      if (TOP_PATH.equals(element)) {
        // Registering top path found.
        tops++;
      }
      else if (!CURRENT_PATH.equals(element)) {
        if (tops > 0) {
          // Merging path element with element corresponding to top path.
          tops--;
        }
        else {
          // Normal path element found.
          pathElements.add(0, element);
        }
      }
    }

    // All path elements stayed the same - shortcut
    if (pathArray.length == pathElements.size()) {
      return prefix.concat(pathToUse);
    }

    // Remaining top paths need to be retained.
    for (int i = 0; i < tops; i++) {
      pathElements.add(0, TOP_PATH);
    }
    // If nothing else left, at least explicitly point to current path.
    if (pathElements.size() == 1
            && Constant.BLANK.equals(pathElements.getLast())
            && !prefix.endsWith(FOLDER_SEPARATOR)) {

      pathElements.add(0, CURRENT_PATH);
    }

    return prefix.concat(collectionToString(pathElements, FOLDER_SEPARATOR));
  }

  /**
   * Compare two paths after normalization of them.
   *
   * @param path1 first path for comparison
   * @param path2 second path for comparison
   * @return whether the two paths are equivalent after normalization
   */
  public static boolean pathEquals(String path1, String path2) {
    return cleanPath(path1).equals(cleanPath(path2));
  }

  /**
   * Check Url, format url like :
   *
   * <pre>
   * users    -> /users
   * /users   -> /users
   * </pre>
   *
   * @param url Input url
   */
  public static String formatURL(String url) {
    if (StringUtils.isEmpty(url)) {
      return Constant.BLANK;
    }
    else {
      if (url.charAt(0) == '/') {
        return url;
      }
      return '/' + url;
    }
  }

  /**
   * Append line to {@link StringBuilder}
   *
   * @param reader String line read from {@link BufferedReader}
   * @param builder The {@link StringBuilder} append to
   * @throws IOException If an I/O error occurs
   */
  public static void appendLine(
          final BufferedReader reader, final StringBuilder builder) throws IOException {
    String line;
    while ((line = reader.readLine()) != null) {
      builder.append(line);
    }
  }

  /**
   * Replace all occurrences of a substring within a string with another string.
   *
   * @param inString {@code String} to examine
   * @param oldPattern {@code String} to replace
   * @param newPattern {@code String} to insert
   * @return a {@code String} with the replacements
   */
  public static String replace(String inString, String oldPattern, @Nullable String newPattern) {
    if (isEmpty(inString) || isEmpty(oldPattern) || newPattern == null) {
      return inString;
    }
    int index = inString.indexOf(oldPattern);
    if (index == -1) {
      return inString; // no occurrence -> can return input as-is
    }

    int capacity = inString.length();
    if (newPattern.length() > oldPattern.length()) {
      capacity += 16;
    }
    final StringBuilder sb = new StringBuilder(capacity);

    int pos = 0; // our position in the old string
    int patLen = oldPattern.length();
    while (index >= 0) {
      sb.append(inString, pos, index);
      sb.append(newPattern);
      index = inString.indexOf(oldPattern, pos = index + patLen);
    }

    // append any characters to the right of a match
    sb.append(inString, pos, inString.length());
    return sb.toString();
  }

  /**
   * Capitalize a {@code String}, changing the first letter to upper case as per
   * {@link Character#toUpperCase(char)}. No other letters are changed.
   *
   * @param str the {@code String} to capitalize
   * @return the capitalized {@code String}
   */
  public static String capitalize(String str) {
    return changeFirstCharacterCase(str, true);
  }

  /**
   * Uncapitalize a {@code String}, changing the first letter to lower case as per
   * {@link Character#toLowerCase(char)}. No other letters are changed.
   *
   * @param str the {@code String} to uncapitalize
   * @return the uncapitalized {@code String}
   */
  public static String uncapitalize(String str) {
    return changeFirstCharacterCase(str, false);
  }

  public static String changeFirstCharacterCase(String str, boolean capitalize) {
    if (isEmpty(str)) {
      return str;
    }
    final char firstChar = str.charAt(0);
    if (capitalize
            ? (firstChar >= 'A' && firstChar <= 'Z')// already upper case
            : (firstChar >= 'a' && firstChar <= 'z')) {
      return str;
    }
    final char[] chars = str.toCharArray();
    chars[0] = capitalize ? Character.toUpperCase(firstChar) : Character.toLowerCase(firstChar);
    return new String(chars, 0, chars.length);
  }

  /**
   * Delete any character in a given {@code String}.
   *
   * @param inString the original {@code String}
   * @param charsToDelete a set of characters to delete. E.g. "az\n" will delete 'a's, 'z's
   * and new lines.
   * @return the resulting {@code String}
   */
  public static String deleteAny(final String inString, @Nullable final String charsToDelete) {
    if (isEmpty(inString) || isEmpty(charsToDelete)) {
      return inString;
    }

    final int length = inString.length();
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      final char c = inString.charAt(i);

      if (charsToDelete.indexOf(c) == -1) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Convert a comma delimited list (e.g., a row from a CSV file) into an
   * array of strings.
   *
   * @param str the input {@code String} (potentially {@code null} or empty)
   * @return an array of strings, or the empty array in case of empty input
   */
  @NonNull
  public static String[] commaDelimitedListToStringArray(@Nullable String str) {
    return delimitedListToStringArray(str, ",");
  }

  /**
   * Take a {@code String} that is a delimited list and convert it into a
   * {@code String} array.
   * <p>
   * A single {@code delimiter} may consist of more than one character, but it
   * will still be considered as a single delimiter string, rather than as bunch
   * of potential delimiter characters, in contrast to
   * {@link #tokenizeToStringArray}.
   *
   * @param str the input {@code String} (potentially {@code null} or empty)
   * @param delimiter the delimiter between elements (this is a single delimiter, rather
   * than a bunch individual delimiter characters)
   * @return an array of the tokens in the list
   * @see #tokenizeToStringArray
   */
  @NonNull
  public static String[] delimitedListToStringArray(@Nullable String str, @Nullable String delimiter) {
    return delimitedListToStringArray(str, delimiter, null);
  }

  /**
   * Take a {@code String} that is a delimited list and convert it into a
   * {@code String} array.
   * <p>
   * A single {@code delimiter} may consist of more than one character, but it
   * will still be considered as a single delimiter string, rather than as bunch
   * of potential delimiter characters, in contrast to
   * {@link #tokenizeToStringArray}.
   *
   * @param str the input {@code String} (potentially {@code null} or empty)
   * @param delimiter the delimiter between elements (this is a single delimiter, rather
   * than a bunch individual delimiter characters)
   * @param charsToDelete a set of characters to delete; useful for deleting unwanted line
   * breaks: e.g. "\r\n\f" will delete all new lines and line feeds in
   * a {@code String}
   * @return an array of the tokens in the list
   * @see #tokenizeToStringArray
   */
  @NonNull
  public static String[] delimitedListToStringArray(
          @Nullable String str, @Nullable String delimiter, @Nullable String charsToDelete) {

    if (str == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    if (delimiter == null) {
      return new String[] { str };
    }

    int length = str.length();
    ArrayList<String> result = new ArrayList<>();
    if (delimiter.isEmpty()) {
      for (int i = 0; i < length; i++) {
        result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
      }
    }
    else {
      int pos = 0;
      int delPos;
      while ((delPos = str.indexOf(delimiter, pos)) != -1) {
        result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
        pos = delPos + delimiter.length();
      }
      if (length > 0 && pos <= length) {
        // Add rest of String, but not in case of empty input.
        result.add(deleteAny(str.substring(pos), charsToDelete));
      }
    }
    return toStringArray(result);
  }

  /**
   * Concatenate the given {@code String} arrays into one, with overlapping array
   * elements included twice.
   * <p>
   * The order of elements in the original arrays is preserved.
   *
   * @param array1 the first array (can be {@code null})
   * @param array2 the second array (can be {@code null})
   * @return the new array ({@code null} if both given arrays were {@code null})
   */
  public static String[] concatenateStringArrays(String[] array1, String[] array2) {
    if (ObjectUtils.isEmpty(array1)) {
      return array2;
    }
    if (ObjectUtils.isEmpty(array2)) {
      return array1;
    }

    String[] newArr = new String[array1.length + array2.length];
    System.arraycopy(array1, 0, newArr, 0, array1.length);
    System.arraycopy(array2, 0, newArr, array1.length, array2.length);
    return newArr;
  }

  /**
   * Check whether the given {@code CharSequence} contains actual <em>text</em>.
   * <p>
   * More specifically, this method returns {@code true} if the
   * {@code CharSequence} is not {@code null}, its length is greater than 0, and
   * it contains at least one non-whitespace character.
   * <p>
   * <pre class="code">
   * StringUtils.hasText(null) = false
   * StringUtils.hasText("") = false
   * StringUtils.hasText(" ") = false
   * StringUtils.hasText("12345") = true
   * StringUtils.hasText(" 12345 ") = true
   * </pre>
   *
   * @param str the {@code CharSequence} to check (may be {@code null})
   * @return {@code true} if the {@code CharSequence} is not {@code null}, its
   * length is greater than 0, and it does not contain whitespace only
   * @see Character#isWhitespace
   */
  public static boolean hasText(@Nullable CharSequence str) {
    return isNotEmpty(str) && containsText(str);
  }

  /**
   * Check whether the given {@code String} contains actual <em>text</em>.
   * <p>More specifically, this method returns {@code true} if the
   * {@code String} is not {@code null}, its length is greater than 0,
   * and it contains at least one non-whitespace character.
   *
   * @param str the {@code String} to check (may be {@code null})
   * @return {@code true} if the {@code String} is not {@code null}, its
   * length is greater than 0, and it does not contain whitespace only
   * @see #hasText(CharSequence)
   * @see #isNotEmpty(CharSequence)
   * @see Character#isWhitespace
   */
  public static boolean hasText(@Nullable String str) {
    return (str != null && !str.isEmpty() && containsText(str));
  }

  private static boolean containsText(CharSequence str) {
    int strLen = str.length();
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Extract the filename from the given Java resource path, e.g.
   * {@code "mypath/myfile.txt" -> "myfile.txt"}.
   *
   * @param path the file path (may be {@code null})
   * @return the extracted filename, or {@code null} if none
   */
  public static String getFilename(String path) {
    if (path == null) {
      return null;
    }
    int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
    return (separatorIndex != -1 ? path.substring(separatorIndex + 1) : path);
  }

  /**
   * Extract the filename extension from the given Java resource path, e.g.
   * "mypath/myfile.txt" -> "txt".
   *
   * @param path the file path (may be {@code null})
   * @return the extracted filename extension, or {@code null} if none
   */
  public static String getFilenameExtension(String path) {
    if (path == null) {
      return null;
    }

    int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
    if (extIndex == -1) {
      return null;
    }

    int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR);
    if (folderIndex > extIndex) {
      return null;
    }

    return path.substring(extIndex + 1);
  }

  /**
   * Strip the filename extension from the given Java resource path,
   * e.g. "mypath/myfile.txt" -> "mypath/myfile".
   *
   * @param path the file path
   * @return the path with stripped filename extension
   */
  public static String stripFilenameExtension(String path) {
    int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
    if (extIndex == -1) {
      return path;
    }

    int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR);
    if (folderIndex > extIndex) {
      return path;
    }

    return path.substring(0, extIndex);
  }

  //

  /**
   * Match a String against the given pattern, supporting the following simple
   * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
   * arbitrary number of pattern parts), as well as direct equality.
   *
   * @param pattern the pattern to match against
   * @param str the String to match
   * @return whether the String matches the given pattern
   */
  public static boolean simpleMatch(String pattern, String str) {
    if (pattern == null || str == null) {
      return false;
    }

    int firstIndex = pattern.indexOf('*');
    if (firstIndex == -1) {
      return pattern.equals(str);
    }

    if (firstIndex == 0) {
      if (pattern.length() == 1) {
        return true;
      }
      int nextIndex = pattern.indexOf('*', 1);
      if (nextIndex == -1) {
        return str.endsWith(pattern.substring(1));
      }
      String part = pattern.substring(1, nextIndex);
      if (part.isEmpty()) {
        return simpleMatch(pattern.substring(nextIndex), str);
      }
      int partIndex = str.indexOf(part);
      while (partIndex != -1) {
        if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()))) {
          return true;
        }
        partIndex = str.indexOf(part, partIndex + 1);
      }
      return false;
    }

    return str.length() >= firstIndex
            && pattern.substring(0, firstIndex).equals(str.substring(0, firstIndex))
            && simpleMatch(pattern.substring(firstIndex), str.substring(firstIndex));
  }

  /**
   * Match a String against the given patterns, supporting the following simple
   * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
   * arbitrary number of pattern parts), as well as direct equality.
   *
   * @param patterns the patterns to match against
   * @param str the String to match
   * @return whether the String matches any of the given patterns
   */
  public static boolean simpleMatch(@Nullable String[] patterns, String str) {
    if (patterns != null) {
      for (String pattern : patterns) {
        if (simpleMatch(pattern, str)) {
          return true;
        }
      }
    }
    return false;
  }

  public static String generateRandomString(int length) {
    final char[] ret = new char[length];
    final Random random = StringUtils.random;
    for (int i = 0; i < length; i++) {
      ret[i] = generateRandomCharacter(random.nextInt(3));
    }
    return String.valueOf(ret);
  }

  private static char generateRandomCharacter(int type) {
    int rand;
    switch (type) {
      case 0://随机小写字母
        rand = random.nextInt(26);
        rand += 97;
        return (char) rand;
      case 1://随机大写字母
        rand = random.nextInt(26);
        rand += 65;
        return (char) rand;
      case 2://随机数字
      default:
        rand = random.nextInt(10);
        rand += 48;
        return (char) rand;
    }
  }
  // 3.0

  /**
   * Check whether the given {@code CharSequence} contains any whitespace characters.
   *
   * @param str the {@code CharSequence} to check (may be {@code null})
   * @return {@code true} if the {@code CharSequence} is not empty and
   * contains at least 1 whitespace character
   * @see Character#isWhitespace
   * @since 3.0
   */
  public static boolean containsWhitespace(CharSequence str) {
    if (isEmpty(str)) {
      return false;
    }

    int strLen = str.length();
    for (int i = 0; i < strLen; i++) {
      if (Character.isWhitespace(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check whether the given {@code String} contains any whitespace characters.
   *
   * @param str the {@code String} to check (may be {@code null})
   * @return {@code true} if the {@code String} is not empty and
   * contains at least 1 whitespace character
   * @see #containsWhitespace(CharSequence)
   * @since 3.0
   */
  public static boolean containsWhitespace(String str) {
    return containsWhitespace((CharSequence) str);
  }

  /**
   * Trim leading and trailing whitespace from the given {@code String}.
   *
   * @param str the {@code String} to check
   * @return the trimmed {@code String}
   * @see java.lang.Character#isWhitespace
   * @since 3.0
   */
  public static String trimWhitespace(String str) {
    if (isEmpty(str)) {
      return str;
    }

    int beginIndex = 0;
    int endIndex = str.length() - 1;

    while (beginIndex <= endIndex && Character.isWhitespace(str.charAt(beginIndex))) {
      beginIndex++;
    }

    while (endIndex > beginIndex && Character.isWhitespace(str.charAt(endIndex))) {
      endIndex--;
    }

    return str.substring(beginIndex, endIndex + 1);
  }

  /**
   * Trim <i>all</i> whitespace from the given {@code String}:
   * leading, trailing, and in between characters.
   *
   * @param str the {@code String} to check
   * @return the trimmed {@code String}
   * @see java.lang.Character#isWhitespace
   * @since 3.0
   */
  public static String trimAllWhitespace(String str) {
    if (isEmpty(str)) {
      return str;
    }

    int len = str.length();
    StringBuilder sb = new StringBuilder(str.length());
    for (int i = 0; i < len; i++) {
      char c = str.charAt(i);
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Trim leading whitespace from the given {@code String}.
   *
   * @param str the {@code String} to check
   * @return the trimmed {@code String}
   * @see java.lang.Character#isWhitespace
   * @since 3.0
   */
  public static String trimLeadingWhitespace(String str) {
    if (isEmpty(str)) {
      return str;
    }

    int beginIdx = 0;
    int length = str.length();
    while (beginIdx < length && Character.isWhitespace(str.charAt(beginIdx))) {
      beginIdx++;
    }
    return str.substring(beginIdx);
  }

  /**
   * Trim trailing whitespace from the given {@code String}.
   *
   * @param str the {@code String} to check
   * @return the trimmed {@code String}
   * @see java.lang.Character#isWhitespace
   * @since 3.0
   */
  public static String trimTrailingWhitespace(String str) {
    if (isEmpty(str)) {
      return str;
    }

    int endIdx = str.length() - 1;
    while (endIdx >= 0 && Character.isWhitespace(str.charAt(endIdx))) {
      endIdx--;
    }
    return str.substring(0, endIdx + 1);
  }

  /**
   * Trim all occurrences of the supplied leading character from the given {@code String}.
   *
   * @param str the {@code String} to check
   * @param leadingCharacter the leading character to be trimmed
   * @return the trimmed {@code String}
   * @since 3.0
   */
  public static String trimLeadingCharacter(String str, char leadingCharacter) {
    if (isEmpty(str)) {
      return str;
    }

    int beginIdx = 0;
    int length = str.length();
    while (beginIdx < length && leadingCharacter == str.charAt(beginIdx)) {
      beginIdx++;
    }
    return str.substring(beginIdx);
  }

  /**
   * Trim all occurrences of the supplied trailing character from the given {@code String}.
   *
   * @param str the {@code String} to check
   * @param trailingCharacter the trailing character to be trimmed
   * @return the trimmed {@code String}
   * @since 3.0
   */
  public static String trimTrailingCharacter(String str, char trailingCharacter) {
    if (isEmpty(str)) {
      return str;
    }

    int endIdx = str.length() - 1;
    while (endIdx >= 0 && trailingCharacter == str.charAt(endIdx)) {
      endIdx--;
    }
    return str.substring(0, endIdx + 1);
  }

  /**
   * Test if the given {@code String} starts with the specified prefix,
   * ignoring upper/lower case.
   *
   * @param str the {@code String} to check
   * @param prefix the prefix to look for
   * @see java.lang.String#startsWith
   * @since 4.0
   */
  public static boolean startsWithIgnoreCase(@Nullable String str, @Nullable String prefix) {
    return str != null
            && prefix != null
            && str.length() >= prefix.length()
            && str.regionMatches(true, 0, prefix, 0, prefix.length());
  }

  /**
   * Test if the given {@code String} ends with the specified suffix,
   * ignoring upper/lower case.
   *
   * @param str the {@code String} to check
   * @param suffix the suffix to look for
   * @see java.lang.String#endsWith
   * @since 4.0
   */
  public static boolean endsWithIgnoreCase(@Nullable String str, @Nullable String suffix) {
    return str != null
            && suffix != null
            && str.length() >= suffix.length()
            && str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length());
  }

  /**
   * Test whether the given string matches the given substring
   * at the given index.
   *
   * @param str the original string (or StringBuilder)
   * @param index the index in the original string to start matching against
   * @param substring the substring to match at the given index
   */
  public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
    int substringLength = substring.length();
    if (index + substringLength > str.length()) {
      return false;
    }
    for (int i = 0; i < substringLength; i++) {
      if (str.charAt(index + i) != substring.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Test if the given {@code String} matches the given single character.
   *
   * @param str the {@code String} to check
   * @param singleCharacter the character to compare to
   * @since 3.0
   */
  public static boolean matchesCharacter(String str, char singleCharacter) {
    return (str != null && str.length() == 1 && str.charAt(0) == singleCharacter);
  }

  /**
   * Test if the given {@code String} matches the given index to single character.
   *
   * @param str given string
   * @param idx str's index to match
   * @param charToMatch char To Match
   * @since 4.0
   */
  public static boolean matchesCharacter(@Nullable String str, int idx, char charToMatch) {
    if (str == null || (idx < 0) || (idx >= str.length())) {
      return false;
    }
    return str.charAt(idx) == charToMatch;
  }

  /**
   * Test if the first given {@code String} matches the given single character.
   * <p>
   * Suitable for comparing the first character
   * </p>
   *
   * @param str given string
   * @param charToMatch char To Match
   * @since 4.0
   */
  public static boolean matchesFirst(@Nullable String str, char charToMatch) {
    return str != null && str.length() != 0 && str.charAt(0) == charToMatch;
  }

  /**
   * Test if the last given {@code String} matches the given single character.
   *
   * @param str given string
   * @param charToMatch char To Match
   * @since 4.0
   */
  public static boolean matchesLast(@Nullable String str, char charToMatch) {
    return isNotEmpty(str) && str.charAt(str.length() - 1) == charToMatch;
  }

  //

  /**
   * Parse the given {@code String} value into a {@link Locale}, accepting
   * the {@link Locale#toString} format as well as BCP 47 language tags.
   *
   * @param localeValue the locale value: following either {@code Locale's}
   * {@code toString()} format ("en", "en_UK", etc), also accepting spaces as
   * separators (as an alternative to underscores), or BCP 47 (e.g. "en-UK")
   * as specified by {@link Locale#forLanguageTag} on Java 7+
   * @return a corresponding {@code Locale} instance, or {@code null} if none
   * @throws IllegalArgumentException in case of an invalid locale specification
   * @see #parseLocaleString
   * @see Locale#forLanguageTag
   * @since 3.0
   */
  public static Locale parseLocale(String localeValue) {
    String[] tokens = tokenizeLocaleSource(localeValue);
    if (tokens.length == 1) {
      validateLocalePart(localeValue);
      Locale resolved = Locale.forLanguageTag(localeValue);
      if (resolved.getLanguage().length() > 0) {
        return resolved;
      }
    }
    return parseLocaleTokens(localeValue, tokens);
  }

  /**
   * Parse the given {@code String} representation into a {@link Locale}.
   * <p>For many parsing scenarios, this is an inverse operation of
   * {@link Locale#toString Locale's toString}, in a lenient sense.
   * This method does not aim for strict {@code Locale} design compliance;
   * it is rather specifically tailored for typical parsing needs.
   * <p><b>Note: This delegate does not accept the BCP 47 language tag format.
   * Please use {@link #parseLocale} for lenient parsing of both formats.</b>
   *
   * @param localeString the locale {@code String}: following {@code Locale's}
   * {@code toString()} format ("en", "en_UK", etc), also accepting spaces as
   * separators (as an alternative to underscores)
   * @return a corresponding {@code Locale} instance, or {@code null} if none
   * @throws IllegalArgumentException in case of an invalid locale specification
   * @since 3.0
   */
  public static Locale parseLocaleString(String localeString) {
    return parseLocaleTokens(localeString, tokenizeLocaleSource(localeString));
  }

  private static String[] tokenizeLocaleSource(String localeSource) {
    return tokenizeToStringArray(localeSource, "_ ", false, false);
  }

  private static Locale parseLocaleTokens(String localeString, String[] tokens) {
    String language = (tokens.length > 0 ? tokens[0] : Constant.BLANK);
    String country = (tokens.length > 1 ? tokens[1] : Constant.BLANK);
    validateLocalePart(language);
    validateLocalePart(country);

    String variant = Constant.BLANK;
    if (tokens.length > 2) {
      // There is definitely a variant, and it is everything after the country
      // code sans the separator between the country code and the variant.
      int endIndexOfCountryCode = localeString.indexOf(country, language.length()) + country.length();
      // Strip off any leading '_' and whitespace, what's left is the variant.
      variant = trimLeadingWhitespace(localeString.substring(endIndexOfCountryCode));
      if (matchesFirst(variant, '_')) {
        variant = trimLeadingCharacter(variant, '_');
      }
    }

    if (variant.isEmpty() && matchesFirst(country, '#')) {
      variant = country;
      country = Constant.BLANK;
    }

    return (language.length() > 0 ? new Locale(language, country, variant) : null);
  }

  private static void validateLocalePart(String localePart) {
    int length = localePart.length();
    for (int i = 0; i < length; i++) {
      char ch = localePart.charAt(i);
      if (ch != ' ' && ch != '_' && ch != '-' && ch != '#' && !Character.isLetterOrDigit(ch)) {
        throw new IllegalArgumentException(
                "Locale part \"" + localePart + "\" contains invalid characters");
      }
    }
  }

  /**
   * Parse the given {@code timeZoneString} value into a {@link TimeZone}.
   *
   * @param timeZoneString the time zone {@code String}, following {@link TimeZone#getTimeZone(String)}
   * but throwing {@link IllegalArgumentException} in case of an invalid time zone specification
   * @return a corresponding {@link TimeZone} instance
   * @throws IllegalArgumentException in case of an invalid time zone specification
   */
  public static TimeZone parseTimeZoneString(String timeZoneString) {
    TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
    if ("GMT".equals(timeZone.getID()) && !timeZoneString.startsWith("GMT")) {
      // We don't want that GMT fallback...
      throw new IllegalArgumentException("Invalid time zone specification '" + timeZoneString + "'");
    }
    return timeZone;
  }

  /**
   * Convert a name in camelCase to an underscored name in lower case. Any upper
   * case letters are converted to lower case with a preceding underscore.
   *
   * @param name the original name
   * @return the converted name
   * @since 4.0
   */
  public static String camelCaseToUnderscore(String name) {
    if (StringUtils.isEmpty(name)) {
      return Constant.BLANK;
    }
    final int length = name.length();
    final StringBuilder ret = new StringBuilder();
    for (int i = 0; i < length; i++) {
      final char c = name.charAt(i);
      if (c > 0x40 && c < 0x5b) {
        ret.append('_').append((char) (c | 0x20));
      }
      else {
        ret.append(c);
      }
    }
    return ret.toString();
  }

  /**
   * Takes a string formatted like: 'my_string_variable' and returns it as:
   * 'myStringVariable'
   *
   * @param underscore a string formatted like: 'my_string_variable'
   * @return Takes a string formatted like: 'my_string_variable' and returns it as:
   * 'myStringVariable'
   * @since 4.0
   */
  public static String underscoreToCamelCase(String underscore) {
    if (StringUtils.isEmpty(underscore)) {
      return underscore;
    }

    final char[] chars = underscore.toCharArray();
    int write = -1;
    boolean upper = false;
    for (final char c : chars) {
      if ('_' == c) {
        upper = true;
        continue;
      }
      if (upper) {
        upper = false;
        chars[++write] = Character.toUpperCase(c);
      }
      else {
        chars[++write] = Character.toLowerCase(c);
      }
    }
    return new String(chars, 0, ++write);
  }

}
