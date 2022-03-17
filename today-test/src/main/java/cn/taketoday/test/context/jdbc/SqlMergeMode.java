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

package cn.taketoday.test.context.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @SqlMergeMode} is used to annotate a test class or test method to
 * configure whether method-level {@code @Sql} declarations are merged with
 * class-level {@code @Sql} declarations.
 *
 * <p>A method-level {@code @SqlMergeMode} declaration overrides a class-level
 * declaration.
 *
 * <p>If {@code @SqlMergeMode} is not declared on a test class or test method,
 * {@link MergeMode#OVERRIDE} will be used by default.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * @author Sam Brannen
 * @author Dmitry Semukhin
 * @see Sql
 * @see MergeMode#MERGE
 * @see MergeMode#OVERRIDE
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SqlMergeMode {

  /**
   * Indicates whether method-level {@code @Sql} annotations should be merged
   * with class-level {@code @Sql} annotations or override them.
   */
  MergeMode value();

  /**
   * Enumeration of <em>modes</em> that dictate whether method-level {@code @Sql}
   * declarations are merged with class-level {@code @Sql} declarations.
   */
  enum MergeMode {

    /**
     * Indicates that method-level {@code @Sql} declarations should be merged
     * with class-level {@code @Sql} declarations, with class-level SQL
     * scripts and statements executed before method-level scripts and
     * statements.
     */
    MERGE,

    /**
     * Indicates that method-level {@code @Sql} declarations should override
     * class-level {@code @Sql} declarations.
     */
    OVERRIDE

  }

}
