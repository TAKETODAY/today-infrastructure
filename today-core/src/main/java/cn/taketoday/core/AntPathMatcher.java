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

package cn.taketoday.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link PathMatcher} implementation for Ant-style path patterns.
 *
 * <p>
 * Part of this mapping code has been kindly borrowed from
 * <a href="https://ant.apache.org">Apache Ant</a>.
 *
 * <p>
 * The mapping matches URLs using the following rules:<br>
 * <ul>
 * <li>{@code ?} matches one character</li>
 * <li>{@code *} matches zero or more characters</li>
 * <li>{@code **} matches zero or more <em>directories</em> in a path</li>
 * <li>{@code {spring:[a-z]+}} matches the regexp {@code [a-z]+} as a path
 * variable named "spring"</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <ul>
 * <li>{@code com/t?st.jsp} &mdash; matches {@code com/test.jsp} but also
 * {@code com/tast.jsp} or {@code com/txst.jsp}</li>
 * <li>{@code com/*.jsp} &mdash; matches all {@code .jsp} files in the
 * {@code com} directory</li>
 * <li><code>com/&#42;&#42;/test.jsp</code> &mdash; matches all {@code test.jsp}
 * files underneath the {@code com} path</li>
 * <li><code>cn/taketoday/&#42;&#42;/*.jsp</code> &mdash; matches all
 * {@code .jsp} files underneath the {@code cn/taketoday} path</li>
 * <li><code>org/&#42;&#42;/servlet/bla.jsp</code> &mdash; matches
 * {@code cn/taketoday/servlet/bla.jsp} but also
 * {@code cn/taketoday/testing/servlet/bla.jsp} and
 * {@code org/servlet/bla.jsp}</li>
 * <li>{@code com/{filename:\\w+}.jsp} will match {@code com/test.jsp} and
 * assign the value {@code test} to the {@code filename} variable</li>
 * </ul>
 *
 * <p>
 * <strong>Note:</strong> a pattern and a path must both be absolute or must
 * both be relative in order for the two to match. Therefore it is recommended
 * that users of this implementation to sanitize patterns in order to prefix
 * them with "/" as it makes sense in the context in which they're used.
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.1.7 2019-03-26 10:20
 */
public class AntPathMatcher implements PathMatcher {

  /** Default path separator: "/". */
  public static final String DEFAULT_PATH_SEPARATOR = "/";
  private static final int CACHE_TURNOFF_THRESHOLD = 65536;

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^/]+?\\}");

  private static final char[] WILDCARD_CHARS = { '*', '?', '{' };

  private String pathSeparator;

  private boolean caseSensitive = true;

  private boolean trimTokens = false;

  private PathSeparatorPatternCache pathSeparatorPatternCache;

  @Nullable
  private volatile Boolean cachePatterns;

  private final ConcurrentHashMap<String, String[]> tokenizedPatternCache = new ConcurrentHashMap<>(256);

  final ConcurrentHashMap<String, AntPathStringMatcher> stringMatcherCache = new ConcurrentHashMap<>(256);

  /**
   * Create a new instance with the {@link #DEFAULT_PATH_SEPARATOR}.
   */
  public AntPathMatcher() {
    this.pathSeparator = DEFAULT_PATH_SEPARATOR;
    this.pathSeparatorPatternCache = new PathSeparatorPatternCache(DEFAULT_PATH_SEPARATOR);
  }

  /**
   * A convenient, alternative constructor to use with a custom path separator.
   *
   * @param pathSeparator the path separator to use, must not be {@code null}.
   * @since 4.0
   */
  public AntPathMatcher(String pathSeparator) {
    Assert.notNull(pathSeparator, "'pathSeparator' is required");
    this.pathSeparator = pathSeparator;
    this.pathSeparatorPatternCache = new PathSeparatorPatternCache(pathSeparator);
  }

  /**
   * Set the path separator to use for pattern parsing.
   * <p>
   * Default is "/", as in Ant.
   */
  public void setPathSeparator(@Nullable String pathSeparator) {
    this.pathSeparator = (pathSeparator != null ? pathSeparator : DEFAULT_PATH_SEPARATOR);
    this.pathSeparatorPatternCache = new PathSeparatorPatternCache(this.pathSeparator);
  }

  /**
   * Specify whether to perform pattern matching in a case-sensitive fashion.
   * <p>
   * Default is {@code true}. Switch this to {@code false} for case-insensitive
   * matching.
   */
  public void setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  /**
   * Specify whether to trim tokenized paths and patterns.
   * <p>
   * Default is {@code false}.
   */
  public void setTrimTokens(boolean trimTokens) {
    this.trimTokens = trimTokens;
  }

  /**
   * Specify whether to cache parsed pattern metadata for patterns passed
   * into this matcher's {@link #match} method. A value of {@code true}
   * activates an unlimited pattern cache; a value of {@code false} turns
   * the pattern cache off completely.
   * <p>Default is for the cache to be on, but with the variant to automatically
   * turn it off when encountering too many patterns to cache at runtime
   * (the threshold is 65536), assuming that arbitrary permutations of patterns
   * are coming in, with little chance for encountering a recurring pattern.
   *
   * @see #getStringMatcher(String)
   * @since 4.0
   */
  public void setCachePatterns(boolean cachePatterns) {
    this.cachePatterns = cachePatterns;
  }

  private void deactivatePatternCache() {
    this.cachePatterns = false;
    this.tokenizedPatternCache.clear();
    this.stringMatcherCache.clear();
  }

  @Override
  public boolean isPattern(@Nullable final String path) {
    if (path != null) {
      boolean uriVar = false;
      final int length = path.length();
      for (int i = 0; i < length; i++) {
        final char c = path.charAt(i);
        if (c == '*' || c == '?') {
          return true;
        }
        if (c == '{') {
          uriVar = true;
          continue;
        }
        if (c == '}' && uriVar) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean match(final String pattern, final String path) {
    return doMatch(pattern, path, true, null);
  }

  @Override
  public boolean matchStart(final String pattern, final String path) {
    return doMatch(pattern, path, false, null);
  }

  /**
   * Actually match the given {@code path} against the given {@code pattern}.
   *
   * @param pattern the pattern to match against
   * @param path the path String to test
   * @param fullMatch whether a full pattern match is required (else a pattern match as
   * far as the given base path goes is sufficient)
   * @return {@code true} if the supplied {@code path} matched, {@code false} if
   * it didn't
   */
  protected boolean doMatch(String pattern, @Nullable String path, boolean fullMatch, @Nullable Map<String, String> uriTemplateVariables) {

    final String pathSeparator;
    if (path == null || path.startsWith(pathSeparator = this.pathSeparator) != pattern.startsWith(pathSeparator)) {
      return false;
    }

    String[] pattDirs = tokenizePattern(pattern);
    if (fullMatch && this.caseSensitive && !isPotentialMatch(pathSeparator, path, pattDirs)) {
      return false;
    }

    String[] pathDirs = tokenizePath(path);

    int pattIdxStart = 0;
    int pattIdxEnd = pattDirs.length - 1;
    int pathIdxStart = 0;
    int pathIdxEnd = pathDirs.length - 1;

    // Match all elements up to the first **
    while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
      String pattDir = pattDirs[pattIdxStart];
      if ("**".equals(pattDir)) {
        break;
      }
      if (!matchStrings(pattDir, pathDirs[pathIdxStart], uriTemplateVariables)) {
        return false;
      }
      pattIdxStart++;
      pathIdxStart++;
    }

    if (pathIdxStart > pathIdxEnd) {
      // Path is exhausted, only match if rest of pattern is * or **'s
      if (pattIdxStart > pattIdxEnd) {
        return (pattern.endsWith(pathSeparator) == path.endsWith(pathSeparator));
      }
      if (!fullMatch) {
        return true;
      }
      if (pattIdxStart == pattIdxEnd && "*".equals(pattDirs[pattIdxStart]) && path.endsWith(pathSeparator)) {
        return true;
      }
      for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
        if (!"**".equals(pattDirs[i])) {
          return false;
        }
      }
      return true;
    }
    else if (pattIdxStart > pattIdxEnd) {
      // String not exhausted, but pattern is. Failure.
      return false;
    }
    else if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
      // Path start definitely matches due to "**" part in pattern.
      return true;
    }

    // up to last '**'
    while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
      final String pattDir = pattDirs[pattIdxEnd];
      if ("**".equals(pattDir)) {
        break;
      }
      if (!matchStrings(pattDir, pathDirs[pathIdxEnd], uriTemplateVariables)) {
        return false;
      }
      if (pattIdxEnd == (pattDirs.length - 1)
              && pattern.endsWith(this.pathSeparator) != path.endsWith(this.pathSeparator)) {
        return false;
      }
      pattIdxEnd--;
      pathIdxEnd--;
    }
    if (pathIdxStart > pathIdxEnd) {
      // String is exhausted
      for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
        if (!"**".equals(pattDirs[i])) {
          return false;
        }
      }
      return true;
    }

    while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
      int patIdxTmp = -1;
      for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
        if ("**".equals(pattDirs[i])) {
          patIdxTmp = i;
          break;
        }
      }
      if (patIdxTmp == pattIdxStart + 1) {
        // '**/**' situation, so skip one
        pattIdxStart++;
        continue;
      }
      // Find the pattern between padIdxStart & padIdxTmp in str between
      // strIdxStart & strIdxEnd
      int patLength = (patIdxTmp - pattIdxStart - 1);
      int strLength = (pathIdxEnd - pathIdxStart + 1);
      int foundIdx = -1;

      strLoop:
      for (int i = 0; i <= strLength - patLength; i++) {
        for (int j = 0; j < patLength; j++) {
          String subPat = pattDirs[pattIdxStart + j + 1];
          String subStr = pathDirs[pathIdxStart + i + j];
          if (!matchStrings(subPat, subStr, uriTemplateVariables)) {
            continue strLoop;
          }
        }
        foundIdx = pathIdxStart + i;
        break;
      }

      if (foundIdx == -1) {
        return false;
      }

      pattIdxStart = patIdxTmp;
      pathIdxStart = foundIdx + patLength;
    }

    for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
      if (!"**".equals(pattDirs[i])) {
        return false;
      }
    }

    return true;
  }

  private boolean isPotentialMatch(final String pathSeparator, final String path, final String[] pattDirs) {
    if (!this.trimTokens) {
      int pos = 0;
      for (String pattDir : pattDirs) {
        int skipped = skipSeparator(path, pos, pathSeparator);
        pos += skipped;
        skipped = skipSegment(path, pos, pattDir);
        if (skipped < pattDir.length()) {
          return (skipped > 0 || (!pattDir.isEmpty() && isWildcardChar(pattDir.charAt(0))));
        }
        pos += skipped;
      }
    }
    return true;
  }

  private int skipSegment(String path, int pos, String prefix) {
    int skipped = 0;
    for (int i = 0; i < prefix.length(); i++) {
      char c = prefix.charAt(i);
      if (isWildcardChar(c)) {
        return skipped;
      }
      int currPos = pos + skipped;
      if (currPos >= path.length()) {
        return 0;
      }
      if (c == path.charAt(currPos)) {
        skipped++;
      }
    }
    return skipped;
  }

  private int skipSeparator(String path, int pos, String separator) {
    int skipped = 0;
    while (path.startsWith(separator, pos + skipped)) {
      skipped += separator.length();
    }
    return skipped;
  }

  private boolean isWildcardChar(char c) {
    for (char candidate : WILDCARD_CHARS) {
      if (c == candidate) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tokenize the given path pattern into parts, based on this matcher's settings.
   * <p>
   *
   * @param pattern the pattern to tokenize
   * @return the tokenized pattern parts
   */
  protected String[] tokenizePattern(final String pattern) {
    String[] tokenized = null;
    Boolean cachePatterns = this.cachePatterns;
    if (cachePatterns == null || cachePatterns) {
      tokenized = this.tokenizedPatternCache.get(pattern);
    }
    if (tokenized == null) {
      tokenized = tokenizePath(pattern);
      if (cachePatterns == null && this.tokenizedPatternCache.size() >= CACHE_TURNOFF_THRESHOLD) {
        // Try to adapt to the runtime situation that we're encountering:
        // There are obviously too many different patterns coming in here...
        // So let's turn off the cache since the patterns are unlikely to be reoccurring.
        deactivatePatternCache();
        return tokenized;
      }
      if (cachePatterns == null || cachePatterns) {
        this.tokenizedPatternCache.put(pattern, tokenized);
      }
    }
    return tokenized;
  }

  /**
   * Tokenize the given path String into parts, based on this matcher's settings.
   *
   * @param path the path to tokenize
   * @return the tokenized path parts
   */
  protected String[] tokenizePath(final String path) {
    return StringUtils.tokenizeToStringArray(path, pathSeparator, this.trimTokens, true);
  }

  /**
   * Test whether or not a string matches against a pattern.
   *
   * @param pattern the pattern to match against (never {@code null})
   * @param str the String which must be matched against the pattern (never
   * {@code null})
   * @return {@code true} if the string matches against the pattern, or
   * {@code false} otherwise
   */
  private boolean matchStrings(String pattern, String str, @Nullable Map<String, String> uriTemplateVariables) {
    return getStringMatcher(pattern).matchStrings(str, uriTemplateVariables);
  }

  /**
   * Build or retrieve an {@link AntPathStringMatcher} for the given pattern.
   * <p>
   * When encountering too many patterns to cache at runtime (the threshold is
   * 65536), it turns the default cache off, assuming that arbitrary permutations
   * of patterns are coming in, with little chance for encountering a recurring
   * pattern.
   * <p>
   * This method may be overridden to implement a custom cache strategy.
   *
   * @param pattern the pattern to match against (never {@code null})
   * @return a corresponding AntPathStringMatcher (never {@code null})
   */
  protected AntPathStringMatcher getStringMatcher(String pattern) {
    AntPathStringMatcher matcher = null;
    Boolean cachePatterns = this.cachePatterns;
    if (cachePatterns == null || cachePatterns) {
      matcher = this.stringMatcherCache.get(pattern);
    }
    if (matcher == null) {
      matcher = new AntPathStringMatcher(pattern, this.caseSensitive);
      if (cachePatterns == null && this.stringMatcherCache.size() >= CACHE_TURNOFF_THRESHOLD) {
        // Try to adapt to the runtime situation that we're encountering:
        // There are obviously too many different patterns coming in here...
        // So let's turn off the cache since the patterns are unlikely to be reoccurring.
        deactivatePatternCache();
        return matcher;
      }
      if (cachePatterns == null || cachePatterns) {
        this.stringMatcherCache.put(pattern, matcher);
      }
    }
    return matcher;
  }

  /**
   * Given a pattern and a full path, determine the pattern-mapped part.
   * <p>
   * For example:
   * <ul>
   * <li>'{@code /docs/cvs/commit.html}' and '{@code /docs/cvs/commit.html} ->
   * ''</li>
   * <li>'{@code /docs/*}' and '{@code /docs/cvs/commit} ->
   * '{@code cvs/commit}'</li>
   * <li>'{@code /docs/cvs/*.html}' and '{@code /docs/cvs/commit.html} ->
   * '{@code commit.html}'</li>
   * <li>'{@code /docs/**}' and '{@code /docs/cvs/commit} ->
   * '{@code cvs/commit}'</li>
   * <li>'{@code /docs/**\/*.html}' and '{@code /docs/cvs/commit.html} ->
   * '{@code cvs/commit.html}'</li>
   * <li>'{@code /*.html}' and '{@code /docs/cvs/commit.html} ->
   * '{@code docs/cvs/commit.html}'</li>
   * <li>'{@code *.html}' and '{@code /docs/cvs/commit.html} ->
   * '{@code /docs/cvs/commit.html}'</li>
   * <li>'{@code *}' and '{@code /docs/cvs/commit.html} ->
   * '{@code /docs/cvs/commit.html}'</li>
   * </ul>
   * <p>
   * Assumes that {@link #match} returns {@code true} for '{@code pattern}' and
   * '{@code path}', but does <strong>not</strong> enforce this.
   */
  @Override
  public String extractPathWithinPattern(String pattern, String path) {
    String pathSeparator = this.pathSeparator;

    String[] patternParts = tokenizePattern(pattern);
    String[] pathParts = StringUtils.tokenizeToStringArray(path, pathSeparator, this.trimTokens, true);

    StringBuilder builder = new StringBuilder();
    boolean pathStarted = false;

    for (int segment = 0; segment < patternParts.length; segment++) {
      String patternPart = patternParts[segment];
      if (patternPart.indexOf('*') > -1 || patternPart.indexOf('?') > -1) {
        for (; segment < pathParts.length; segment++) {
          if (pathStarted || (segment == 0 && !pattern.startsWith(pathSeparator))) {
            builder.append(pathSeparator);
          }
          builder.append(pathParts[segment]);
          pathStarted = true;
        }
      }
    }

    return builder.toString();
  }

  @Override
  public Map<String, String> extractUriTemplateVariables(String pattern, String path) {
    Map<String, String> variables = new LinkedHashMap<>();
    if (doMatch(pattern, path, true, variables)) {
      return variables;
    }
    throw new IllegalStateException("Pattern \"" + pattern + "\" is not a match for \"" + path + "\"");
  }

  @Override
  public String[] extractVariables(String pattern, String path) {
    final String[] variables = getStringMatcher(pattern).extractVariables(path);
    if (variables == null) {
      throw new IllegalStateException("Pattern \"" + pattern + "\" is not a match for \"" + path + "\"");
    }
    return variables;
  }

  @Override
  public List<String> extractVariableNames(String pattern) {
    if (isPattern(pattern)) {
      return getStringMatcher(pattern).variableNames;
    }
    return Collections.emptyList();
  }

  /**
   * Combine two patterns into a new pattern.
   * <p>
   * This implementation simply concatenates the two patterns, unless the first
   * pattern contains a file extension match (e.g., {@code *.html}). In that case,
   * the second pattern will be merged into the first. Otherwise, an
   * {@code IllegalArgumentException} will be thrown.
   * <h3>Examples</h3>
   * <table border="1">
   * <tr>
   * <th>Pattern 1</th>
   * <th>Pattern 2</th>
   * <th>Result</th>
   * </tr>
   * <tr>
   * <td>{@code null}</td>
   * <td>{@code null}</td>
   * <td>&nbsp;</td>
   * </tr>
   * <tr>
   * <td>/hotels</td>
   * <td>{@code null}</td>
   * <td>/hotels</td>
   * </tr>
   * <tr>
   * <td>{@code null}</td>
   * <td>/hotels</td>
   * <td>/hotels</td>
   * </tr>
   * <tr>
   * <td>/hotels</td>
   * <td>/bookings</td>
   * <td>/hotels/bookings</td>
   * </tr>
   * <tr>
   * <td>/hotels</td>
   * <td>bookings</td>
   * <td>/hotels/bookings</td>
   * </tr>
   * <tr>
   * <td>/hotels/*</td>
   * <td>/bookings</td>
   * <td>/hotels/bookings</td>
   * </tr>
   * <tr>
   * <td>/hotels/&#42;&#42;</td>
   * <td>/bookings</td>
   * <td>/hotels/&#42;&#42;/bookings</td>
   * </tr>
   * <tr>
   * <td>/hotels</td>
   * <td>{hotel}</td>
   * <td>/hotels/{hotel}</td>
   * </tr>
   * <tr>
   * <td>/hotels/*</td>
   * <td>{hotel}</td>
   * <td>/hotels/{hotel}</td>
   * </tr>
   * <tr>
   * <td>/hotels/&#42;&#42;</td>
   * <td>{hotel}</td>
   * <td>/hotels/&#42;&#42;/{hotel}</td>
   * </tr>
   * <tr>
   * <td>/*.html</td>
   * <td>/hotels.html</td>
   * <td>/hotels.html</td>
   * </tr>
   * <tr>
   * <td>/*.html</td>
   * <td>/hotels</td>
   * <td>/hotels.html</td>
   * </tr>
   * <tr>
   * <td>/*.html</td>
   * <td>/*.txt</td>
   * <td>{@code IllegalArgumentException}</td>
   * </tr>
   * </table>
   *
   * @param pattern1 the first pattern
   * @param pattern2 the second pattern
   * @return the combination of the two patterns
   * @throws IllegalArgumentException if the two patterns cannot be combined
   */
  @Override
  public String combine(final String pattern1, final String pattern2) {

//        if (StringUtils.isEmpty(pattern1) && StringUtils.isEmpty(pattern2)) 
//            return Constant.BLANK;
//        if (StringUtils.isEmpty(pattern1))
//            return pattern2;
//        if (StringUtils.isEmpty(pattern2)) 
//            return pattern1;

    if (StringUtils.isEmpty(pattern1)) {
      if (StringUtils.isEmpty(pattern2)) {
        return Constant.BLANK;
      }
      return pattern2;
    }

    if (StringUtils.isEmpty(pattern2)) {
      if (StringUtils.isEmpty(pattern1)) {
        return Constant.BLANK;
      }
      return pattern1;
    }

    final boolean pattern1ContainsUriVar = (pattern1.indexOf('{') != -1);
    if (!pattern1.equals(pattern2) && !pattern1ContainsUriVar && match(pattern1, pattern2)) {
      // /* + /hotel -> /hotel ; "/*.*" + "/*.html" -> /*.html
      // However /user + /user -> /usr/user ; /{foo} + /bar -> /{foo}/bar
      return pattern2;
    }

    // /hotels/* + /booking -> /hotels/booking
    // /hotels/* + booking -> /hotels/booking
    if (pattern1.endsWith(this.pathSeparatorPatternCache.endsOnWildCard)) {
      return concat(pattern1.substring(0, pattern1.length() - 2), pattern2);
    }

    // /hotels/** + /booking -> /hotels/**/booking
    // /hotels/** + booking -> /hotels/**/booking
    if (pattern1.endsWith(this.pathSeparatorPatternCache.endsOnDoubleWildCard)) {
      return concat(pattern1, pattern2);
    }

    final int starDotPos1 = pattern1.indexOf("*.");
    if (pattern1ContainsUriVar || starDotPos1 == -1 || pathSeparator.equals(".")) {
      // simply concatenate the two patterns
      return concat(pattern1, pattern2);
    }

    final String ext1 = pattern1.substring(starDotPos1 + 1);
    final int dotPos2 = pattern2.indexOf('.');
    final String file2 = (dotPos2 == -1 ? pattern2 : pattern2.substring(0, dotPos2));
    final String ext2 = (dotPos2 == -1 ? Constant.BLANK : pattern2.substring(dotPos2));
    final boolean ext1All = (ext1.equals(".*") || ext1.isEmpty());
    final boolean ext2All = (ext2.equals(".*") || ext2.isEmpty());
    if (!ext1All && !ext2All) {
      throw new IllegalArgumentException("Cannot combine patterns: " + pattern1 + " vs " + pattern2);
    }
    return file2.concat(ext1All ? ext2 : ext1);
  }

  private String concat(String path1, String path2) {

    final String pathSeparator = this.pathSeparator;

    final boolean path1EndsWithSeparator = path1.endsWith(pathSeparator);
    final boolean path2StartsWithSeparator = path2.startsWith(pathSeparator);

    if (path1EndsWithSeparator && path2StartsWithSeparator) {
      return path1.concat(path2.substring(1));
    }
    if (path1EndsWithSeparator || path2StartsWithSeparator) {
      return path1.concat(path2);
    }

    return new StringBuilder(pathSeparator.length() + path1.length() + path2.length())
            .append(path1)
            .append(pathSeparator)
            .append(path2)
            .toString();
  }

  /**
   * Given a full path, returns a {@link Comparator} suitable for sorting patterns
   * in order of explicitness.
   * <p>
   * This{@code Comparator} will {@linkplain java.util.List#sort(Comparator) sort}
   * a list so that more specific patterns (without uri templates or wild cards)
   * come before generic patterns. So given a list with the following patterns:
   * <ol>
   * <li>{@code /hotels/new}</li>
   * <li>{@code /hotels/{hotel}}</li>
   * <li>{@code /hotels/*}</li>
   * </ol>
   * the returned comparator will sort this list so that the order will be as
   * indicated.
   * <p>
   * The full path given as parameter is used to test for exact matches. So when
   * the given path is {@code /hotels/2}, the pattern {@code /hotels/2} will be
   * sorted before {@code /hotels/1}.
   *
   * @param path the full path to use for comparison
   * @return a comparator capable of sorting patterns in order of explicitness
   */
  @Override
  public Comparator<String> getPatternComparator(String path) {
    return new AntPatternComparator(path);
  }

  /**
   * Tests whether or not a string matches against a pattern via a
   * {@link Pattern}.
   * <p>
   * The pattern may contain special characters: '*' means zero or more
   * characters; '?' means one and only one character; '{' and '}' indicate a URI
   * template pattern. For example <tt>/users/{user}</tt>.
   */
  protected static class AntPathStringMatcher {

    private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

    private static final String DEFAULT_VARIABLE_PATTERN = "(.*)";

    @Nullable
    private final Pattern pattern;

    private final ArrayList<String> variableNames;

    private final String rawPattern;

    private final boolean caseSensitive;

    private final boolean exactMatch;

    public AntPathStringMatcher(final String pattern, final boolean caseSensitive) {
      this.rawPattern = pattern;
      this.caseSensitive = caseSensitive;
      StringBuilder patternBuilder = new StringBuilder();
      Matcher matcher = GLOB_PATTERN.matcher(pattern);
      int end = 0;
      final ArrayList<String> variableNames = new ArrayList<>(4);
      while (matcher.find()) {
        patternBuilder.append(quote(pattern, end, matcher.start()));
        String match = matcher.group();
        if ("?".equals(match)) {
          patternBuilder.append('.');
        }
        else if ("*".equals(match)) {
          patternBuilder.append(".*");
        }
        else if (match.startsWith("{") && match.endsWith("}")) {
          int colonIdx = match.indexOf(':');
          if (colonIdx == -1) {
            patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
            variableNames.add(matcher.group(1));
          }
          else {
            String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
            patternBuilder.append('(');
            patternBuilder.append(variablePattern);
            patternBuilder.append(')');
            String variableName = match.substring(1, colonIdx);
            variableNames.add(variableName);
          }
        }
        end = matcher.end();
      }
      // No glob pattern was found, this is an exact String match
      if (end == 0) {
        this.exactMatch = true;
        this.pattern = null;
      }
      else {
        this.exactMatch = false;
        patternBuilder.append(quote(pattern, end, pattern.length()));
        this.pattern = Pattern.compile(patternBuilder.toString(),
                Pattern.DOTALL | (this.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE));
      }
      this.variableNames = variableNames;
    }

    private String quote(String s, int start, int end) {
      return start == end ? Constant.BLANK : Pattern.quote(s.substring(start, end));
    }

    @Nullable
    public String[] extractVariables(String str) {
      if (pattern != null) {
        Matcher matcher = this.pattern.matcher(str);
        if (matcher.matches()) {
          final int groupCount = matcher.groupCount();
          if (variableNames.size() != groupCount) {
            throwIllegalArgumentException();
          }
          final String[] ret = new String[groupCount];
          for (int i = 0; i < groupCount; i++) {
            ret[i] = matcher.group(i + 1);
          }
          return ret;
        }
      }
      return null;
    }

    protected void throwIllegalArgumentException() {
      throw new IllegalArgumentException(
              "The number of capturing groups in the pattern segment " + this.pattern
                      + " does not match the number of URI template variables it defines, which can occur if capturing groups are used in a URI template regex. Use non-capturing groups instead.");
    }

    /**
     * Main entry point.
     *
     * @return {@code true} if the string matches against the pattern, or
     * {@code false} otherwise.
     */
    public boolean matchStrings(String str, @Nullable Map<String, String> uriTemplateVariables) {
      if (exactMatch) {
        return caseSensitive ? rawPattern.equals(str) : rawPattern.equalsIgnoreCase(str);
      }
      else if (pattern != null) {
        Matcher matcher = pattern.matcher(str);
        if (matcher.matches()) {
          if (uriTemplateVariables != null) {
            if (this.variableNames.size() != matcher.groupCount()) {
              throw new IllegalArgumentException("The number of capturing groups in the pattern segment " +
                      this.pattern + " does not match the number of URI template variables it defines, " +
                      "which can occur if capturing groups are used in a URI template regex. " +
                      "Use non-capturing groups instead.");
            }
            for (int i = 1; i <= matcher.groupCount(); i++) {
              String name = this.variableNames.get(i - 1);
              if (name.startsWith("*")) {
                throw new IllegalArgumentException("Capturing patterns (" + name + ") are not " +
                        "supported by the AntPathMatcher. Use the PathPatternParser instead.");
              }
              String value = matcher.group(i);
              uriTemplateVariables.put(name, value);
            }
          }
          return true;
        }
      }
      return false;
    }
  }

  /**
   * The default {@link Comparator} implementation returned by
   * {@link #getPatternComparator(String)}.
   * <p>
   * In order, the most "generic" pattern is determined by the following:
   * <ul>
   * <li>if it's null or a capture all pattern (i.e. it is equal to "/**")</li>
   * <li>if the other pattern is an actual match</li>
   * <li>if it's a catch-all pattern (i.e. it ends with "**"</li>
   * <li>if it's got more "*" than the other pattern</li>
   * <li>if it's got more "{foo}" than the other pattern</li>
   * <li>if it's shorter than the other pattern</li>
   * </ul>
   */
  protected static class AntPatternComparator implements Comparator<String> {

    private final String path;

    public AntPatternComparator(String path) {
      this.path = path;
    }

    /**
     * Compare two patterns to determine which should match first, i.e. which is the
     * most specific regarding the current path.
     *
     * @return a negative integer, zero, or a positive integer as pattern1 is more
     * specific, equally specific, or less specific than pattern2.
     */
    @Override
    public int compare(String pattern1, String pattern2) {
      PatternInfo info1 = new PatternInfo(pattern1);
      PatternInfo info2 = new PatternInfo(pattern2);

      if (info1.isLeastSpecific() && info2.isLeastSpecific()) {
        return 0;
      }
      else if (info1.isLeastSpecific()) {
        return 1;
      }
      else if (info2.isLeastSpecific()) {
        return -1;
      }

      boolean pattern1EqualsPath = pattern1.equals(this.path);
      boolean pattern2EqualsPath = pattern2.equals(this.path);
      if (pattern1EqualsPath && pattern2EqualsPath) {
        return 0;
      }
      else if (pattern1EqualsPath) {
        return -1;
      }
      else if (pattern2EqualsPath) {
        return 1;
      }

      if (info1.prefixPattern && info2.doubleWildcards == 0) {
        return 1;
      }
      else if (info2.prefixPattern && info1.doubleWildcards == 0) {
        return -1;
      }

      if (info1.getTotalCount() != info2.getTotalCount()) {
        return info1.getTotalCount() - info2.getTotalCount();
      }

      if (info1.getLength() != info2.getLength()) {
        return info2.getLength() - info1.getLength();
      }

      if (info1.singleWildcards < info2.singleWildcards) {
        return -1;
      }
      else if (info2.singleWildcards < info1.singleWildcards) {
        return 1;
      }

      if (info1.uriVars < info2.uriVars) {
        return -1;
      }
      else if (info2.uriVars < info1.uriVars) {
        return 1;
      }
      return 0;
    }

    /**
     * Value class that holds information about the pattern, e.g. number of
     * occurrences of "*", "**", and "{" pattern elements.
     */
    private static class PatternInfo {

      public int uriVars;

      @Nullable
      private Integer length;

      public int singleWildcards;

      public int doubleWildcards;

      public boolean prefixPattern;

      public boolean catchAllPattern;

      @Nullable
      private final String pattern;

      public PatternInfo(@Nullable String pattern) {
        this.pattern = pattern;
        if (pattern != null) {
          initCounters(pattern);
          this.catchAllPattern = pattern.equals("/**");
          this.prefixPattern = !this.catchAllPattern && pattern.endsWith("/**");
        }
        if (this.uriVars == 0) {
          this.length = (pattern != null ? pattern.length() : 0);
        }
      }

      protected void initCounters(@Nullable String pattern) {
        if (pattern != null) {
          int pos = 0;
          final int length = pattern.length();
          while (pos < length) {
            if (pattern.charAt(pos) == '{') {
              this.uriVars++;
              pos++;
            }
            else if (pattern.charAt(pos) == '*') {
              if (pos + 1 < length && pattern.charAt(pos + 1) == '*') {
                this.doubleWildcards++;
                pos += 2;
              }
              else if (pos > 0 && !pattern.substring(pos - 1).equals(".*")) {
                this.singleWildcards++;
                pos++;
              }
              else {
                pos++;
              }
            }
            else {
              pos++;
            }
          }
        }
      }

      public boolean isLeastSpecific() {
        return (this.pattern == null || this.catchAllPattern);
      }

      public int getTotalCount() {
        return this.uriVars + this.singleWildcards + (2 * this.doubleWildcards);
      }

      /**
       * Returns the length of the given pattern, where template variables are
       * considered to be 1 long.
       */
      public int getLength() {
        final Integer length = this.length;
        if (length == null) {
          return this.length = (this.pattern != null
                                ? VARIABLE_PATTERN.matcher(this.pattern).replaceAll("#").length() : 0);
        }
        return length;
      }
    }
  }

  /**
   * A simple cache for patterns that depend on the configured path separator.
   */
  private static final class PathSeparatorPatternCache {

    private final String endsOnWildCard;
    private final String endsOnDoubleWildCard;

    private PathSeparatorPatternCache(String pathSeparator) {
      this.endsOnWildCard = pathSeparator + '*';
      this.endsOnDoubleWildCard = pathSeparator.concat("**");
    }
  }

}
