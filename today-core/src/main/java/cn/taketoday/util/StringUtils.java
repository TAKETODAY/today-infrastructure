/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * Miscellaneous {@link String} utility methods.
 *
 * <p>Mainly for internal use within the framework; consider
 * <a href="https://commons.apache.org/proper/commons-lang/">Apache's Commons Lang</a>
 * for a more comprehensive suite of {@code String} utilities.
 *
 * <p>This class delivers some simple functionality that should really be
 * provided by the core Java {@link String} and {@link StringBuilder}
 * classes. It also provides easy-to-use methods to convert between
 * delimited strings, such as CSV strings, and collections and arrays.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-06-26 21:19:09
 */
public abstract class StringUtils {

  public static final String TOP_PATH = "..";
  public static final String CURRENT_PATH = ".";
  public static final char FOLDER_SEPARATOR_CHAR = Constant.PATH_SEPARATOR;
  public static final String FOLDER_SEPARATOR = "/";
  public static final String WINDOWS_FOLDER_SEPARATOR = "\\";
  public static final char EXTENSION_SEPARATOR = Constant.PACKAGE_SEPARATOR;

  private static final int DEFAULT_TRUNCATION_THRESHOLD = 100;

  private static final String TRUNCATION_SUFFIX = " (truncated)...";

  private static final Random random = new Random();

  //---------------------------------------------------------------------
  // General convenience methods for working with Strings
  //---------------------------------------------------------------------

  public static boolean isEmpty(@Nullable CharSequence str) {
    return str == null || str.isEmpty();
  }

  public static boolean isNotEmpty(@Nullable CharSequence str) {
    return !isEmpty(str);
  }

  /**
   * Split with {@link Constant#SPLIT_REGEXP}
   *
   * @param source source string
   * @return if source is null this will returns
   * {@link Constant#EMPTY_STRING_ARRAY}
   */

  public static String[] split(@Nullable String source) {
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
  public static List<String> splitAsList(@Nullable String source) {
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
   * Decode the given encoded URI component value. Based on the following rules:
   * <ul>
   * <li>Alphanumeric characters {@code "a"} through {@code "z"}, {@code "A"} through {@code "Z"},
   * and {@code "0"} through {@code "9"} stay the same.</li>
   * <li>Special characters {@code "-"}, {@code "_"}, {@code "."}, and {@code "*"} stay the same.</li>
   * <li>A sequence "{@code %<i>xy</i>}" is interpreted as a hexadecimal representation of the character.</li>
   * </ul>
   *
   * @param source the encoded String
   * @param charset the character set
   * @return the decoded value
   * @throws IllegalArgumentException when the given source contains invalid encoded sequences
   * @see java.net.URLDecoder#decode(String, String)
   * @since 4.0
   */
  public static String uriDecode(String source, Charset charset) {
    int length = source.length();
    if (length == 0) {
      return source;
    }
    Assert.notNull(charset, "Charset must not be null");

    ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
    boolean changed = false;
    for (int i = 0; i < length; i++) {
      int ch = source.charAt(i);
      if (ch == '%') {
        if (i + 2 < length) {
          char hex1 = source.charAt(i + 1);
          char hex2 = source.charAt(i + 2);
          int u = Character.digit(hex1, 16);
          int l = Character.digit(hex2, 16);
          if (u == -1 || l == -1) {
            throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
          }
          baos.write((char) ((u << 4) + l));
          i += 2;
          changed = true;
        }
        else {
          throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
        }
      }
      else {
        baos.write(ch);
      }
    }
    return (changed ? StreamUtils.copyToString(baos, charset) : source);
  }

  //---------------------------------------------------------------------
  // Convenience methods for working with String arrays
  //---------------------------------------------------------------------

  /**
   * Take an array of strings and split each element based on the given delimiter.
   * A {@code Properties} instance is then generated, with the left of the delimiter
   * providing the key, and the right of the delimiter providing the value.
   * <p>Will trim both the key and value before adding them to the {@code Properties}.
   *
   * @param array the array to process
   * @param delimiter to split each element using (typically the equals symbol)
   * @return a {@code Properties} instance representing the array contents,
   * or {@code null} if the array to process was {@code null} or empty
   * @since 4.0
   */
  @Nullable
  public static Properties splitArrayElementsIntoProperties(String[] array, String delimiter) {
    return splitArrayElementsIntoProperties(array, delimiter, null);
  }

  /**
   * Take an array of strings and split each element based on the given delimiter.
   * A {@code Properties} instance is then generated, with the left of the
   * delimiter providing the key, and the right of the delimiter providing the value.
   * <p>Will trim both the key and value before adding them to the
   * {@code Properties} instance.
   *
   * @param array the array to process
   * @param delimiter to split each element using (typically the equals symbol)
   * @param charsToDelete one or more characters to remove from each element
   * prior to attempting the split operation (typically the quotation mark
   * symbol), or {@code null} if no removal should occur
   * @return a {@code Properties} instance representing the array contents,
   * or {@code null} if the array to process was {@code null} or empty
   * @since 4.0
   */
  @Nullable
  public static Properties splitArrayElementsIntoProperties(
          String[] array, String delimiter, @Nullable String charsToDelete) {

    if (ObjectUtils.isEmpty(array)) {
      return null;
    }

    Properties result = new Properties();
    for (String element : array) {
      if (charsToDelete != null) {
        element = deleteAny(element, charsToDelete);
      }
      String[] splittedElement = split(element, delimiter);
      if (splittedElement == null) {
        continue;
      }
      result.setProperty(splittedElement[0].trim(), splittedElement[1].trim());
    }
    return result;
  }

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
  public static String[] tokenizeToStringArray(@Nullable final String str, final String delimiters) {
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
  public static String[] tokenizeToStringArray(
          @Nullable String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {
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
      if (!ignoreEmptyTokens || !token.isEmpty()) {
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
      return Constant.EMPTY_STRING_ARRAY;
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
   * @return the array in sorted form (never {@code null})
   * @since 4.0
   */
  public static String[] sortArray(String[] array) {
    if (ObjectUtils.isEmpty(array)) {
      return array;
    }

    Arrays.sort(array);
    return array;
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
   * Convert a {@code String} array into a delimited {@code String} (e.g. CSV).
   * <p>Useful for {@code toString()} implementations.
   *
   * @param arr the array to display (potentially {@code null} or empty)
   * @param delim the delimiter to use (typically a ",")
   * @return the delimited {@code String}
   */
  public static String arrayToDelimitedString(@Nullable Object[] arr, String delim) {
    if (ObjectUtils.isEmpty(arr)) {
      return "";
    }
    if (arr.length == 1) {
      return ObjectUtils.nullSafeToString(arr[0]);
    }

    StringJoiner sj = new StringJoiner(delim);
    for (Object elem : arr) {
      sj.add(String.valueOf(elem));
    }
    return sj.toString();
  }

  /**
   * Convert a {@code String} array into a comma delimited {@code String}
   * (i.e., CSV).
   * <p>Useful for {@code toString()} implementations.
   *
   * @param arr the array to display (potentially {@code null} or empty)
   * @return the delimited {@code String}
   */
  public static String arrayToCommaDelimitedString(@Nullable Object[] arr) {
    return arrayToDelimitedString(arr, ",");
  }

  /**
   * Convert a {@link Collection} to a delimited {@code String} (e.g. CSV).
   * <p>Useful for {@code toString()} implementations.
   *
   * @param coll the {@code Collection} to convert (potentially {@code null} or empty)
   * @param delim the delimiter to use (typically a ",")
   * @param prefix the {@code String} to start each element with
   * @param suffix the {@code String} to end each element with
   * @return the delimited {@code String}
   * @since 2.1.7
   */
  public static String collectionToDelimitedString(
          @Nullable Collection<?> coll, String delim, String prefix, String suffix) {

    if (CollectionUtils.isEmpty(coll)) {
      return "";
    }

    int totalLength = coll.size() * (prefix.length() + suffix.length()) + (coll.size() - 1) * delim.length();
    for (Object element : coll) {
      totalLength += String.valueOf(element).length();
    }

    StringBuilder sb = new StringBuilder(totalLength);
    Iterator<?> it = coll.iterator();
    while (it.hasNext()) {
      sb.append(prefix).append(it.next()).append(suffix);
      if (it.hasNext()) {
        sb.append(delim);
      }
    }
    return sb.toString();
  }

  /**
   * Convert a {@code Collection} into a delimited {@code String} (e.g. CSV).
   * <p>Useful for {@code toString()} implementations.
   *
   * @param coll the {@code Collection} to convert (potentially {@code null} or empty)
   * @param delim the delimiter to use (typically a ",")
   * @return the delimited {@code String}
   * @since 4.0
   */
  public static String collectionToDelimitedString(@Nullable Collection<?> coll, String delim) {
    return collectionToDelimitedString(coll, delim, "", "");
  }

  /**
   * Convert a {@code Collection} into a delimited {@code String} (e.g., CSV).
   * <p>Useful for {@code toString()} implementations.
   *
   * @param coll the {@code Collection} to convert (potentially {@code null} or empty)
   * @return the delimited {@code String}
   * @since 2.1.7
   */
  public static String collectionToCommaDelimitedString(@Nullable Collection<?> coll) {
    return collectionToDelimitedString(coll, ",");
  }

  /**
   * Convert a comma delimited list (e.g., a row from a CSV file) into a set.
   * <p>Note that this will suppress duplicates, and as of 4.2, the elements in
   * the returned set will preserve the original order in a {@link LinkedHashSet}.
   *
   * @param str the input {@code String} (potentially {@code null} or empty)
   * @return a set of {@code String} entries in the list
   * @see #removeDuplicateStrings(String[])
   * @since 4.0
   */
  public static Set<String> commaDelimitedListToSet(@Nullable String str) {
    String[] tokens = commaDelimitedListToStringArray(str);
    return new LinkedHashSet<>(Arrays.asList(tokens));
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
      prefix = prefix + FOLDER_SEPARATOR_CHAR;
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

    return prefix.concat(collectionToDelimitedString(pathElements, FOLDER_SEPARATOR));
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
   * @param path Input path
   */
  public static String prependLeadingSlash(@Nullable String path) {
    if (StringUtils.isEmpty(path)) {
      return Constant.BLANK;
    }
    else {
      if (path.charAt(0) == '/') {
        return path;
      }
      return '/' + path;
    }
  }

  /**
   * Append line to {@link StringBuilder}
   *
   * @param reader String line read from {@link BufferedReader}
   * @param builder The {@link StringBuilder} append to
   * @throws IOException If an I/O error occurs
   */
  public static void appendLine(BufferedReader reader, StringBuilder builder) throws IOException {
    String line;
    while ((line = reader.readLine()) != null) {
      builder.append(line);
    }
  }

  /**
   * Count the occurrences of the substring {@code sub} in string {@code str}.
   *
   * @param str string to search in
   * @param sub string to search for
   */
  public static int countOccurrencesOf(String str, String sub) {
    if (isEmpty(str) || isEmpty(sub)) {
      return 0;
    }

    int count = 0;
    int pos = 0;
    int idx;
    while ((idx = str.indexOf(sub, pos)) != -1) {
      ++count;
      pos = idx + sub.length();
    }
    return count;
  }

  /**
   * Delete all occurrences of the given substring.
   *
   * @param inString the original {@code String}
   * @param pattern the pattern to delete all occurrences of
   * @return the resulting {@code String}
   * @since 4.0
   */
  public static String delete(String inString, String pattern) {
    return replace(inString, pattern, "");
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

  //---------------------------------------------------------------------
  // Convenience methods for working with formatted Strings
  //---------------------------------------------------------------------

  /**
   * Quote the given {@code String} with single quotes.
   *
   * @param str the input {@code String} (e.g. "myString")
   * @return the quoted {@code String} (e.g. "'myString'"),
   * or {@code null} if the input was {@code null}
   * @since 4.0
   */
  @Nullable
  public static String quote(@Nullable String str) {
    return (str != null ? "'" + str + "'" : null);
  }

  /**
   * Turn the given Object into a {@code String} with single quotes
   * if it is a {@code String}; keeping the Object as-is else.
   *
   * @param obj the input Object (e.g. "myString")
   * @return the quoted {@code String} (e.g. "'myString'"),
   * or the input object as-is if not a {@code String}
   * @since 4.0
   */
  @Nullable
  public static Object quoteIfString(@Nullable Object obj) {
    return obj instanceof String ? quote((String) obj) : obj;
  }

  /**
   * Unqualify a string qualified by a '.' dot character. For example,
   * "this.name.is.qualified", returns "qualified".
   *
   * @param qualifiedName the qualified name
   * @since 4.0
   */
  public static String unqualify(String qualifiedName) {
    return unqualify(qualifiedName, '.');
  }

  /**
   * Unqualify a string qualified by a separator character. For example,
   * "this:name:is:qualified" returns "qualified" if using a ':' separator.
   *
   * @param qualifiedName the qualified name
   * @param separator the separator
   * @since 4.0
   */
  public static String unqualify(String qualifiedName, char separator) {
    return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
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

  /**
   * Uncapitalize a {@code String} in JavaBeans property format,
   * changing the first letter to lower case as per
   * {@link Character#toLowerCase(char)}, unless the initial two
   * letters are upper case in direct succession.
   *
   * @param str the {@code String} to uncapitalize
   * @return the uncapitalized {@code String}
   * @see java.beans.Introspector#decapitalize(String)
   * @since 4.0
   */
  public static String uncapitalizeAsProperty(String str) {
    if (isEmpty(str) || (str.length() > 1 && Character.isUpperCase(str.charAt(0))
            && Character.isUpperCase(str.charAt(1)))) {
      return str;
    }
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
    return new String(chars);
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
   * Check whether the given {@code String} contains actual <em>text</em>.
   * <p>More specifically, this method returns {@code true} if the
   * {@code String} is {@code null}, its length is greater than 0,
   * and it contains whitespace character only.
   *
   * <pre>{@code
   * StringUtils.isBlank(null) = true
   * StringUtils.isBlank("") = true
   * StringUtils.isBlank(" ") = true
   * StringUtils.isBlank("12345") = false
   * StringUtils.isBlank(" 12345 ") = false
   * }</pre>
   *
   * @param str the {@code String} to check (may be {@code null})
   * @return {@code true} if the {@code String} is {@code null}, its
   * length is greater than 0, and it contains whitespace only
   * @see #hasText(CharSequence)
   * @see #isNotEmpty(CharSequence)
   * @see Character#isWhitespace
   * @since 4.0
   */
  public static boolean isBlank(@Nullable String str) {
    return !hasText(str);
  }

  /**
   * Check whether the given {@code CharSequence} contains actual <em>text</em>.
   * <p>
   * More specifically, this method returns {@code true} if the
   * {@code CharSequence} is {@code null} or its length is greater than 0, and
   * it contains whitespace character only.
   * <p>
   * <pre>{@code
   * StringUtils.isBlank(null) = true
   * StringUtils.isBlank("") = true
   * StringUtils.isBlank(" ") = true
   * StringUtils.isBlank("12345") = false
   * StringUtils.isBlank(" 12345 ") = false
   * }</pre>
   *
   * @param str the {@code CharSequence} to check (may be {@code null})
   * @return {@code true} if the {@code CharSequence} is not {@code null}, its
   * length is greater than 0, and it contains whitespace only
   * @see Character#isWhitespace
   * @since 4.0
   */
  public static boolean isBlank(@Nullable CharSequence str) {
    return !hasText(str);
  }

  /**
   * Extract the filename from the given Java resource path, e.g.
   * {@code "mypath/myfile.txt" -> "myfile.txt"}.
   *
   * @param path the file path (may be {@code null})
   * @return the extracted filename, or {@code null} if none
   */
  @Nullable
  public static String getFilename(@Nullable String path) {
    if (path == null) {
      return null;
    }
    int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
    return (separatorIndex != -1 ? path.substring(separatorIndex + 1) : path);
  }

  /**
   * Extract the filename extension from the given Java resource path, e.g.
   * "mypath/myfile.txt" -> "txt".
   *
   * @param path the file path (may be {@code null})
   * @return the extracted filename extension, or {@code null} if none
   */
  @Nullable
  public static String getFilenameExtension(@Nullable String path) {
    if (path == null) {
      return null;
    }

    int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
    if (extIndex == -1) {
      return null;
    }

    int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
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

    int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
    if (folderIndex > extIndex) {
      return path;
    }

    return path.substring(0, extIndex);
  }

  /**
   * Apply the given relative path to the given Java resource path,
   * assuming standard Java folder separation (i.e. "/" separators).
   *
   * @param path the path to start from (usually a full file path)
   * @param relativePath the relative path to apply
   * (relative to the full file path above)
   * @return the full file path that results from applying the relative path
   */
  public static String applyRelativePath(String path, String relativePath) {
    int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
    if (separatorIndex != -1) {
      String newPath = path.substring(0, separatorIndex);
      if (!relativePath.startsWith(FOLDER_SEPARATOR)) {
        newPath += FOLDER_SEPARATOR_CHAR;
      }
      return newPath + relativePath;
    }
    else {
      return relativePath;
    }
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
  public static boolean simpleMatch(@Nullable String pattern, @Nullable String str) {
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
      case 0 -> {//随机小写字母
        rand = random.nextInt(26);
        rand += 97;
        return (char) rand;
      }
      case 1 -> {//随机大写字母
        rand = random.nextInt(26);
        rand += 65;
        return (char) rand;
      }//随机数字
      default -> {
        rand = random.nextInt(10);
        rand += 48;
        return (char) rand;
      }
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
  @Nullable
  public static String trimWhitespace(@Nullable String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.strip();
  }

  /**
   * Trim <em>all</em> whitespace from the given {@code String}:
   * leading, trailing, and in between characters.
   *
   * @param str the {@code String} to check
   * @return the trimmed {@code String}
   * @see #trimAllWhitespace(CharSequence)
   * @see java.lang.Character#isWhitespace
   * @since 3.0
   */
  @Nullable
  public static String trimAllWhitespace(@Nullable String str) {
    if (str == null) {
      return null;
    }
    return trimAllWhitespace((CharSequence) str).toString();
  }

  /**
   * Trim <em>all</em> whitespace from the given {@code CharSequence}:
   * leading, trailing, and in between characters.
   *
   * @param text the {@code CharSequence} to check
   * @return the trimmed {@code CharSequence}
   * @see #trimAllWhitespace(String)
   * @see java.lang.Character#isWhitespace
   * @since 4.0
   */
  public static CharSequence trimAllWhitespace(CharSequence text) {
    if (isEmpty(text)) {
      return text;
    }

    int len = text.length();
    StringBuilder sb = new StringBuilder(text.length());
    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);
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
  @Nullable
  public static String trimLeadingWhitespace(@Nullable String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.stripLeading();
  }

  /**
   * Trim trailing whitespace from the given {@code String}.
   *
   * @param str the {@code String} to check
   * @return the trimmed {@code String}
   * @see java.lang.Character#isWhitespace
   * @since 3.0
   */
  @Nullable
  public static String trimTrailingWhitespace(@Nullable String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.stripTrailing();
  }

  /**
   * Trim all occurrences of the supplied leading character from the given {@code String}.
   *
   * @param str the {@code String} to check
   * @param leadingCharacter the leading character to be trimmed
   * @return the trimmed {@code String}
   * @since 3.0
   */
  @Nullable
  public static String trimLeadingCharacter(@Nullable String str, char leadingCharacter) {
    if (str == null || str.isEmpty()) {
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
  @Nullable
  public static String trimTrailingCharacter(@Nullable String str, char trailingCharacter) {
    if (str == null || str.isEmpty()) {
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
  public static boolean matchesCharacter(@Nullable String str, char singleCharacter) {
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
    return str != null && !str.isEmpty() && str.charAt(0) == charToMatch;
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
  @Nullable
  public static Locale parseLocale(String localeValue) {
    if (!localeValue.contains("_") && !localeValue.contains(" ")) {
      validateLocalePart(localeValue);
      Locale resolved = Locale.forLanguageTag(localeValue);
      if (!resolved.getLanguage().isEmpty()) {
        return resolved;
      }
    }
    return parseLocaleString(localeValue);
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
  @Nullable
  public static Locale parseLocaleString(@Nullable String localeString) {
    if (localeString == null || localeString.isEmpty()) {
      return null;
    }
    String delimiter = "_";
    if (!localeString.contains("_") && localeString.contains(" ")) {
      delimiter = " ";
    }
    final String[] tokens = localeString.split(delimiter, -1);
    if (tokens.length == 1) {
      final String language = tokens[0];
      validateLocalePart(language);
      return new Locale(language);
    }
    else if (tokens.length == 2) {
      final String language = tokens[0];
      validateLocalePart(language);
      final String country = tokens[1];
      validateLocalePart(country);
      return new Locale(language, country);
    }
    else if (tokens.length > 2) {
      final String language = tokens[0];
      validateLocalePart(language);
      final String country = tokens[1];
      validateLocalePart(country);
      final String variant = Arrays.stream(tokens).skip(2).collect(Collectors.joining(delimiter));
      return new Locale(language, country, variant);
    }
    throw new IllegalArgumentException("Invalid locale format: '" + localeString + "'");
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
   * <p>
   * Convert a property name using "camelCase" to a corresponding column name with underscores.
   * A name like "customerNumber" would match a "customer_number" column name.
   *
   * @param name the original name
   * @return the converted name
   * @since 4.0
   */
  public static String camelCaseToUnderscore(@Nullable String name) {
    if (StringUtils.isEmpty(name)) {
      return Constant.BLANK;
    }
    final int length = name.length();
    StringBuilder result = new StringBuilder(length + 1);
    result.append(Character.toLowerCase(name.charAt(0)));
    for (int i = 1; i < length; i++) {
      char c = name.charAt(i);
      if (Character.isUpperCase(c)) {
        result.append('_').append(Character.toLowerCase(c));
      }
      else {
        result.append(c);
      }
    }
    return result.toString();
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

  /**
   * Truncate the supplied {@link CharSequence}.
   * <p>Delegates to {@link #truncate(CharSequence, int)}, supplying {@code 100}
   * as the threshold.
   *
   * @param charSequence the {@code CharSequence} to truncate
   * @return a truncated string, or a string representation of the original
   * {@code CharSequence} if its length does not exceed the threshold
   * @since 4.0
   */
  public static String truncate(CharSequence charSequence) {
    return truncate(charSequence, DEFAULT_TRUNCATION_THRESHOLD);
  }

  /**
   * Truncate the supplied {@link CharSequence}.
   * <p>If the length of the {@code CharSequence} is greater than the threshold,
   * this method returns a {@linkplain CharSequence#subSequence(int, int)
   * subsequence} of the {@code CharSequence} (up to the threshold) appended
   * with the suffix {@code " (truncated)..."}. Otherwise, this method returns
   * {@code charSequence.toString()}.
   *
   * @param charSequence the {@code CharSequence} to truncate
   * @param threshold the maximum length after which to truncate; must be a
   * positive number
   * @return a truncated string, or a string representation of the original
   * {@code CharSequence} if its length does not exceed the threshold
   * @since 4.0
   */
  public static String truncate(CharSequence charSequence, int threshold) {
    Assert.isTrue(threshold > 0,
            () -> "Truncation threshold must be a positive number: " + threshold);
    if (charSequence.length() > threshold) {
      return charSequence.subSequence(0, threshold) + TRUNCATION_SUFFIX;
    }
    return charSequence.toString();
  }

}
