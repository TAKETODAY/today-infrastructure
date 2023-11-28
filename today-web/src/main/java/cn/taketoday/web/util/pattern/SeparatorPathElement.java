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

import cn.taketoday.web.util.pattern.PathPattern.MatchingContext;

/**
 * A separator path element. In the pattern '/foo/bar' the two occurrences
 * of '/' will be represented by a SeparatorPathElement (if the default
 * separator of '/' is being used).
 *
 * @author Andy Clement
 * @since 4.0
 */
class SeparatorPathElement extends PathElement {

  SeparatorPathElement(int pos, char separator) {
    super(pos, separator);
  }

  /**
   * Matching a separator is easy, basically the character at candidateIndex
   * must be the separator.
   */
  @Override
  public boolean matches(int pathIndex, MatchingContext matchingContext) {
    if (pathIndex < matchingContext.pathLength && matchingContext.isSeparator(pathIndex)) {
      if (isNoMorePattern()) {
        if (matchingContext.determineRemainingPath) {
          matchingContext.remainingPathIndex = pathIndex + 1;
          return true;
        }
        else {
          return (pathIndex + 1 == matchingContext.pathLength);
        }
      }
      else {
        pathIndex++;
        return (this.next != null && this.next.matches(pathIndex, matchingContext));
      }
    }
    return false;
  }

  @Override
  public int getNormalizedLength() {
    return 1;
  }

  @Override
  public char[] getChars() {
    return new char[] { this.separator };
  }

  @Override
  public boolean isLiteral() {
    return true;
  }

  @Override
  public String toString() {
    return "Separator(" + this.separator + ")";
  }

}
