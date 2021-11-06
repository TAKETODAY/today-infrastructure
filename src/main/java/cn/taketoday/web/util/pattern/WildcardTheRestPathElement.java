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

/**
 * A path element representing wildcarding the rest of a path. In the pattern
 * '/foo/**' the /** is represented as a {@link WildcardTheRestPathElement}.
 *
 * @author Andy Clement
 * @since 4.0
 */
class WildcardTheRestPathElement extends PathElement {

  WildcardTheRestPathElement(int pos, char separator) {
    super(pos, separator);
  }

  @Override
  public boolean matches(int pathIndex, PathPattern.MatchingContext matchingContext) {
    // If there is more data, it must start with the separator
    if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
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
    return "WildcardTheRest(" + this.separator + "**)";
  }

}
