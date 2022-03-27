/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.retry.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.retry.backoff.BackOffPolicy;
import cn.taketoday.retry.backoff.ExponentialBackOffPolicy;

/**
 * Collects metadata for a {@link BackOffPolicy}. Features:
 *
 * <ul>
 * <li>With no explicit settings the default is a fixed delay of 1000ms</li>
 * <li>Only the {@link #delay()} set: the backoff is a fixed delay with that value</li>
 * <li>When {@link #delay()} and {@link #maxDelay()} are set the backoff is uniformly
 * distributed between the two values</li>
 * <li>With {@link #delay()}, {@link #maxDelay()} and {@link #multiplier()} the backoff is
 * exponentially growing up to the maximum value</li>
 * <li>If, in addition, the {@link #random()} flag is set then the multiplier is chosen
 * for each delay from a uniform distribution in [1, multiplier-1]</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Gary Russell
 * @since 4.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Backoff {

  /**
   * Synonym for {@link #delay()}. When {@link #delay()} is non-zero, value of this
   * element is ignored, otherwise value of this element is taken.
   *
   * @return the delay in milliseconds (default 1000)
   */
  long value() default 1000;

  /**
   * A canonical backoff period. Used as an initial value in the exponential case, and
   * as a minimum value in the uniform case. When the value of this element is 0, value
   * of element {@link #value()} is taken, otherwise value of this element is taken and
   * {@link #value()} is ignored.
   *
   * @return the initial or canonical backoff period in milliseconds (default 0)
   */
  long delay() default 0;

  /**
   * The maximum wait (in milliseconds) between retries. If less than the
   * {@link #delay()} then the default of
   * {@value ExponentialBackOffPolicy#DEFAULT_MAX_INTERVAL}
   * is applied.
   *
   * @return the maximum delay between retries (default 0 = ignored)
   */
  long maxDelay() default 0;

  /**
   * If positive, then used as a multiplier for generating the next delay for backoff.
   *
   * @return a multiplier to use to calculate the next backoff delay (default 0 =
   * ignored)
   */
  double multiplier() default 0;

  /**
   * An expression evaluating to the canonical backoff period. Used as an initial value
   * in the exponential case, and as a minimum value in the uniform case. Overrides
   * {@link #delay()}.
   *
   * @return the initial or canonical backoff period in milliseconds.
   */
  String delayExpression() default "";

  /**
   * An expression evaluating to the maximum wait (in milliseconds) between retries. If
   * less than the {@link #delay()} then the default of
   * {@value ExponentialBackOffPolicy#DEFAULT_MAX_INTERVAL}
   * is applied. Overrides {@link #maxDelay()}
   *
   * @return the maximum delay between retries (default 0 = ignored)
   */
  String maxDelayExpression() default "";

  /**
   * Evaluates to a value used as a multiplier for generating the next delay for
   * backoff. Overrides {@link #multiplier()}.
   *
   * @return a multiplier expression to use to calculate the next backoff delay (default
   * 0 = ignored)
   */
  String multiplierExpression() default "";

  /**
   * In the exponential case ({@link #multiplier()} &gt; 0) set this to true to have the
   * backoff delays randomized, so that the maximum delay is multiplier times the
   * previous delay and the distribution is uniform between the two values.
   *
   * @return the flag to signal randomization is required (default false)
   */
  boolean random() default false;

  /**
   * Evaluates to a value. In the exponential case ({@link #multiplier()} &gt; 0) set
   * this to true to have the backoff delays randomized, so that the maximum delay is
   * multiplier times the previous delay and the distribution is uniform between the two
   * values.
   *
   * @return the flag to signal randomization is required (default false)
   */
  String randomExpression() default "";

}
