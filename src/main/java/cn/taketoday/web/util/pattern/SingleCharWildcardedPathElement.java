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

import cn.taketoday.http.server.PathContainer.Element;
import cn.taketoday.http.server.PathContainer.PathSegment;
import cn.taketoday.web.util.pattern.PathPattern.MatchingContext;

/**
 * A literal path element that does includes the single character wildcard '?' one
 * or more times (to basically many any character at that position).
 *
 * @author Andy Clement
 * @since 4.0
 */
class SingleCharWildcardedPathElement extends PathElement {

  private final int len;
  private final char[] text;
  private final int questionMarkCount;
  private final boolean caseSensitive;

  public SingleCharWildcardedPathElement(
          int pos, char[] literalText, int questionMarkCount, boolean caseSensitive, char separator) {

    super(pos, separator);
    this.len = literalText.length;
    this.questionMarkCount = questionMarkCount;
    this.caseSensitive = caseSensitive;
    if (caseSensitive) {
      this.text = literalText;
    }
    else {
      this.text = new char[literalText.length];
      for (int i = 0; i < this.len; i++) {
        this.text[i] = Character.toLowerCase(literalText[i]);
      }
    }
  }

  @Override
  public boolean matches(int pathIndex, MatchingContext matchingContext) {
    if (pathIndex >= matchingContext.pathLength) {
      // no more path left to match this element
      return false;
    }

    Element element = matchingContext.pathElements.get(pathIndex);
    if (!(element instanceof PathSegment)) {
      return false;
    }
    String value = ((PathSegment) element).valueToMatch();
    if (value.length() != this.len) {
      // Not enough data to match this path element
      return false;
    }

    if (this.caseSensitive) {
      for (int i = 0; i < this.len; i++) {
        char ch = this.text[i];
        if ((ch != '?') && (ch != value.charAt((i)))) {
          return false;
        }
      }
    }
    else {
      for (int i = 0; i < this.len; i++) {
        char ch = this.text[i];
        // TODO revisit performance if doing a lot of case insensitive matching
        if ((ch != '?') && (ch != Character.toLowerCase(value.charAt(i)))) {
          return false;
        }
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
  public int getWildcardCount() {
    return this.questionMarkCount;
  }

  @Override
  public int getNormalizedLength() {
    return this.len;
  }

  @Override
  public char[] getChars() {
    return this.text;
  }

  @Override
  public String toString() {
    return "SingleCharWildcarded(" + String.valueOf(this.text) + ")";
  }

}
