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

import java.util.List;

import infra.http.server.PathContainer.Element;
import infra.http.server.PathContainer.PathSegment;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.util.pattern.PathPattern.MatchingContext;

/**
 * A path element that captures multiple path segments.
 * This element is only allowed in two situations:
 * <ol>
 * <li>At the start of a path, immediately followed by a {@link LiteralPathElement} like '/{*foobar}/foo/{bar}'
 * <li>At the end of a path, like '/foo/{*foobar}'
 * </ol>
 * <p>Only a single {@link WildcardSegmentsPathElement} or {@link CaptureSegmentsPathElement} element is allowed
 *  * in a pattern. In the pattern '/foo/{*foobar}' the /{*foobar} is represented as a {@link CaptureSegmentsPathElement}.
 *
 * @author Andy Clement
 * @author Brian Clozel
 * @since 4.0
 */
class CaptureSegmentsPathElement extends PathElement implements VariableNameProvider {

  private final String variableName;

  /**
   * Create a new {@link CaptureSegmentsPathElement} instance.
   *
   * @param pos position of the path element within the path pattern text
   * @param captureDescriptor a character array containing contents like '{' '*' 'a' 'b' '}'
   * @param separator the separator used in the path pattern
   */
  CaptureSegmentsPathElement(int pos, char[] captureDescriptor, char separator) {
    super(pos, separator);
    this.variableName = new String(captureDescriptor, 2, captureDescriptor.length - 3);
  }

  @Override
  public boolean matches(int pathIndex, MatchingContext matchingContext) {
    // wildcard segments at the start of the pattern
    if (pathIndex == 0 && this.next != null) {
      int endPathIndex = pathIndex;
      while (endPathIndex < matchingContext.pathLength) {
        if (this.next.matches(endPathIndex, matchingContext)) {
          collectParameters(matchingContext, pathIndex, endPathIndex);
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
    collectParameters(matchingContext, pathIndex, matchingContext.pathLength);
    return true;
  }

  private void collectParameters(MatchingContext matchingContext, int pathIndex, int endPathIndex) {
    if (matchingContext.extractingVariables) {
      // Collect the parameters from all the remaining segments
      MultiValueMap<String, String> parametersCollector = null;
      for (int i = pathIndex; i < endPathIndex; i++) {
        Element element = matchingContext.pathElements.get(i);
        if (element instanceof PathSegment pathSegment) {
          MultiValueMap<String, String> parameters = pathSegment.parameters();
          if (!parameters.isEmpty()) {
            if (parametersCollector == null) {
              parametersCollector = new LinkedMultiValueMap<>();
            }
            parametersCollector.addAll(parameters);
          }
        }
      }
      matchingContext.set(this.variableName,
              pathToString(pathIndex, endPathIndex, matchingContext.pathElements), parametersCollector);
    }
  }

  private String pathToString(int fromSegment, int toSegment, List<Element> pathElements) {
    StringBuilder sb = new StringBuilder();
    for (int i = fromSegment; i < toSegment; i++) {
      Element element = pathElements.get(i);
      if (element instanceof PathSegment pathSegment) {
        sb.append(pathSegment.valueToMatch());
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
    return "CaptureSegments(/{*" + this.variableName + "})";
  }

}
