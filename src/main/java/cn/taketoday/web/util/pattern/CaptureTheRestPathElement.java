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

import java.util.List;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.server.PathContainer.Element;
import cn.taketoday.http.server.PathContainer.PathSegment;
import cn.taketoday.web.util.pattern.PathPattern.MatchingContext;

/**
 * A path element representing capturing the rest of a path. In the pattern
 * '/foo/{*foobar}' the /{*foobar} is represented as a {@link CaptureTheRestPathElement}.
 *
 * @author Andy Clement
 * @since 4.0
 */
class CaptureTheRestPathElement extends PathElement implements VariableNameProvider {

  private final String variableName;

  /**
   * Create a new {@link CaptureTheRestPathElement} instance.
   *
   * @param pos position of the path element within the path pattern text
   * @param captureDescriptor a character array containing contents like '{' '*' 'a' 'b' '}'
   * @param separator the separator used in the path pattern
   */
  CaptureTheRestPathElement(int pos, char[] captureDescriptor, char separator) {
    super(pos, separator);
    this.variableName = new String(captureDescriptor, 2, captureDescriptor.length - 3);
  }

  @Override
  public boolean matches(int pathIndex, MatchingContext matchingContext) {
    // No need to handle 'match start' checking as this captures everything
    // anyway and cannot be followed by anything else
    // assert next == null

    // If there is more data, it must start with the separator
    if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
      return false;
    }
    if (matchingContext.determineRemainingPath) {
      matchingContext.remainingPathIndex = matchingContext.pathLength;
    }
    if (matchingContext.extractingVariables) {
      // Collect the parameters from all the remaining segments
      MultiValueMap<String, String> parametersCollector = null;
      for (int i = pathIndex; i < matchingContext.pathLength; i++) {
        Element element = matchingContext.pathElements.get(i);
        if (element instanceof PathSegment) {
          MultiValueMap<String, String> parameters = ((PathSegment) element).parameters();
          if (!parameters.isEmpty()) {
            if (parametersCollector == null) {
              parametersCollector = MultiValueMap.fromLinkedHashMap();
            }
            parametersCollector.addAll(parameters);
          }
        }
      }
      matchingContext.set(variableName, pathToString(pathIndex, matchingContext.pathElements),
              parametersCollector);
    }
    return true;
  }

  private String pathToString(int fromSegment, List<Element> pathElements) {
    StringBuilder sb = new StringBuilder();
    for (int i = fromSegment, max = pathElements.size(); i < max; i++) {
      Element element = pathElements.get(i);
      if (element instanceof PathSegment) {
        sb.append(((PathSegment) element).valueToMatch());
      }
      else {
        sb.append(element.value());
      }
    }
    return sb.toString();
  }

  @Override
  public String getVariableName() {
    return variableName;
  }

  @Override
  public int getNormalizedLength() {
    return 1;
  }

  @Override
  public char[] getChars() {
    return ("/{*" + this.variableName + "}").toCharArray();
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
  public String toString() {
    return "CaptureTheRest(/{*" + this.variableName + "})";
  }

}
