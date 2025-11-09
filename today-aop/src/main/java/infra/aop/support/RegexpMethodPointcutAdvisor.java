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

package infra.aop.support;

import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

import infra.aop.Pointcut;
import infra.util.ObjectUtils;

/**
 * Convenient class for regexp method pointcuts that hold an Advice,
 * making them an {@link infra.aop.Advisor}.
 *
 * <p>Configure this class using the "pattern" and "patterns"
 * pass-through properties. These are analogous to the pattern
 * and patterns properties of {@link AbstractRegexpMethodPointcut}.
 *
 * <p>Can delegate to any {@link AbstractRegexpMethodPointcut} subclass.
 * By default, {@link JdkRegexpMethodPointcut} will be used. To choose
 * a specific one, override the {@link #createPointcut} method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setPattern
 * @see #setPatterns
 * @see JdkRegexpMethodPointcut
 * @since 4.0 2022/3/12 23:27
 */
@SuppressWarnings("serial")
public class RegexpMethodPointcutAdvisor extends AbstractGenericPointcutAdvisor {

  private String @Nullable []patterns;

  @Nullable
  private AbstractRegexpMethodPointcut pointcut;

  private final Object pointcutMonitor = new SerializableMonitor();

  /**
   * Create an empty RegexpMethodPointcutAdvisor.
   *
   * @see #setPattern
   * @see #setPatterns
   * @see #setAdvice
   */
  public RegexpMethodPointcutAdvisor() {
  }

  /**
   * Create a RegexpMethodPointcutAdvisor for the given advice.
   * The pattern still needs to be specified afterwards.
   *
   * @param advice the advice to use
   * @see #setPattern
   * @see #setPatterns
   */
  public RegexpMethodPointcutAdvisor(Advice advice) {
    setAdvice(advice);
  }

  /**
   * Create a RegexpMethodPointcutAdvisor for the given advice.
   *
   * @param pattern the pattern to use
   * @param advice the advice to use
   */
  public RegexpMethodPointcutAdvisor(String pattern, Advice advice) {
    setPattern(pattern);
    setAdvice(advice);
  }

  /**
   * Create a RegexpMethodPointcutAdvisor for the given advice.
   *
   * @param patterns the patterns to use
   * @param advice the advice to use
   */
  public RegexpMethodPointcutAdvisor(String[] patterns, Advice advice) {
    setPatterns(patterns);
    setAdvice(advice);
  }

  /**
   * Set the regular expression defining methods to match.
   * <p>Use either this method or {@link #setPatterns}, not both.
   *
   * @see #setPatterns
   */
  public void setPattern(String pattern) {
    setPatterns(pattern);
  }

  /**
   * Set the regular expressions defining methods to match.
   * To be passed through to the pointcut implementation.
   * <p>Matching will be the union of all these; if any of the
   * patterns matches, the pointcut matches.
   *
   * @see AbstractRegexpMethodPointcut#setPatterns
   */
  public void setPatterns(String... patterns) {
    this.patterns = patterns;
  }

  /**
   * Initialize the singleton Pointcut held within this Advisor.
   */
  @Override
  public Pointcut getPointcut() {
    synchronized(this.pointcutMonitor) {
      if (this.pointcut == null) {
        this.pointcut = createPointcut();
        if (this.patterns != null) {
          this.pointcut.setPatterns(this.patterns);
        }
      }
      return this.pointcut;
    }
  }

  /**
   * Create the actual pointcut: By default, a {@link JdkRegexpMethodPointcut}
   * will be used.
   *
   * @return the Pointcut instance (never {@code null})
   */
  protected AbstractRegexpMethodPointcut createPointcut() {
    return new JdkRegexpMethodPointcut();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": advice [" + getAdvice() +
            "], pointcut patterns " + ObjectUtils.nullSafeToString(this.patterns);
  }

  /**
   * Empty class used for a serializable monitor object.
   */
  private static final class SerializableMonitor implements Serializable {

  }

}
