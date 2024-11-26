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

package infra.web.util.pattern;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import infra.core.AntPathMatcher;
import infra.http.server.PathContainer;
import infra.http.server.PathContainer.Element;
import infra.http.server.PathContainer.Separator;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.util.StringUtils;

/**
 * Representation of a parsed path pattern. Includes a chain of path elements
 * for fast matching and accumulates computed state for quick comparison of
 * patterns.
 *
 * <p>{@code PathPattern} matches URL paths using the following rules:<br>
 * <ul>
 * <li>{@code ?} matches one character</li>
 * <li>{@code *} matches zero or more characters within a path segment</li>
 * <li>{@code **} matches zero or more <em>path segments</em> until the end of the path</li>
 * <li><code>{spring}</code> matches a <em>path segment</em> and captures it as a variable named "spring"</li>
 * <li><code>{spring:[a-z]+}</code> matches the regexp {@code [a-z]+} as a path variable named "spring"</li>
 * <li><code>{*spring}</code> matches zero or more <em>path segments</em> until the end of the path
 * and captures it as a variable named "spring"</li>
 * </ul>
 *
 * <p><strong>Note:</strong> In contrast to
 * {@link AntPathMatcher}, {@code **} is supported only
 * at the end of a pattern. For example {@code /pages/{**}} is valid but
 * {@code /pages/{**}/details} is not. The same applies also to the capturing
 * variant <code>{*spring}</code>. The aim is to eliminate ambiguity when
 * comparing patterns for specificity.
 *
 * <h3>Examples</h3>
 * <ul>
 * <li>{@code /pages/t?st.html} &mdash; matches {@code /pages/test.html} as well as
 * {@code /pages/tXst.html} but not {@code /pages/toast.html}</li>
 * <li>{@code /resources/*.png} &mdash; matches all {@code .png} files in the
 * {@code resources} directory</li>
 * <li><code>/resources/&#42;&#42;</code> &mdash; matches all files
 * underneath the {@code /resources/} path, including {@code /resources/image.png}
 * and {@code /resources/css/spring.css}</li>
 * <li><code>/resources/{&#42;path}</code> &mdash; matches all files
 * underneath the {@code /resources/}, as well as {@code /resources}, and captures
 * their relative path in a variable named "path"; {@code /resources/image.png}
 * will match with "path" &rarr; "/image.png", and {@code /resources/css/spring.css}
 * will match with "path" &rarr; "/css/spring.css"</li>
 * <li><code>/resources/{filename:\\w+}.dat</code> will match {@code /resources/spring.dat}
 * and assign the value {@code "spring"} to the {@code filename} variable</li>
 * </ul>
 *
 * @author Andy Clement
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PathContainer
 * @since 4.0
 */
public class PathPattern implements Comparable<PathPattern> {

  private static final PathContainer EMPTY_PATH = PathContainer.empty();

  /**
   * Comparator that sorts patterns by specificity as follows:
   * <ol>
   * <li>Null instances are last.
   * <li>Catch-all patterns are last.
   * <li>If both patterns are catch-all, consider the length (longer wins).
   * <li>Compare wildcard and captured variable count (lower wins).
   * <li>Consider length (longer wins)
   * </ol>
   */
  public static final Comparator<PathPattern> SPECIFICITY_COMPARATOR =
          Comparator.nullsLast(
                  Comparator.<PathPattern>comparingInt(p -> p.catchAll ? 1 : 0)
                          .thenComparingInt(p -> p.catchAll ? scoreByNormalizedLength(p) : 0)
                          .thenComparingInt(PathPattern::getScore)
                          .thenComparingInt(PathPattern::scoreByNormalizedLength)
          );

  /** The text of the parsed pattern. */
  private final String patternString;

  /** The parser used to construct this pattern. */
  private final PathPatternParser parser;

  /** The options to use to parse a pattern. */
  private final PathContainer.Options pathOptions;

  /** If this pattern has no trailing slash, allow candidates to include one and still match successfully. */
  private final boolean matchOptionalTrailingSeparator;

  /** Will this match candidates in a case sensitive way? (case sensitivity  at parse time). */
  private final boolean caseSensitive;

  /** First path element in the parsed chain of path elements for this pattern. */
  @Nullable
  private final PathElement head;

  /** How many variables are captured in this pattern. */
  private int capturedVariableCount;

  /**
   * The normalized length is trying to measure the 'active' part of the pattern. It is computed
   * by assuming all captured variables have a normalized length of 1. Effectively this means changing
   * your variable name lengths isn't going to change the length of the active part of the pattern.
   * Useful when comparing two patterns.
   */
  private int normalizedLength;

  /**
   * Does the pattern end with '&lt;separator&gt;'.
   */
  private boolean endsWithSeparatorWildcard = false;

  /**
   * Score is used to quickly compare patterns. Different pattern components are given different
   * weights. A 'lower score' is more specific. Current weights:
   * <ul>
   * <li>Captured variables are worth 1
   * <li>Wildcard is worth 100
   * </ul>
   */
  private int score;

  /** Does the pattern end with {*...}. */
  private boolean catchAll = false;

  @Nullable
  private ArrayList<String> variableNames;

  PathPattern(String patternText, PathPatternParser parser, @Nullable PathElement head) {
    this.head = head;
    this.parser = parser;
    this.patternString = patternText;
    this.pathOptions = parser.getPathOptions();
    this.caseSensitive = parser.isCaseSensitive();
    this.matchOptionalTrailingSeparator = parser.isMatchOptionalTrailingSeparator();

    // Compute fields for fast comparison
    PathElement elem = head;
    while (elem != null) {
      this.capturedVariableCount += elem.getCaptureCount();
      this.normalizedLength += elem.getNormalizedLength();
      this.score += elem.getScore();
      if (elem instanceof CaptureTheRestPathElement || elem instanceof WildcardTheRestPathElement) {
        this.catchAll = true;
      }
      if (elem instanceof SeparatorPathElement && elem.next instanceof WildcardPathElement && elem.next.next == null) {
        this.endsWithSeparatorWildcard = true;
      }
      elem = elem.next;
    }
  }

  /**
   * Return the original String that was parsed to create this PathPattern.
   */
  public String getPatternString() {
    return this.patternString;
  }

  /**
   * Whether the pattern string contains pattern syntax that would require
   * use of {@link #matches(PathContainer)}, or if it is a regular String that
   * could be compared directly to others.
   */
  public boolean hasPatternSyntax() {
    return this.score > 0 || this.catchAll || this.patternString.indexOf('?') != -1;
  }

  /**
   * Whether this pattern matches the given path.
   *
   * @param pathContainer the candidate path to attempt to match against
   * @return {@code true} if the path matches this pattern
   */
  public boolean matches(PathContainer pathContainer) {
    if (head == null) {
      return !hasLength(pathContainer)
              || (matchOptionalTrailingSeparator && pathContainerIsJustSeparator(pathContainer));
    }
    else if (!hasLength(pathContainer)) {
      if (head instanceof WildcardTheRestPathElement || head instanceof CaptureTheRestPathElement) {
        pathContainer = EMPTY_PATH; // Will allow CaptureTheRest to bind the variable to empty
      }
      else {
        return false;
      }
    }
    MatchingContext matchingContext = new MatchingContext(pathContainer, false);
    return head.matches(0, matchingContext);
  }

  /**
   * Match this pattern to the given URI path and return extracted URI template
   * variables as well as path parameters (matrix variables).
   *
   * @param pathContainer the candidate path to attempt to match against
   * @return info object with the extracted variables, or {@code null} for no match
   */
  @Nullable
  public PathMatchInfo matchAndExtract(PathContainer pathContainer) {
    if (this.head == null) {
      return hasLength(pathContainer)
                     && !(this.matchOptionalTrailingSeparator && pathContainerIsJustSeparator(pathContainer))
             ? null : PathMatchInfo.EMPTY;
    }
    else if (!hasLength(pathContainer)) {
      if (this.head instanceof WildcardTheRestPathElement || this.head instanceof CaptureTheRestPathElement) {
        pathContainer = EMPTY_PATH; // Will allow CaptureTheRest to bind the variable to empty
      }
      else {
        return null;
      }
    }
    MatchingContext matchingContext = new MatchingContext(pathContainer, true);
    return this.head.matches(0, matchingContext) ? matchingContext.getPathMatchResult() : null;
  }

  /**
   * Match the beginning of the given path and return the remaining portion
   * not covered by this pattern. This is useful for matching nested routes
   * where the path is matched incrementally at each level.
   *
   * @param pathContainer the candidate path to attempt to match against
   * @return info object with the match result or {@code null} for no match
   */
  @Nullable
  public PathRemainingMatchInfo matchStartOfPath(PathContainer pathContainer) {
    if (this.head == null) {
      return new PathRemainingMatchInfo(EMPTY_PATH, pathContainer);
    }
    else if (!hasLength(pathContainer)) {
      return null;
    }

    MatchingContext matchingContext = new MatchingContext(pathContainer, true);
    matchingContext.setMatchAllowExtraPath();
    boolean matches = this.head.matches(0, matchingContext);
    if (!matches) {
      return null;
    }
    else {
      PathContainer pathMatched;
      PathContainer pathRemaining;
      if (matchingContext.remainingPathIndex == pathContainer.elements().size()) {
        pathMatched = pathContainer;
        pathRemaining = EMPTY_PATH;
      }
      else {
        pathMatched = pathContainer.subPath(0, matchingContext.remainingPathIndex);
        pathRemaining = pathContainer.subPath(matchingContext.remainingPathIndex);
      }
      return new PathRemainingMatchInfo(pathMatched, pathRemaining, matchingContext.getPathMatchResult());
    }
  }

  /**
   * Determine the pattern-mapped part for the given path.
   * <p>For example: <ul>
   * <li>'{@code /docs/cvs/commit.html}' and '{@code /docs/cvs/commit.html} &rarr; ''</li>
   * <li>'{@code /docs/*}' and '{@code /docs/cvs/commit}' &rarr; '{@code cvs/commit}'</li>
   * <li>'{@code /docs/cvs/*.html}' and '{@code /docs/cvs/commit.html} &rarr; '{@code commit.html}'</li>
   * <li>'{@code /docs/**}' and '{@code /docs/cvs/commit} &rarr; '{@code cvs/commit}'</li>
   * </ul>
   * <p><b>Notes:</b>
   * <ul>
   * <li>Assumes that {@link #matches} returns {@code true} for
   * the same path but does <strong>not</strong> enforce this.
   * <li>Duplicate occurrences of separators within the returned result are removed
   * <li>Leading and trailing separators are removed from the returned result
   * </ul>
   *
   * @param path a path that matches this pattern
   * @return the subset of the path that is matched by pattern or "" if none
   * of it is matched by pattern elements
   */
  public PathContainer extractPathWithinPattern(PathContainer path) {
    List<Element> pathElements = path.elements();
    int pathElementsCount = pathElements.size();

    int startIndex = 0;
    // Find first path element that is not a separator or a literal (i.e. the first pattern based element)
    PathElement elem = this.head;
    while (elem != null) {
      if (!elem.isLiteral()) {
        break;
      }
      elem = elem.next;
      startIndex++;
    }
    if (elem == null) {
      // There is no pattern piece
      return EMPTY_PATH;
    }

    // Skip leading separators that would be in the result
    while (startIndex < pathElementsCount && (pathElements.get(startIndex) instanceof Separator)) {
      startIndex++;
    }

    int endIndex = pathElements.size();
    // Skip trailing separators that would be in the result
    while (endIndex > 0 && (pathElements.get(endIndex - 1) instanceof Separator)) {
      endIndex--;
    }

    boolean multipleAdjacentSeparators = false;
    for (int i = startIndex; i < (endIndex - 1); i++) {
      if ((pathElements.get(i) instanceof Separator) && (pathElements.get(i + 1) instanceof Separator)) {
        multipleAdjacentSeparators = true;
        break;
      }
    }

    PathContainer resultPath;
    if (multipleAdjacentSeparators) {
      // Need to rebuild the path without the duplicate adjacent separators
      StringBuilder sb = new StringBuilder();
      int i = startIndex;
      while (i < endIndex) {
        Element e = pathElements.get(i++);
        sb.append(e.value());
        if (e instanceof Separator) {
          while (i < endIndex && (pathElements.get(i) instanceof Separator)) {
            i++;
          }
        }
      }
      resultPath = PathContainer.parsePath(sb.toString(), this.pathOptions);
    }
    else if (startIndex >= endIndex) {
      resultPath = EMPTY_PATH;
    }
    else {
      resultPath = path.subPath(startIndex, endIndex);
    }
    return resultPath;
  }

  /**
   * returns variable names
   */
  public ArrayList<String> getVariableNames() {
    if (variableNames == null) {
      this.variableNames = new ArrayList<>();
      PathElement current = head;
      while (current != null) {
        if (current instanceof VariableNameProvider provider) {
          String variableName = provider.getVariableName();
          variableNames.add(variableName);
        }
        current = current.next;
      }
    }
    return variableNames;
  }

  /**
   * Compare this pattern with a supplied pattern: return -1,0,+1 if this pattern
   * is more specific, the same or less specific than the supplied pattern.
   * The aim is to sort more specific patterns first.
   */
  @Override
  public int compareTo(@Nullable PathPattern otherPattern) {
    int result = SPECIFICITY_COMPARATOR.compare(this, otherPattern);
    return (result == 0 && otherPattern != null ?
            this.patternString.compareTo(otherPattern.patternString) : result);
  }

  /**
   * Combine this pattern with another.
   */
  public PathPattern combine(PathPattern pattern2string) {
    // If one of them is empty the result is the other. If both empty the result is ""
    if (StringUtils.isEmpty(patternString)) {
      if (StringUtils.isEmpty(pattern2string.patternString)) {
        return parser.parse("");
      }
      else {
        return pattern2string;
      }
    }
    else if (StringUtils.isEmpty(pattern2string.patternString)) {
      return this;
    }

    // /* + /hotel => /hotel
    // /*.* + /*.html => /*.html
    // However:
    // /usr + /user => /usr/user
    // /{foo} + /bar => /{foo}/bar
    if (capturedVariableCount == 0
            && !patternString.equals(pattern2string.patternString)
            && matches(PathContainer.parsePath(pattern2string.patternString))) {
      return pattern2string;
    }

    // /hotels/* + /booking => /hotels/booking
    // /hotels/* + booking => /hotels/booking
    if (endsWithSeparatorWildcard) {
      return parser.parse(concat(
              patternString.substring(0, patternString.length() - 2),
              pattern2string.patternString));
    }

    // /hotels + /booking => /hotels/booking
    // /hotels + booking => /hotels/booking
    int starDotPos1 = patternString.indexOf("*.");  // Are there any file prefix/suffix things to consider?
    if (capturedVariableCount != 0 || starDotPos1 == -1 || getSeparator() == '.') {
      return parser.parse(concat(patternString, pattern2string.patternString));
    }

    // /*.html + /hotel => /hotel.html
    // /*.html + /hotel.* => /hotel.html
    String firstExtension = patternString.substring(starDotPos1 + 1);  // looking for the first extension
    String p2string = pattern2string.patternString;
    int dotPos2 = p2string.indexOf('.');
    String file2 = dotPos2 == -1 ? p2string : p2string.substring(0, dotPos2);
    String secondExtension = dotPos2 == -1 ? "" : p2string.substring(dotPos2);
    boolean firstExtensionWild = firstExtension.equals(".*") || firstExtension.isEmpty();
    boolean secondExtensionWild = secondExtension.equals(".*") || secondExtension.isEmpty();
    if (!firstExtensionWild && !secondExtensionWild) {
      throw new IllegalArgumentException(
              "Cannot combine patterns: " + patternString + " and " + pattern2string);
    }
    return parser.parse(file2 + (firstExtensionWild ? secondExtension : firstExtension));
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof PathPattern otherPattern) {
      return caseSensitive == otherPattern.caseSensitive
              && getSeparator() == otherPattern.getSeparator()
              && patternString.equals(otherPattern.getPatternString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (this.patternString.hashCode() + getSeparator()) * 17 + (this.caseSensitive ? 1 : 0);
  }

  @Override
  public String toString() {
    return this.patternString;
  }

  int getScore() {
    return this.score;
  }

  boolean isCatchAll() {
    return this.catchAll;
  }

  /**
   * The normalized length is trying to measure the 'active' part of the pattern. It is computed
   * by assuming all capture variables have a normalized length of 1. Effectively this means changing
   * your variable name lengths isn't going to change the length of the active part of the pattern.
   * Useful when comparing two patterns.
   */
  int getNormalizedLength() {
    return this.normalizedLength;
  }

  char getSeparator() {
    return this.pathOptions.separator();
  }

  int getCapturedVariableCount() {
    return this.capturedVariableCount;
  }

  String toChainString() {
    StringJoiner stringJoiner = new StringJoiner(" ");
    PathElement pe = this.head;
    while (pe != null) {
      stringJoiner.add(pe.toString());
      pe = pe.next;
    }
    return stringJoiner.toString();
  }

  /**
   * Return the string form of the pattern built from walking the path element chain.
   *
   * @return the string form of the pattern
   */
  String computePatternString() {
    StringBuilder sb = new StringBuilder();
    PathElement pe = this.head;
    while (pe != null) {
      sb.append(pe.getChars());
      pe = pe.next;
    }
    return sb.toString();
  }

  @Nullable
  PathElement getHeadSection() {
    return this.head;
  }

  /**
   * Join two paths together including a separator if necessary.
   * Extraneous separators are removed (if the first path
   * ends with one and the second path starts with one).
   *
   * @param path1 first path
   * @param path2 second path
   * @return joined path that may include separator if necessary
   */
  private String concat(String path1, String path2) {
    boolean path1EndsWithSeparator = (path1.charAt(path1.length() - 1) == getSeparator());
    boolean path2StartsWithSeparator = (path2.charAt(0) == getSeparator());
    if (path1EndsWithSeparator && path2StartsWithSeparator) {
      return path1 + path2.substring(1);
    }
    else if (path1EndsWithSeparator || path2StartsWithSeparator) {
      return path1 + path2;
    }
    else {
      return path1 + getSeparator() + path2;
    }
  }

  /**
   * Return if the container is not null and has more than zero elements.
   *
   * @param container a path container
   * @return {@code true} has more than zero elements
   */
  private boolean hasLength(@Nullable PathContainer container) {
    return container != null && !container.elements().isEmpty();
  }

  private static int scoreByNormalizedLength(PathPattern pattern) {
    return -pattern.normalizedLength;
  }

  private boolean pathContainerIsJustSeparator(PathContainer pathContainer) {
    return pathContainer.value().length() == 1
            && pathContainer.value().charAt(0) == getSeparator();
  }

  /**
   * Holder for the result of a match on the start of a pattern.
   * Provides access to the remaining path not matched to the pattern as well
   * as any variables bound in that first part that was matched.
   */
  public static class PathRemainingMatchInfo {

    /**
     * Return the part of a path that was matched by a pattern.
     *
     * @since 4.0
     */
    public final PathContainer pathMatched;

    /**
     * Return the part of a path that was not matched by a pattern.
     */
    public final PathContainer pathRemaining;

    public final PathMatchInfo pathMatchInfo;

    PathRemainingMatchInfo(PathContainer pathMatched, PathContainer pathRemaining) {
      this(pathMatched, pathRemaining, PathMatchInfo.EMPTY);
    }

    PathRemainingMatchInfo(PathContainer pathMatched, PathContainer pathRemaining,
            PathMatchInfo pathMatchInfo) {
      this.pathRemaining = pathRemaining;
      this.pathMatched = pathMatched;
      this.pathMatchInfo = pathMatchInfo;
    }

    /**
     * Return variables that were bound in the part of the path that was
     * successfully matched or an empty map.
     */
    public Map<String, String> getUriVariables() {
      return this.pathMatchInfo.getUriVariables();
    }

    /**
     * Return the path parameters for each bound variable.
     */
    public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
      return this.pathMatchInfo.getMatrixVariables();
    }
  }

  /**
   * Encapsulates context when attempting a match. Includes some fixed state like the
   * candidate currently being considered for a match but also some accumulators for
   * extracted variables.
   */
  class MatchingContext {

    public final PathContainer candidate;

    public final List<Element> pathElements;

    public final int pathLength;

    @Nullable
    private Map<String, String> extractedUriVariables;

    @Nullable
    private Map<String, MultiValueMap<String, String>> extractedMatrixVariables;

    public boolean extractingVariables;

    public boolean determineRemainingPath = false;

    // if determineRemaining is true, this is set to the position in
    // the candidate where the pattern finished matching - i.e. it
    // points to the remaining path that wasn't consumed
    public int remainingPathIndex;

    @Nullable
    public List<String> uriVariables;

    public MatchingContext(PathContainer pathContainer, boolean extractVariables) {
      this.candidate = pathContainer;
      this.pathElements = pathContainer.elements();
      this.pathLength = pathElements.size();
      this.extractingVariables = extractVariables;
    }

    public void setMatchAllowExtraPath() {
      this.determineRemainingPath = true;
    }

    public boolean isMatchOptionalTrailingSeparator() {
      return matchOptionalTrailingSeparator;
    }

    public void set(String key, String value, @Nullable MultiValueMap<String, String> parameters) {
      if (extractedUriVariables == null) {
        this.extractedUriVariables = new HashMap<>();
      }
      extractedUriVariables.put(key, value);

      if (uriVariables == null) {
        uriVariables = new ArrayList<>();
      }
      uriVariables.add(value);

      if (CollectionUtils.isNotEmpty(parameters)) {
        if (extractedMatrixVariables == null) {
          this.extractedMatrixVariables = new HashMap<>();
        }
        extractedMatrixVariables.put(key, parameters.asReadOnly());
      }
    }

    public PathMatchInfo getPathMatchResult() {
      if (this.extractedUriVariables == null) {
        return PathMatchInfo.EMPTY;
      }
      else {
        return new PathMatchInfo(this.extractedUriVariables, this.extractedMatrixVariables);
      }
    }

    /**
     * Return if element at specified index is a separator.
     *
     * @param pathIndex possible index of a separator
     * @return {@code true} if element is a separator
     */
    public boolean isSeparator(int pathIndex) {
      return this.pathElements.get(pathIndex) instanceof Separator;
    }

    /**
     * Return the decoded value of the specified element.
     *
     * @param pathIndex path element index
     * @return the decoded value
     */
    public String pathElementValue(int pathIndex) {
      Element element = pathIndex < this.pathLength ? this.pathElements.get(pathIndex) : null;
      return element instanceof PathContainer.PathSegment pathSegment ? pathSegment.valueToMatch() : "";
    }
  }

}
