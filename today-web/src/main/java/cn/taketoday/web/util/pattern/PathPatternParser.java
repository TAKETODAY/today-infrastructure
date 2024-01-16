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

package cn.taketoday.web.util.pattern;

import cn.taketoday.http.server.PathContainer;

/**
 * Parser for URI path patterns producing {@link PathPattern} instances that can
 * then be matched to request.
 *
 * <p>The {@link PathPatternParser} and {@link PathPattern} are specifically
 * designed for use with HTTP URL paths in web applications where a large number
 * of URI path patterns, continuously matched against incoming requests,
 * motivates the need for efficient matching.
 *
 * <p>For details of the path pattern syntax see {@link PathPattern}.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PathPatternParser {

  private boolean caseSensitive = true;

  private boolean matchOptionalTrailingSeparator = true;

  private PathContainer.Options pathOptions = PathContainer.Options.HTTP_PATH;

  /**
   * Whether a {@link PathPattern} produced by this parser should
   * automatically match request paths with a trailing slash.
   * <p>If set to {@code true} a {@code PathPattern} without a trailing slash
   * will also match request paths with a trailing slash. If set to
   * {@code false} a {@code PathPattern} will only match request paths with
   * a trailing slash.
   * <p>The default is {@code true}.
   */
  public void setMatchOptionalTrailingSeparator(boolean matchOptionalTrailingSeparator) {
    this.matchOptionalTrailingSeparator = matchOptionalTrailingSeparator;
  }

  /**
   * Whether optional trailing slashing match is enabled.
   */
  public boolean isMatchOptionalTrailingSeparator() {
    return this.matchOptionalTrailingSeparator;
  }

  /**
   * Whether path pattern matching should be case-sensitive.
   * <p>The default is {@code true}.
   */
  public void setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  /**
   * Whether case-sensitive pattern matching is enabled.
   */
  public boolean isCaseSensitive() {
    return this.caseSensitive;
  }

  /**
   * Set options for parsing patterns. These should be the same as the
   * options used to parse input paths.
   * <p>{@link cn.taketoday.http.server.PathContainer.Options#HTTP_PATH}
   * is used by default.
   */
  public void setPathOptions(PathContainer.Options pathOptions) {
    this.pathOptions = pathOptions;
  }

  /**
   * Return the {@link #setPathOptions configured} pattern parsing options.
   */
  public PathContainer.Options getPathOptions() {
    return this.pathOptions;
  }

  /**
   * Process the path pattern content, a character at a time, breaking it into
   * path elements around separator boundaries and verifying the structure at each
   * stage. Produces a PathPattern object that can be used for fast matching
   * against paths. Each invocation of this method delegates to a new instance of
   * the {@link InternalPathPatternParser} because that class is not thread-safe.
   *
   * @param pathPattern the input path pattern, e.g. /project/{name}
   * @return a PathPattern for quickly matching paths against request paths
   * @throws PatternParseException in case of parse errors
   */
  public PathPattern parse(String pathPattern) throws PatternParseException {
    return new InternalPathPatternParser(this).parse(pathPattern);
  }

  /**
   * Shared, read-only instance of {@code PathPatternParser}. Uses default settings:
   * <ul>
   * <li>{@code matchOptionalTrailingSeparator=true}
   * <li>{@code caseSensitive=true}
   * <li>{@code pathOptions=PathContainer.Options.HTTP_PATH}
   * </ul>
   */
  public final static PathPatternParser defaultInstance = new PathPatternParser() {

    @Override
    public void setMatchOptionalTrailingSeparator(boolean matchOptionalTrailingSeparator) {
      raiseError();
    }

    @Override
    public void setCaseSensitive(boolean caseSensitive) {
      raiseError();
    }

    @Override
    public void setPathOptions(PathContainer.Options pathOptions) {
      raiseError();
    }

    private void raiseError() {
      throw new UnsupportedOperationException(
              "This is a read-only, shared instance that cannot be modified");
    }
  };
}
