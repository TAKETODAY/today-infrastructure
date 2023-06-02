/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.util.pattern;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.util.pattern.PathPattern.MatchingContext;

/**
 * Common supertype for the Ast nodes created to represent a path pattern.
 *
 * @author Andy Clement
 * @since 4.0
 */
abstract class PathElement {

  // Score related
  protected static final int WILDCARD_WEIGHT = 100;
  protected static final int CAPTURE_VARIABLE_WEIGHT = 1;

  // Position in the pattern where this path element starts
  protected final int pos;
  // The separator used in this path pattern
  protected final char separator;
  // The next path element in the chain
  @Nullable
  protected PathElement next;
  // The previous path element in the chain
  @Nullable
  protected PathElement prev;

  /**
   * Create a new path element.
   *
   * @param pos the position where this path element starts in the pattern data
   * @param separator the separator in use in the path pattern
   */
  PathElement(int pos, char separator) {
    this.pos = pos;
    this.separator = separator;
  }

  /**
   * Attempt to match this path element.
   *
   * @param candidatePos the current position within the candidate path
   * @param matchingContext encapsulates context for the match including the candidate
   * @return {@code true} if it matches, otherwise {@code false}
   */
  public abstract boolean matches(int candidatePos, MatchingContext matchingContext);

  /**
   * Return the length of the path element where captures are considered to be one character long.
   *
   * @return the normalized length
   */
  public abstract int getNormalizedLength();

  public abstract char[] getChars();

  /**
   * Return the number of variables captured by the path element.
   */
  public int getCaptureCount() {
    return 0;
  }

  /**
   * Return the number of wildcard elements (*, ?) in the path element.
   */
  public int getWildcardCount() {
    return 0;
  }

  /**
   * Return the score for this PathElement, combined score is used to compare parsed patterns.
   */
  public int getScore() {
    return 0;
  }

  /**
   * Return whether this PathElement can be strictly {@link String#compareTo(String) compared}
   * against another element for matching.
   */
  public boolean isLiteral() {
    return false;
  }

  /**
   * Return if the there are no more PathElements in the pattern.
   *
   * @return {@code true} if the there are no more elements
   */
  protected final boolean isNoMorePattern() {
    return this.next == null;
  }

}
