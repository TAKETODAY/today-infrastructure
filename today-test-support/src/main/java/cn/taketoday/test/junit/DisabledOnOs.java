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

package cn.taketoday.test.junit;

import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Improves JUnit5's {@link org.junit.jupiter.api.condition.DisabledOnOs} by adding an
 * architecture check.
 *
 * @author Moritz Halbritter
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnOsCondition.class)
public @interface DisabledOnOs {

  /**
   * The operating systems on which the annotated class or method should be disabled.
   *
   * @return the operating systems where the test is disabled
   */
  OS[] value() default {};

  /**
   * The operating systems on which the annotated class or method should be disabled.
   *
   * @return the operating systems where the test is disabled
   */
  OS[] os() default {};

  /**
   * The architectures on which the annotated class or method should be disabled.
   *
   * @return the architectures where the test is disabled
   */
  String[] architecture() default {};

  /**
   * See {@link org.junit.jupiter.api.condition.DisabledOnOs#disabledReason()}.
   *
   * @return disabled reason
   */
  String disabledReason() default "";

}
