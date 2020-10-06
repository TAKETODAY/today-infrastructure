/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import cn.taketoday.context.Constant;

/**
 * @author TODAY <br>
 * 2018-06-26 21:19:09
 */
public abstract class StringUtils {

  private static final int caseDiff = ('a' - 'A');
  private static BitSet dontNeedEncoding;

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

  public static boolean isArrayNotEmpty(String... strs) {
    return strs != null && strs.length != 0;
  }

  public static boolean isArrayEmpty(String... strs) {
    return strs == null || strs.length == 0;
  }

  /**
   * Split with {@link Constant#SPLIT_REGEXP}
   *
   * @param source
   *         source string
   *
   * @return if source is null this will returns {@link Constant#EMPTY_STRING_ARRAY}
   */
  public static String[] split(String source) {
    if (source == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    if (source.isEmpty()) {
      return new String[] { source };
    }
    final LinkedList<String> list = new LinkedList<>();

    int idx = 0;
    int start = 0;
    final char[] chars = source.toCharArray();
    for (final char c : chars) {
      if (isSplitable(c)) {
        list.add(new String(chars, start, idx - start));
        start = idx + 1;
      }
      idx++;
    }
    if (idx != start && idx == source.length()) { // 最后一次分割
      list.add(new String(chars, start, idx - start));
    }
    if (list.isEmpty()) {
      return new String[] { source };
    }
    return list.toArray(new String[list.size()]);
  }

//  public static String[] split(String source) {
//    if (isEmpty(source)) {
//      return Constant.EMPTY_STRING_ARRAY;
//    }
//    final int length = source.length();
//    final LinkedList<String> list = new LinkedList<>();
//
//    int count = 0;
//    final char[] buffer = new char[length];
//
//    for (int i = 0; i < length; i++) {
//      final char c = source.charAt(i);
//      if (isSplitable(c)) {
//        list.add(new String(buffer, 0, count));
//        count = 0;
//      }
//      else {
//        buffer[count++] = c;
//      }
//    }
//    if (count != 0) { // 最后一次分割
//      list.add(new String(buffer, 0, count));
//    }
//    if (list.isEmpty()) {
//      return new String[] { source };
//    }
//    return list.toArray(new String[list.size()]);
//  }

  static boolean isSplitable(final char c) {
    return c == ',' || c == ';';
  }

  public static String decodeUrl(String s) {

    final int numChars = s.length();

    final StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
    final Charset charset = Constant.DEFAULT_CHARSET;

    int i = 0;
    char c;
    boolean needToChange = false;
    byte[] bytes = null;
    while (i < numChars) {
      switch (c = s.charAt(i)) {
        case '+': {
          sb.append(' ');
          i++;
          needToChange = true;
          break;
        }
        case '%': {
          try {
            // (numChars-i)/3 is an upper bound for the number
            // of remaining bytes
            if (bytes == null) bytes = new byte[(numChars - i) / 3];
            int pos = 0;
            while (((i + 2) < numChars) && (c == '%')) {
              int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
              if (v < 0) {
                throw new IllegalArgumentException("Illegal hex characters in escape (%) pattern - negative value");
              }
              bytes[pos++] = (byte) v;
              i += 3;
              if (i < numChars) c = s.charAt(i);
            }
            // A trailing, incomplete byte encoding such as
            // "%x" will cause an exception to be thrown
            if ((i < numChars) && (c == '%')) {
              throw new IllegalArgumentException("Incomplete trailing escape (%) pattern");
            }
            sb.append(new String(bytes, 0, pos, charset));
          }
          catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal hex characters in escape (%) pattern - " + e.getMessage());
          }
          needToChange = true;
          break;
        }
        default: {
          sb.append(c);
          i++;
          break;
        }
      }
    }
    return (needToChange ? sb.toString() : s);
  }

  public static String encodeUrl(String s) {

    boolean needToChange = false;
    final int length = s.length();
    final StringBuilder out = new StringBuilder(length);
    final CharArrayWriter charArrayWriter = new CharArrayWriter();

    final BitSet dontNeedEncoding = StringUtils.dontNeedEncoding;
    final int caseDiff = StringUtils.caseDiff;
    final Charset charset = Constant.DEFAULT_CHARSET;

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
          int d = (int) s.charAt(i + 1);
          // System.out.println("\tExamining " + Integer.toHexString(d));
          if (d >= 0xDC00 && d <= 0xDFFF) {
            // System.out.println("\t" + Integer.toHexString(d) + " is low surrogate");
            charArrayWriter.write(d);
            i++;
          }
        }
        i++;
      }
      while (i < length && !dontNeedEncoding.get((c = (int) s.charAt(i))));

      charArrayWriter.flush();
      byte[] ba = new String(charArrayWriter.toCharArray()).getBytes(charset);
      for (int j = 0; j < ba.length; j++) {
        out.append('%');
        char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
        // converting to use uppercase letter as part of
        // the hex value if ch is a letter.
        if (Character.isLetter(ch)) {
          ch -= caseDiff;
        }
        out.append(ch);
        ch = Character.forDigit(ba[j] & 0xF, 16);
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

  /**
   * Parse Parameters
   *
   * @param s
   *         Input {@link String}
   *
   * @return Map of list parameters
   */
  public static Map<String, List<String>> parseParameters(final String s) {

    if (isEmpty(s)) {
      return Collections.emptyMap();
    }

    final Map<String, List<String>> params = new HashMap<>();
    int nameStart = 0;
    int valueStart = -1;
    int i;
    final int len = s.length();
    loop:
    for (i = 0; i < len; i++) {
      switch (s.charAt(i)) {
        case '=':
          if (nameStart == i) {
            nameStart = i + 1;
          }
          else if (valueStart < nameStart) {
            valueStart = i + 1;
          }
          break;
        case '&':
        case ';':
          addParam(s, nameStart, valueStart, i, params);
          nameStart = i + 1;
          break;
        case '#':
          break loop;
        default:
          // continue
      }
    }
    addParam(s, nameStart, valueStart, i, params);
    return params;
  }

  private static void addParam(String s,
                               int nameStart,
                               int valueStart,
                               int valueEnd,
                               Map<String, List<String>> params) {

    if (nameStart < valueEnd) {
      if (valueStart <= nameStart) {
        valueStart = valueEnd + 1;
      }
      String name = s.substring(nameStart, valueStart - 1); //FIXME
      String value = s.substring(valueStart, valueEnd);
      List<String> values = params.get(name);
      if (values == null) {
        params.put(name, values = new ArrayList<>(2));
      }
      values.add(value);
    }
  }

  /**
   * Use StringTokenizer to split string to string array
   *
   * @param str
   *         Input string
   * @param delimiter
   *         Input delimiter
   *
   * @return Returns the splitted string array
   */
  public static String[] tokenizeToStringArray(final String str, final String delimiter) {

    if (str == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }

    StringTokenizer st = new StringTokenizer(str, delimiter);
    List<String> tokens = new ArrayList<>(4);
    while (st.hasMoreTokens()) {
      tokens.add(st.nextToken());
    }
    return toStringArray(tokens);
  }

  public static String[] tokenizeToStringArray(String str,
                                               String delimiters,
                                               boolean trimTokens,
                                               boolean ignoreEmptyTokens) {

    if (str == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    StringTokenizer st = new StringTokenizer(str, delimiters);
    ArrayList<String> tokens = new ArrayList<>(4);
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
   * {@link Collection} to string array
   *
   * @param collection
   *         All element must be a string
   *
   * @return String array
   */
  public static String[] toStringArray(Collection<String> collection) {
    return collection.toArray(new String[collection.size()]);
  }

  /**
   * Use default delimiter:',' append array to a string
   *
   * @param array
   *         Input array object
   */
  public static String arrayToString(Object[] array) {
    return arrayToString(array, ",");
  }

  /**
   * Array to string
   *
   * @param array
   *         Input array object
   * @param delimiter
   *         Delimiter string
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
   * @param collection
   *         Input {@link Collection} object
   *
   * @since 2.1.7
   */
  public static <T> String collectionToString(final Collection<T> collection) {
    return collectionToString(collection, ",");
  }

  /**
   * {@link Collection} to string
   *
   * @param coll
   *         Input {@link Collection} object
   * @param delimiter
   *         Delimiter string
   *
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
   * Check properties file name
   *
   * @param fileName
   *         Input file name
   *
   * @return checked properties file name
   */
  public static String checkPropertiesName(final String fileName) {
    return fileName.endsWith(Constant.PROPERTIES_SUFFIX) ? fileName : fileName + Constant.PROPERTIES_SUFFIX;
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
   * Read the {@link InputStream} to text string
   *
   * @param inputStream
   *         Input stream
   *
   * @return String {@link InputStream} read as text
   *
   * @throws IOException
   *         If can't read the string
   */
  public static String readAsText(final InputStream inputStream) throws IOException {
    return readAsText(inputStream, 1024);
  }

  /**
   * Read the {@link InputStream} to text string
   *
   * @param inputStream
   *         Input stream
   * @param bufferSize
   *         Buffer size
   *
   * @return String {@link InputStream} read as text
   *
   * @throws IOException
   *         If can't read the string
   */
  public static String readAsText(final InputStream inputStream, final int bufferSize) throws IOException {
    final ByteArrayOutputStream result = new ByteArrayOutputStream(bufferSize);
    byte[] buffer = new byte[bufferSize];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }
    buffer = null;
    return result.toString();
  }

  /**
   * Normalize the path by suppressing sequences like "path/.." and inner simple
   * dots.
   * <p>
   * The result is convenient for path comparison. For other uses, notice that
   * Windows separators ("\") are replaced by simple slashes.
   *
   * @param path
   *         the original path
   *
   * @return the normalized path
   */
  public static String cleanPath(String path) {
    if (isEmpty(path)) {
      return path;
    }
    String pathToUse = replace(path, Constant.WINDOWS_FOLDER_SEPARATOR, Constant.FOLDER_SEPARATOR);

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
      if (prefix.contains(Constant.FOLDER_SEPARATOR)) {
        prefix = Constant.BLANK;
      }
      else {
        pathToUse = pathToUse.substring(prefixIndex + 1);
      }
    }
    if (pathToUse.startsWith(Constant.FOLDER_SEPARATOR)) {
      prefix = prefix + Constant.FOLDER_SEPARATOR;
      pathToUse = pathToUse.substring(1);
    }

    String[] pathArray = delimitedListToStringArray(pathToUse, Constant.FOLDER_SEPARATOR);
    LinkedList<String> pathElements = new LinkedList<>();
    int tops = 0;

    for (int i = pathArray.length - 1; i >= 0; i--) {
      String element = pathArray[i];
/*          if (Constant.CURRENT_PATH.equals(element)) {
     // Points to current directory - drop it.
}
else */
      if (Constant.TOP_PATH.equals(element)) {
        // Registering top path found.
        tops++;
      }
      else if (!Constant.CURRENT_PATH.equals(element)) {
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
      pathElements.add(0, Constant.TOP_PATH);
    }
    // If nothing else left, at least explicitly point to current path.
    if (pathElements.size() == 1
            && Constant.BLANK.equals(pathElements.getLast())
            && !prefix.endsWith(Constant.FOLDER_SEPARATOR)) {

      pathElements.add(0, Constant.CURRENT_PATH);
    }

    return prefix.concat(collectionToString(pathElements, Constant.FOLDER_SEPARATOR));
  }

  /**
   * Check Url, format url like :
   *
   * <pre>
   * users    -> /users
   * /users   -> /users
   * </pre>
   *
   * @param url
   *         Input url
   */
  public static String checkUrl(String url) {
    return StringUtils.isEmpty(url) ? Constant.BLANK : (url.charAt(0) == '/' ? url : '/' + url);
  }

  /**
   * Append line to {@link StringBuilder}
   *
   * @param reader
   *         String line read from {@link BufferedReader}
   * @param builder
   *         The {@link StringBuilder} append to
   *
   * @throws IOException
   *         If an I/O error occurs
   */
  public static void appendLine(final BufferedReader reader, final StringBuilder builder) throws IOException {
    String line;
    while ((line = reader.readLine()) != null) {
      builder.append(line);
    }
  }

  /**
   * Replace all occurrences of a substring within a string with another string.
   *
   * @param inString
   *         {@code String} to examine
   * @param oldPattern
   *         {@code String} to replace
   * @param newPattern
   *         {@code String} to insert
   *
   * @return a {@code String} with the replacements
   */
  public static String replace(String inString, String oldPattern, String newPattern) {
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
   * @param str
   *         the {@code String} to capitalize
   *
   * @return the capitalized {@code String}
   */
  public static String capitalize(String str) {
    return changeFirstCharacterCase(str, true);
  }

  /**
   * Uncapitalize a {@code String}, changing the first letter to lower case as per
   * {@link Character#toLowerCase(char)}. No other letters are changed.
   *
   * @param str
   *         the {@code String} to uncapitalize
   *
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
   * @param inString
   *         the original {@code String}
   * @param charsToDelete
   *         a set of characters to delete. E.g. "az\n" will delete 'a's, 'z's
   *         and new lines.
   *
   * @return the resulting {@code String}
   */
  public static String deleteAny(final String inString, final String charsToDelete) {

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
   * Take a {@code String} that is a delimited list and convert it into a
   * {@code String} array.
   * <p>
   * A single {@code delimiter} may consist of more than one character, but it
   * will still be considered as a single delimiter string, rather than as bunch
   * of potential delimiter characters, in contrast to
   * {@link #tokenizeToStringArray}.
   *
   * @param str
   *         the input {@code String} (potentially {@code null} or empty)
   * @param delimiter
   *         the delimiter between elements (this is a single delimiter, rather
   *         than a bunch individual delimiter characters)
   *
   * @return an array of the tokens in the list
   *
   * @see #tokenizeToStringArray
   */
  public static String[] delimitedListToStringArray(final String str, final String delimiter) {
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
   * @param str
   *         the input {@code String} (potentially {@code null} or empty)
   * @param delimiter
   *         the delimiter between elements (this is a single delimiter, rather
   *         than a bunch individual delimiter characters)
   * @param charsToDelete
   *         a set of characters to delete; useful for deleting unwanted line
   *         breaks: e.g. "\r\n\f" will delete all new lines and line feeds in
   *         a {@code String}
   *
   * @return an array of the tokens in the list
   *
   * @see #tokenizeToStringArray
   */
  public static String[] delimitedListToStringArray(final String str, final String delimiter, final String charsToDelete) {

    if (str == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    if (delimiter == null) {
      return new String[] { str };
    }

    ArrayList<String> result = new ArrayList<>();
    if (delimiter.isEmpty()) {
      for (int i = 0; i < str.length(); i++) {
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
      if (str.length() > 0 && pos <= str.length()) {
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
   * @param array1
   *         the first array (can be {@code null})
   * @param array2
   *         the second array (can be {@code null})
   *
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
   * @param str
   *         the {@code CharSequence} to check (may be {@code null})
   *
   * @return {@code true} if the {@code CharSequence} is not {@code null}, its
   * length is greater than 0, and it does not contain whitespace only
   *
   * @see Character#isWhitespace
   */
  public static boolean hasText(CharSequence str) {
    return isNotEmpty(str) && containsText(str);
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
   * @param path
   *         the file path (may be {@code null})
   *
   * @return the extracted filename, or {@code null} if none
   */
  public static String getFilename(String path) {
    if (path == null) {
      return null;
    }
    int separatorIndex = path.lastIndexOf(Constant.FOLDER_SEPARATOR);
    return (separatorIndex != -1 ? path.substring(separatorIndex + 1) : path);
  }

  /**
   * Extract the filename extension from the given Java resource path, e.g.
   * "mypath/myfile.txt" -> "txt".
   *
   * @param path
   *         the file path (may be {@code null})
   *
   * @return the extracted filename extension, or {@code null} if none
   */
  public static String getFilenameExtension(String path) {
    if (path == null) {
      return null;
    }

    int extIndex = path.lastIndexOf(Constant.EXTENSION_SEPARATOR);
    if (extIndex == -1) {
      return null;
    }

    int folderIndex = path.lastIndexOf(Constant.FOLDER_SEPARATOR);
    if (folderIndex > extIndex) {
      return null;
    }

    return path.substring(extIndex + 1);
  }

  //

  /**
   * Match a String against the given pattern, supporting the following simple
   * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
   * arbitrary number of pattern parts), as well as direct equality.
   *
   * @param pattern
   *         the pattern to match against
   * @param str
   *         the String to match
   *
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

    return (str.length() >= firstIndex &&
            pattern.substring(0, firstIndex).equals(str.substring(0, firstIndex)) &&
            simpleMatch(pattern.substring(firstIndex), str.substring(firstIndex)));
  }

  /**
   * Match a String against the given patterns, supporting the following simple
   * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
   * arbitrary number of pattern parts), as well as direct equality.
   *
   * @param patterns
   *         the patterns to match against
   * @param str
   *         the String to match
   *
   * @return whether the String matches any of the given patterns
   */
  public static boolean simpleMatch(String[] patterns, String str) {
    if (patterns != null) {
      for (String pattern : patterns) {
        if (simpleMatch(pattern, str)) {
          return true;
        }
      }
    }
    return false;
  }

}
