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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.test.context.transaction.TransactionalTestExecutionListener;

/**
 * {@code @Rollback} is a test annotation that is used to indicate whether
 * a <em>test-managed transaction</em> should be <em>rolled back</em> after
 * the test method has completed.
 *
 * <p>Consult the class-level Javadoc for
 * {@link TransactionalTestExecutionListener}
 * for an explanation of <em>test-managed transactions</em>.
 *
 * <p>When declared as a class-level annotation, {@code @Rollback} defines
 * the default rollback semantics for all test methods within the test class
 * hierarchy. When declared as a method-level annotation, {@code @Rollback}
 * defines rollback semantics for the specific test method, potentially
 * overriding class-level default commit or rollback semantics.
 *
 * <p>As of Spring Framework 4.2, {@code @Commit} can be used as direct
 * replacement for {@code @Rollback(false)}.
 *
 * <p><strong>Warning</strong>: Declaring {@code @Commit} and {@code @Rollback}
 * on the same test method or on the same test class is unsupported and may
 * lead to unpredictable results.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create
 * custom <em>composed annotations</em>. Consult the source code for
 * {@link Commit @Commit} for a concrete example.
 *
 * <p>As of Spring Framework 5.3, this annotation will be inherited from an
 * enclosing test class by default. See
 * {@link NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * @author Sam Brannen
 * @see Commit
 * @see TransactionalTestExecutionListener
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Rollback {

  /**
   * Whether the <em>test-managed transaction</em> should be rolled back
   * after the test method has completed.
   * <p>If {@code true}, the transaction will be rolled back; otherwise,
   * the transaction will be committed.
   * <p>Defaults to {@code true}.
   */
  boolean value() default true;

}
