/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.http.server.PathContainer.Element;
import cn.taketoday.http.server.PathContainer.PathSegment;
import cn.taketoday.web.util.pattern.PathPattern.MatchingContext;

/**
 * A literal path element. In the pattern '/foo/bar/goo' there are three
 * literal path elements 'foo', 'bar' and 'goo'.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class LiteralPathElement extends PathElement {

  private final int len;
  private final String text;
  private final boolean caseSensitive;

  public LiteralPathElement(int pos, char[] literalText, boolean caseSensitive, char separator) {
    super(pos, separator);
    this.len = literalText.length;
    this.caseSensitive = caseSensitive;
    this.text = new String(literalText);
  }

  @Override
  public boolean matches(int pathIndex, MatchingContext matchingContext) {
    if (pathIndex >= matchingContext.pathLength) {
      // no more path left to match this element
      return false;
    }
    Element element = matchingContext.pathElements.get(pathIndex);
    if (!(element instanceof PathSegment pathSegment)) {
      return false;
    }
    String value = pathSegment.valueToMatch();
    if (value.length() != this.len) {
      // Not enough data to match this path element
      return false;
    }

    if (this.caseSensitive) {
      if (!this.text.equals(value)) {
        return false;
      }
    }
    else {
      if (!this.text.equalsIgnoreCase(value)) {
        return false;
      }
    }

    pathIndex++;
    if (isNoMorePattern()) {
      if (matchingContext.determineRemainingPath) {
        matchingContext.remainingPathIndex = pathIndex;
        return true;
      }
      else {
        if (pathIndex == matchingContext.pathLength) {
          return true;
        }
        else {
          return (matchingContext.isMatchOptionalTrailingSeparator() &&
                  (pathIndex + 1) == matchingContext.pathLength &&
                  matchingContext.isSeparator(pathIndex));
        }
      }
    }
    else {
      return (this.next != null && this.next.matches(pathIndex, matchingContext));
    }
  }

  @Override
  public int getNormalizedLength() {
    return this.len;
  }

  @Override
  public char[] getChars() {
    return this.text.toCharArray();
  }

  @Override
  public boolean isLiteral() {
    return true;
  }

  @Override
  public String toString() {
    return "Literal(" + this.text + ")";
  }

}
