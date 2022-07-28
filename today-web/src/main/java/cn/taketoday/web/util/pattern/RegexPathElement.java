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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.http.server.PathContainer.PathSegment;
import cn.taketoday.web.util.pattern.PathPattern.MatchingContext;

/**
 * A regex path element. Used to represent any complicated element of the path.
 * For example in '<tt>/foo/&ast;_&ast;/&ast;_{foobar}</tt>' both <tt>*_*</tt> and <tt>*_{foobar}</tt>
 * are {@link RegexPathElement} path elements. Derived from the general
 * {@link cn.taketoday.core.AntPathMatcher} approach.
 *
 * @author Andy Clement
 * @since 4.0
 */
class RegexPathElement extends PathElement {

  private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

  private static final String DEFAULT_VARIABLE_PATTERN = "(.*)";

  private final char[] regex;

  private final boolean caseSensitive;

  private final Pattern pattern;

  private int wildcardCount;

  private final ArrayList<String> variableNames = new ArrayList<>();

  RegexPathElement(int pos, char[] regex, boolean caseSensitive, char[] completePattern, char separator) {
    super(pos, separator);
    this.regex = regex;
    this.caseSensitive = caseSensitive;
    this.pattern = buildPattern(regex, completePattern);
  }

  public Pattern buildPattern(char[] regex, char[] completePattern) {
    StringBuilder patternBuilder = new StringBuilder();
    String text = new String(regex);
    Matcher matcher = GLOB_PATTERN.matcher(text);
    int end = 0;

    while (matcher.find()) {
      patternBuilder.append(quote(text, end, matcher.start()));
      String match = matcher.group();
      if ("?".equals(match)) {
        patternBuilder.append('.');
      }
      else if ("*".equals(match)) {
        patternBuilder.append(".*");
        int pos = matcher.start();
        if (pos < 1 || regex[pos - 1] != '.') {
          // To be compatible with the AntPathMatcher comparator,
          // '.*' is not considered a wildcard usage
          this.wildcardCount++;
        }
      }
      else if (match.startsWith("{") && match.endsWith("}")) {
        int colonIdx = match.indexOf(':');
        if (colonIdx == -1) {
          patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
          String variableName = matcher.group(1);
          if (this.variableNames.contains(variableName)) {
            throw new PatternParseException(this.pos, completePattern,
                    PatternParseException.PatternMessage.ILLEGAL_DOUBLE_CAPTURE, variableName);
          }
          this.variableNames.add(variableName);
        }
        else {
          String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
          patternBuilder.append('(');
          patternBuilder.append(variablePattern);
          patternBuilder.append(')');
          String variableName = match.substring(1, colonIdx);
          if (this.variableNames.contains(variableName)) {
            throw new PatternParseException(this.pos, completePattern,
                    PatternParseException.PatternMessage.ILLEGAL_DOUBLE_CAPTURE, variableName);
          }
          this.variableNames.add(variableName);
        }
      }
      end = matcher.end();
    }

    patternBuilder.append(quote(text, end, text.length()));
    return Pattern.compile(patternBuilder.toString(),
            Pattern.DOTALL | (this.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE));
  }

  public List<String> getVariableNames() {
    return this.variableNames;
  }

  private String quote(String s, int start, int end) {
    if (start == end) {
      return "";
    }
    return Pattern.quote(s.substring(start, end));
  }

  @Override
  public boolean matches(int pathIndex, MatchingContext matchingContext) {
    String textToMatch = matchingContext.pathElementValue(pathIndex);
    Matcher matcher = this.pattern.matcher(textToMatch);
    boolean matches = matcher.matches();

    if (matches) {
      if (isNoMorePattern()) {
        if (matchingContext.determineRemainingPath &&
                (this.variableNames.isEmpty() || textToMatch.length() > 0)) {
          matchingContext.remainingPathIndex = pathIndex + 1;
          matches = true;
        }
        else {
          // No more pattern, is there more data?
          // If pattern is capturing variables there must be some actual data to bind to them
          matches = (pathIndex + 1 >= matchingContext.pathLength) &&
                  (this.variableNames.isEmpty() || textToMatch.length() > 0);
          if (!matches && matchingContext.isMatchOptionalTrailingSeparator()) {
            matches = (this.variableNames.isEmpty() || textToMatch.length() > 0) &&
                    (pathIndex + 2 >= matchingContext.pathLength) &&
                    matchingContext.isSeparator(pathIndex + 1);
          }
        }
      }
      else {
        matches = (this.next != null && this.next.matches(pathIndex + 1, matchingContext));
      }
    }

    if (matches && matchingContext.extractingVariables) {
      // Process captures
      if (this.variableNames.size() != matcher.groupCount()) {  // SPR-8455
        throw new IllegalArgumentException(
                "The number of capturing groups in the pattern segment " +
                        this.pattern + " does not match the number of URI template variables it defines, " +
                        "which can occur if capturing groups are used in a URI template regex. " +
                        "Use non-capturing groups instead.");
      }
      for (int i = 1; i <= matcher.groupCount(); i++) {
        String name = this.variableNames.get(i - 1);
        String value = matcher.group(i);
        matchingContext.set(name, value,
                (i == this.variableNames.size())
                ? ((PathSegment) matchingContext.pathElements.get(pathIndex)).parameters()
                : null
        );
      }
    }
    return matches;
  }

  @Override
  public int getNormalizedLength() {
    int varsLength = 0;
    for (String variableName : this.variableNames) {
      varsLength += variableName.length();
    }
    return (this.regex.length - varsLength - this.variableNames.size());
  }

  @Override
  public char[] getChars() {
    return this.regex;
  }

  @Override
  public int getCaptureCount() {
    return this.variableNames.size();
  }

  @Override
  public int getWildcardCount() {
    return this.wildcardCount;
  }

  @Override
  public int getScore() {
    return (getCaptureCount() * CAPTURE_VARIABLE_WEIGHT + getWildcardCount() * WILDCARD_WEIGHT);
  }

  @Override
  public String toString() {
    return "Regex(" + String.valueOf(this.regex) + ")";
  }

}
