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

package infra.web.util.pattern;

/**
 * A path element representing wildcarding multiple segments in a path.
 * This element is only allowed in two situations:
 * <ol>
 * <li>At the start of a path, immediately followed by a {@link LiteralPathElement} like '&#47;**&#47;foo&#47;{bar}'
 * <li>At the end of a path, like '&#47;foo&#47;**'
 * </ol>
 * <p>Only a single {@link WildcardSegmentsPathElement} or {@link CaptureSegmentsPathElement} element is allowed
 * in a pattern. In the pattern '&#47;foo&#47;**' the '&#47;**' is represented as a {@link WildcardSegmentsPathElement}.
 *
 * @author Andy Clement
 * @author Brian Clozel
 * @since 4.0
 */
class WildcardSegmentsPathElement extends PathElement {

  WildcardSegmentsPathElement(int pos, char separator) {
    super(pos, separator);
  }

  @Override
  public boolean matches(int pathIndex, PathPattern.MatchingContext matchingContext) {
    // wildcard segments at the start of the pattern
    if (pathIndex == 0 && this.next != null) {
      int endPathIndex = pathIndex;
      while (endPathIndex < matchingContext.pathLength) {
        if (this.next.matches(endPathIndex, matchingContext)) {
          return true;
        }
        endPathIndex++;
      }
      return false;
    }
    // match until the end of the path
    else if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
      return false;
    }
    if (matchingContext.determineRemainingPath) {
      matchingContext.remainingPathIndex = matchingContext.pathLength;
    }
    return true;
  }

  @Override
  public int getNormalizedLength() {
    return 1;
  }

  @Override
  public char[] getChars() {
    return (this.separator + "**").toCharArray();
  }

  @Override
  public int getWildcardCount() {
    return 1;
  }

  @Override
  public String toString() {
    return "WildcardSegments(" + this.separator + "**)";
  }

}
