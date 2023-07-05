/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.aspectj;

import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.TypePatternMatcher;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Framework AOP {@link ClassFilter} implementation using AspectJ type matching.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class TypePatternClassFilter implements ClassFilter {

  private String typePattern = "";

  @Nullable
  private TypePatternMatcher aspectJTypePatternMatcher;

  /**
   * Creates a new instance of the {@link TypePatternClassFilter} class.
   * <p>This is the JavaBean constructor; be sure to set the
   * {@link #setTypePattern(String) typePattern} property, else a
   * no doubt fatal {@link IllegalStateException} will be thrown
   * when the {@link #matches(Class)} method is first invoked.
   */
  public TypePatternClassFilter() {
  }

  /**
   * Create a fully configured {@link TypePatternClassFilter} using the
   * given type pattern.
   *
   * @param typePattern the type pattern that AspectJ weaver should parse
   */
  public TypePatternClassFilter(String typePattern) {
    setTypePattern(typePattern);
  }

  /**
   * Set the AspectJ type pattern to match.
   * <p>Examples include:
   * <code class="code">
   * cn.taketoday.beans.*
   * </code>
   * This will match any class or interface in the given package.
   * <code class="code">
   * cn.taketoday.beans.ITestBean+
   * </code>
   * This will match the {@code ITestBean} interface and any class
   * that implements it.
   * <p>These conventions are established by AspectJ, not Framework AOP.
   *
   * @param typePattern the type pattern that AspectJ weaver should parse
   */
  public void setTypePattern(String typePattern) {
    Assert.notNull(typePattern, "Type pattern must not be null");
    this.typePattern = typePattern;
    this.aspectJTypePatternMatcher =
            PointcutParser.getPointcutParserSupportingAllPrimitivesAndUsingContextClassloaderForResolution().
                    parseTypePattern(replaceBooleanOperators(typePattern));
  }

  /**
   * Return the AspectJ type pattern to match.
   */
  public String getTypePattern() {
    return this.typePattern;
  }

  /**
   * Should the pointcut apply to the given interface or target class?
   *
   * @param clazz candidate target class
   * @return whether the advice should apply to this candidate target class
   * @throws IllegalStateException if no {@link #setTypePattern(String)} has been set
   */
  @Override
  public boolean matches(Class<?> clazz) {
    Assert.state(this.aspectJTypePatternMatcher != null, "No type pattern has been set");
    return this.aspectJTypePatternMatcher.matches(clazz);
  }

  /**
   * If a type pattern has been specified in XML, the user cannot
   * write {@code and} as "&amp;&amp;" (though &amp;&amp; will work).
   * We also allow {@code and} between two sub-expressions.
   * <p>This method converts back to {@code &&} for the AspectJ pointcut parser.
   */
  private String replaceBooleanOperators(String pcExpr) {
    String result = StringUtils.replace(pcExpr, " and ", " && ");
    result = StringUtils.replace(result, " or ", " || ");
    return StringUtils.replace(result, " not ", " ! ");
  }

  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof TypePatternClassFilter &&
            ObjectUtils.nullSafeEquals(this.typePattern, ((TypePatternClassFilter) other).typePattern)));
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(this.typePattern);
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + this.typePattern;
  }

}
