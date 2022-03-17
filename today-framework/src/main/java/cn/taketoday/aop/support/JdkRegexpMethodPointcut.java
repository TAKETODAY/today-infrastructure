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

package cn.taketoday.aop.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regular expression pointcut based on the {@code java.util.regex} package.
 * Supports the following JavaBean properties:
 * <ul>
 * <li>pattern: regular expression for the fully-qualified method names to match
 * <li>patterns: alternative property taking a String array of patterns. The result will
 * be the union of these patterns.
 * </ul>
 *
 * <p>Note: the regular expressions must be a match. For example,
 * {@code .*get.*} will match com.mycom.Foo.getBar().
 * {@code get.*} will not.
 *
 * @author Dmitriy Kopylenko
 * @author Rob Harrop
 * @author TODAY 2021/2/4 12:20
 * @since 3.0
 */
@SuppressWarnings("serial")
public class JdkRegexpMethodPointcut extends AbstractRegexpMethodPointcut {

  /**
   * Compiled form of the patterns.
   */
  private Pattern[] compiledPatterns = new Pattern[0];

  /**
   * Compiled form of the exclusion patterns.
   */
  private Pattern[] compiledExclusionPatterns = new Pattern[0];

  /**
   * Initialize {@link Pattern Patterns} from the supplied {@code String[]}.
   */
  @Override
  protected void initPatternRepresentation(String[] patterns) throws PatternSyntaxException {
    this.compiledPatterns = compilePatterns(patterns);
  }

  /**
   * Initialize exclusion {@link Pattern Patterns} from the supplied {@code String[]}.
   */
  @Override
  protected void initExcludedPatternRepresentation(String[] excludedPatterns) throws PatternSyntaxException {
    this.compiledExclusionPatterns = compilePatterns(excludedPatterns);
  }

  /**
   * Returns {@code true} if the {@link Pattern} at index {@code patternIndex}
   * matches the supplied candidate {@code String}.
   */
  @Override
  protected boolean matches(String pattern, int patternIndex) {
    Matcher matcher = this.compiledPatterns[patternIndex].matcher(pattern);
    return matcher.matches();
  }

  /**
   * Returns {@code true} if the exclusion {@link Pattern} at index {@code patternIndex}
   * matches the supplied candidate {@code String}.
   */
  @Override
  protected boolean matchesExclusion(String candidate, int patternIndex) {
    Matcher matcher = this.compiledExclusionPatterns[patternIndex].matcher(candidate);
    return matcher.matches();
  }

  /**
   * Compiles the supplied {@code String[]} into an array of
   * {@link Pattern} objects and returns that array.
   */
  private Pattern[] compilePatterns(String[] source) throws PatternSyntaxException {
    Pattern[] destination = new Pattern[source.length];
    for (int i = 0; i < source.length; i++) {
      destination[i] = Pattern.compile(source[i]);
    }
    return destination;
  }

}
