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

package cn.taketoday.context.annotation.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.DependsOn;

/**
 * Hint that an {@link EnableAutoConfiguration auto-configuration} should be applied
 * before other specified auto-configuration classes.
 * <p>
 * As with standard {@link Configuration @Configuration} classes, the order in which
 * auto-configuration classes are applied only affects the order in which their beans are
 * defined. The order in which those beans are subsequently created is unaffected and is
 * determined by each bean's dependencies and any {@link DependsOn @DependsOn}
 * relationships.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 11:56
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoConfigureBefore {

  /**
   * The auto-configure classes that should have not yet been applied.
   *
   * @return the classes
   */
  Class<?>[] value() default {};

  /**
   * The names of the auto-configure classes that should have not yet been applied.
   *
   * @return the class names
   */
  String[] name() default {};

}

