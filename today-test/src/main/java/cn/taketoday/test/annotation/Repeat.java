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

import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.junit4.ApplicationJUnit4ClassRunner;
import cn.taketoday.test.context.junit4.rules.ApplicationMethodRule;
import cn.taketoday.test.context.junit4.statements.SpringRepeat;

/**
 * Test annotation for use with JUnit 4 to indicate that a test method should be
 * invoked repeatedly.
 *
 * <p>Note that the scope of execution to be repeated includes execution of the
 * test method itself as well as any <em>set up</em> or <em>tear down</em> of
 * the test fixture. When used with the
 * {@link ApplicationMethodRule
 * ApplicationMethodRule}, the scope additionally includes
 * {@linkplain TestExecutionListener#prepareTestInstance
 * preparation of the test instance}.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @see Timed
 * @see ApplicationJUnit4ClassRunner
 * @see ApplicationMethodRule
 * @see SpringRepeat
 * @since 4.0
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Repeat {

  /**
   * The number of times that the annotated test method should be repeated.
   */
  int value() default 1;

}
