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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.http.server.PathContainer.PathSegment;
import cn.taketoday.lang.Nullable;

/**
 * A path element representing capturing a piece of the path as a variable. In the pattern
 * '/foo/{bar}/goo' the {bar} is represented as a {@link CaptureVariablePathElement}. There
 * must be at least one character to bind to the variable.
 *
 * @author Andy Clement
 * @since 4.0
 */
class CaptureVariablePathElement extends PathElement implements VariableNameProvider {

  private final String variableName;

  @Nullable
  private final Pattern constraintPattern;

  /**
   * Create a new {@link CaptureVariablePathElement} instance.
   *
   * @param pos the position in the pattern of this capture element
   * @param captureDescriptor is of the form {AAAAA[:pattern]}
   */
  CaptureVariablePathElement(int pos, char[] captureDescriptor, boolean caseSensitive, char separator) {
    super(pos, separator);
    int colon = -1;
    for (int i = 0; i < captureDescriptor.length; i++) {
      if (captureDescriptor[i] == ':') {
        colon = i;
        break;
      }
    }
    if (colon == -1) {
      // no constraint
      this.variableName = new String(captureDescriptor, 1, captureDescriptor.length - 2);
      this.constraintPattern = null;
    }
    else {
      this.variableName = new String(captureDescriptor, 1, colon - 1);
      String regex = new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2);
      if (caseSensitive) {
        this.constraintPattern = Pattern.compile(regex);
      }
      else {
        this.constraintPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
      }
    }
  }

  @Override
  public boolean matches(int pathIndex, PathPattern.MatchingContext matchingContext) {
    if (pathIndex >= matchingContext.pathLength) {
      // no more path left to match this element
      return false;
    }
    String candidateCapture = matchingContext.pathElementValue(pathIndex);
    if (candidateCapture.length() == 0) {
      return false;
    }

    if (constraintPattern != null) {
      // TODO possible optimization - only regex match if rest of pattern matches?
      // Benefit likely to vary pattern to pattern
      Matcher matcher = constraintPattern.matcher(candidateCapture);
      if (matcher.groupCount() != 0) {
        throw new IllegalArgumentException(
                "No capture groups allowed in the constraint regex: " + constraintPattern.pattern());
      }
      if (!matcher.matches()) {
        return false;
      }
    }

    boolean match = false;
    pathIndex++;
    if (isNoMorePattern()) {
      if (matchingContext.determineRemainingPath) {
        matchingContext.remainingPathIndex = pathIndex;
        match = true;
      }
      else {
        // Needs to be at least one character #SPR15264
        match = (pathIndex == matchingContext.pathLength);
        if (!match && matchingContext.isMatchOptionalTrailingSeparator()) {
          match = //(nextPos > candidateIndex) &&
                  (pathIndex + 1) == matchingContext.pathLength && matchingContext.isSeparator(pathIndex);
        }
      }
    }
    else {
      if (this.next != null) {
        match = this.next.matches(pathIndex, matchingContext);
      }
    }

    if (match && matchingContext.extractingVariables) {
      matchingContext.set(variableName, candidateCapture,
              ((PathSegment) matchingContext.pathElements.get(pathIndex - 1)).parameters());
    }
    return match;
  }

  @Override
  public String getVariableName() {
    return this.variableName;
  }

  @Override
  public int getNormalizedLength() {
    return 1;
  }

  @Override
  public char[] getChars() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append(this.variableName);
    if (this.constraintPattern != null) {
      sb.append(':').append(this.constraintPattern.pattern());
    }
    sb.append('}');
    return sb.toString().toCharArray();
  }

  @Override
  public int getWildcardCount() {
    return 0;
  }

  @Override
  public int getCaptureCount() {
    return 1;
  }

  @Override
  public int getScore() {
    return CAPTURE_VARIABLE_WEIGHT;
  }

  @Override
  public String toString() {
    return "CaptureVariable({" + this.variableName +
            (this.constraintPattern != null ? ":" + this.constraintPattern.pattern() : "") + "})";
  }

}
