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

package cn.taketoday.web.util.pattern;

import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.PathContainer.Element;
import cn.taketoday.web.util.pattern.PathPattern.MatchingContext;

/**
 * A wildcard path element. In the pattern '/foo/&ast;/goo' the * is
 * represented by a WildcardPathElement. Within a path it matches at least
 * one character but at the end of a path it can match zero characters.
 *
 * @author Andy Clement
 * @since 4.0
 */
class WildcardPathElement extends PathElement {

  public WildcardPathElement(int pos, char separator) {
    super(pos, separator);
  }

  /**
   * Matching on a WildcardPathElement is quite straight forward. Scan the
   * candidate from the candidateIndex onwards for the next separator or the end of the
   * candidate.
   */
  @Override
  public boolean matches(int pathIndex, MatchingContext matchingContext) {
    String segmentData = null;
    // Assert if it exists it is a segment
    if (pathIndex < matchingContext.pathLength) {
      Element element = matchingContext.pathElements.get(pathIndex);
      if (!(element instanceof PathContainer.PathSegment)) {
        // Should not match a separator
        return false;
      }
      segmentData = ((PathContainer.PathSegment) element).valueToMatch();
      pathIndex++;
    }

    if (isNoMorePattern()) {
      if (matchingContext.determineRemainingPath) {
        matchingContext.remainingPathIndex = pathIndex;
        return true;
      }
      else {
        if (pathIndex == matchingContext.pathLength) {
          // and the path data has run out too
          return true;
        }
        else {
          return (matchingContext.isMatchOptionalTrailingSeparator()  // if optional slash is on...
                  && segmentData != null && segmentData.length() > 0  // and there is at least one character to match the *...
                  && (pathIndex + 1) == matchingContext.pathLength  // and the next path element is the end of the candidate...
                  && matchingContext.isSeparator(pathIndex));  // and the final element is a separator
        }
      }
    }
    else {
      // Within a path (e.g. /aa/*/bb) there must be at least one character to match the wildcard
      if (segmentData == null || segmentData.length() == 0) {
        return false;
      }
      return (this.next != null && this.next.matches(pathIndex, matchingContext));
    }
  }

  @Override
  public int getNormalizedLength() {
    return 1;
  }

  @Override
  public char[] getChars() {
    return new char[] { '*' };
  }

  @Override
  public int getWildcardCount() {
    return 1;
  }

  @Override
  public int getScore() {
    return WILDCARD_WEIGHT;
  }

  @Override
  public String toString() {
    return "Wildcard(*)";
  }

}
