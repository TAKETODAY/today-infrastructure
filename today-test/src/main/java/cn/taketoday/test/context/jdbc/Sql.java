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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.test.context.transaction.TransactionalTestExecutionListener;

/**
 * {@code @Sql} is used to annotate a test class or test method to configure
 * SQL {@link #scripts} and {@link #statements} to be executed against a given
 * database during integration tests.
 *
 * <p>Method-level declarations override class-level declarations by default,
 * but this behavior can be configured via {@link SqlMergeMode @SqlMergeMode}.
 *
 * <p>Script execution is performed by the {@link SqlScriptsTestExecutionListener},
 * which is enabled by default.
 *
 * <p>The configuration options provided by this annotation and
 * {@link SqlConfig @SqlConfig} are equivalent to those supported by
 * {@link cn.taketoday.jdbc.datasource.init.ScriptUtils ScriptUtils} and
 * {@link cn.taketoday.jdbc.datasource.init.ResourceDatabasePopulator ResourceDatabasePopulator}
 * but are a superset of those provided by the {@code <jdbc:initialize-database/>}
 * XML namespace element. Consult the javadocs of individual attributes in this
 * annotation and {@link SqlConfig @SqlConfig} for details.
 *
 * <p>{@code @Sql} can be used as a <em>{@linkplain Repeatable repeatable}</em>
 * annotation. Otherwise, {@link SqlGroup @SqlGroup} can be used as an explicit
 * container for declaring multiple instances of {@code @Sql}.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * @author Sam Brannen
 * @see SqlConfig
 * @see SqlMergeMode
 * @see SqlGroup
 * @see SqlScriptsTestExecutionListener
 * @see cn.taketoday.transaction.annotation.Transactional
 * @see TransactionalTestExecutionListener
 * @see cn.taketoday.jdbc.datasource.init.ResourceDatabasePopulator
 * @see cn.taketoday.jdbc.datasource.init.ScriptUtils
 * @since 4.1
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(SqlGroup.class)
public @interface Sql {

  /**
   * Alias for {@link #scripts}.
   * <p>This attribute may <strong>not</strong> be used in conjunction with
   * {@link #scripts}, but it may be used instead of {@link #scripts}.
   *
   * @see #scripts
   * @see #statements
   */
  @AliasFor("scripts")
  String[] value() default {};

  /**
   * The paths to the SQL scripts to execute.
   * <p>This attribute may <strong>not</strong> be used in conjunction with
   * {@link #value}, but it may be used instead of {@link #value}. Similarly,
   * this attribute may be used in conjunction with or instead of
   * {@link #statements}.
   * <h3>Path Resource Semantics</h3>
   * <p>Each path will be interpreted as a Spring
   * {@link cn.taketoday.core.io.Resource Resource}. A plain path
   * &mdash; for example, {@code "schema.sql"} &mdash; will be treated as a
   * classpath resource that is <em>relative</em> to the package in which the
   * test class is defined. A path starting with a slash will be treated as an
   * <em>absolute</em> classpath resource, for example:
   * {@code "/org/example/schema.sql"}. A path which references a
   * URL (e.g., a path prefixed with
   * {@link cn.taketoday.util.ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
   * {@link cn.taketoday.util.ResourceUtils#FILE_URL_PREFIX file:},
   * {@code http:}, etc.) will be loaded using the specified resource protocol.
   * <h3>Default Script Detection</h3>
   * <p>If no SQL scripts or {@link #statements} are specified, an attempt will
   * be made to detect a <em>default</em> script depending on where this
   * annotation is declared. If a default cannot be detected, an
   * {@link IllegalStateException} will be thrown.
   * <ul>
   * <li><strong>class-level declaration</strong>: if the annotated test class
   * is {@code com.example.MyTest}, the corresponding default script is
   * {@code "classpath:com/example/MyTest.sql"}.</li>
   * <li><strong>method-level declaration</strong>: if the annotated test
   * method is named {@code testMethod()} and is defined in the class
   * {@code com.example.MyTest}, the corresponding default script is
   * {@code "classpath:com/example/MyTest.testMethod.sql"}.</li>
   * </ul>
   *
   * @see #value
   * @see #statements
   */
  @AliasFor("value")
  String[] scripts() default {};

  /**
   * <em>Inlined SQL statements</em> to execute.
   * <p>This attribute may be used in conjunction with or instead of
   * {@link #scripts}.
   * <h3>Ordering</h3>
   * <p>Statements declared via this attribute will be executed after
   * statements loaded from resource {@link #scripts}. If you wish to have
   * inlined statements executed before scripts, simply declare multiple
   * instances of {@code @Sql} on the same class or method.
   *
   * @see #scripts
   * @since 4.0
   */
  String[] statements() default {};

  /**
   * When the SQL scripts and statements should be executed.
   * <p>Defaults to {@link ExecutionPhase#BEFORE_TEST_METHOD BEFORE_TEST_METHOD}.
   */
  ExecutionPhase executionPhase() default ExecutionPhase.BEFORE_TEST_METHOD;

  /**
   * Local configuration for the SQL scripts and statements declared within
   * this {@code @Sql} annotation.
   * <p>See the class-level javadocs for {@link SqlConfig} for explanations of
   * local vs. global configuration, inheritance, overrides, etc.
   * <p>Defaults to an empty {@link SqlConfig @SqlConfig} instance.
   */
  SqlConfig config() default @SqlConfig;

  /**
   * Enumeration of <em>phases</em> that dictate when SQL scripts are executed.
   */
  enum ExecutionPhase {

    /**
     * The configured SQL scripts and statements will be executed
     * <em>before</em> the corresponding test method.
     */
    BEFORE_TEST_METHOD,

    /**
     * The configured SQL scripts and statements will be executed
     * <em>after</em> the corresponding test method.
     */
    AFTER_TEST_METHOD
  }

}
