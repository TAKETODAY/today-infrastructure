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

package cn.taketoday.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.test.context.junit4.SpringJUnit4ClassRunner;
import cn.taketoday.test.context.junit4.rules.SpringMethodRule;
import cn.taketoday.test.context.junit4.statements.SpringFailOnTimeout;

/**
 * Test annotation for use with JUnit 4 to indicate that a test method has to finish
 * execution in a {@linkplain #millis() specified time period}.
 *
 * <p>If the text execution takes longer than the specified time period, then
 * the test is considered to have failed.
 *
 * <p>Note that the time period includes execution of the test method itself,
 * any {@linkplain Repeat repetitions} of the test, and any <em>set up</em> or
 * <em>tear down</em> of the test fixture.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @see Repeat
 * @see SpringJUnit4ClassRunner
 * @see SpringMethodRule
 * @see SpringFailOnTimeout
 * @since 4.0
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timed {

  /**
   * The maximum amount of time (in milliseconds) that a test execution can
   * take without being marked as failed due to taking too long.
   */
  long millis();

}
