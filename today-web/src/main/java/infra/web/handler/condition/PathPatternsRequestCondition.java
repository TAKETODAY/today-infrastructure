/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.condition;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import infra.http.server.PathContainer;
import infra.util.ArrayIterator;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

/**
 * A logical disjunction (' || ') request condition that matches a request
 * against a set of URL path patterns.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class PathPatternsRequestCondition extends AbstractRequestCondition<PathPatternsRequestCondition> {

  private static final Set<String> EMPTY_PATH = Collections.singleton("");

  private static final PathPattern[] EMPTY_PATH_PATTERN = {
          PathPatternParser.defaultInstance.parse("")
  };

  private static final PathPattern[] ROOT_PATH_PATTERNS = {
          EMPTY_PATH_PATTERN[0],
          PathPatternParser.defaultInstance.parse("/")
  };

  private final PathPattern[] patterns;

  /**
   * Default constructor resulting in an {@code ""} (empty path) mapping.
   */
  public PathPatternsRequestCondition() {
    this(EMPTY_PATH_PATTERN);
  }

  /**
   * Constructor with patterns to use.
   */
  public PathPatternsRequestCondition(PathPatternParser parser, String... patterns) {
    this(parse(parser, patterns));
  }

  private static PathPattern[] parse(PathPatternParser parser, String... patterns) {
    if (patterns.length == 0 || (patterns.length == 1 && StringUtils.isBlank(patterns[0]))) {
      return EMPTY_PATH_PATTERN;
    }
    TreeSet<PathPattern> result = new TreeSet<>();
    for (String path : patterns) {
      String pathPattern = StringUtils.prependLeadingSlash(path);
      result.add(parser.parse(pathPattern));
    }
    return result.toArray(new PathPattern[0]);
  }

  private PathPatternsRequestCondition(PathPattern[] patterns) {
    this.patterns = patterns;
  }

  /**
   * Return the patterns in this condition. If only the first (top) pattern
   * is needed use {@link #getFirstPattern()}.
   */
  public PathPattern[] getPatterns() {
    return this.patterns;
  }

  @Override
  protected Collection<PathPattern> getContent() {
    return Arrays.asList(patterns);
  }

  @Override
  protected String getToStringInfix() {
    return " || ";
  }

  /**
   * Return the first pattern.
   */
  public PathPattern getFirstPattern() {
    return this.patterns[0];
  }

  /**
   * Whether the condition is the "" (empty path) mapping.
   */
  public boolean isEmptyPathMapping() {
    return this.patterns == EMPTY_PATH_PATTERN;
  }

  /**
   * Return the mapping paths that are not patterns.
   */
  public Set<String> getDirectPaths() {
    if (isEmptyPathMapping()) {
      return EMPTY_PATH;
    }
    Set<String> result = Collections.emptySet();
    for (PathPattern pattern : this.patterns) {
      if (!pattern.hasPatternSyntax()) {
        result = (result.isEmpty() ? new HashSet<>(1) : result);
        result.add(pattern.getPatternString());
      }
    }
    return result;
  }

  /**
   * Return the {@link #getPatterns()} mapped to Strings.
   */
  public Set<String> getPatternValues() {
    return isEmptyPathMapping() ? EMPTY_PATH :
            Arrays.stream(patterns)
                    .map(PathPattern::getPatternString)
                    .collect(Collectors.toSet());
  }

  /**
   * Combine the patterns of the current and of the other instances as follows:
   * <ul>
   * <li>If only one instance has patterns, use those.
   * <li>If both have patterns, combine patterns from "this" instance with
   * patterns from the other instance via {@link PathPattern#combine(PathPattern)}.
   * <li>If neither has patterns, use {@code ""} and {@code "/"} as root path patterns.
   * </ul>
   */
  @Override
  public PathPatternsRequestCondition combine(PathPatternsRequestCondition other) {
    if (isEmptyPathMapping() && other.isEmptyPathMapping()) {
      return new PathPatternsRequestCondition(ROOT_PATH_PATTERNS);
    }
    else if (other.isEmptyPathMapping()) {
      return this;
    }
    else if (isEmptyPathMapping()) {
      return other;
    }
    else {
      TreeSet<PathPattern> combined = new TreeSet<>();
      for (PathPattern pattern1 : this.patterns) {
        for (PathPattern pattern2 : other.patterns) {
          combined.add(pattern1.combine(pattern2));
        }
      }
      return new PathPatternsRequestCondition(combined.toArray(new PathPattern[0]));
    }
  }

  /**
   * Checks if any of the patterns match the given request and returns an
   * instance that is guaranteed to contain matching patterns, sorted.
   *
   * @param request the current request
   * @return the same instance if the condition contains no patterns;
   * or a new condition with sorted matching patterns;
   * or {@code null} if no patterns match.
   */
  @Override
  @Nullable
  public PathPatternsRequestCondition getMatchingCondition(RequestContext request) {
    PathPattern[] matches = getMatchingPatterns(request.getRequestPath());
    return matches != null ? new PathPatternsRequestCondition(matches) : null;
  }

  @Nullable
  private PathPattern[] getMatchingPatterns(PathContainer lookupPath) {
    PathPattern[] pathPatterns = new PathPattern[patterns.length];
    int i = 0;
    for (PathPattern pattern : patterns) {
      if (pattern.matches(lookupPath)) {
        pathPatterns[i++] = pattern;
      }
    }
    if (i == 0) {
      return null;
    }

    if (i == 1 && patterns.length == 1) {
      return pathPatterns;
    }

    PathPattern[] patterns = new PathPattern[i];
    if (i == 1) {
      patterns[0] = pathPatterns[0];
    }
    else {
      System.arraycopy(pathPatterns, 0, patterns, 0, i);
    }
    return patterns;
  }

  /**
   * Compare the two conditions based on the URL patterns they contain.
   * Patterns are compared one at a time, from top to bottom. If all compared
   * patterns match equally, but one instance has more patterns, it is
   * considered a closer match.
   * <p>It is assumed that both instances have been obtained via
   * {@link #getMatchingCondition(RequestContext)} to ensure they
   * contain only patterns that match the request and are sorted with
   * the best matches on top.
   */
  @Override
  public int compareTo(PathPatternsRequestCondition other, RequestContext request) {
    var iterator = new ArrayIterator<>(patterns);
    var iteratorOther = new ArrayIterator<>(other.patterns);
    while (iterator.hasNext() && iteratorOther.hasNext()) {
      int result = PathPattern.SPECIFICITY_COMPARATOR.compare(iterator.next(), iteratorOther.next());
      if (result != 0) {
        return result;
      }
    }
    if (iterator.hasNext()) {
      return -1;
    }
    else if (iteratorOther.hasNext()) {
      return 1;
    }
    else {
      return 0;
    }
  }

}
