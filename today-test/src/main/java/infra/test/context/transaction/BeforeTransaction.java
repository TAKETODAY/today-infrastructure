/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.test.context.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Test annotation which indicates that the annotated {@code void} method
 * should be executed <em>before</em> a transaction is started for a test method
 * configured to run within a transaction via Framework's {@code @Transactional}
 * annotation.
 *
 * <p>{@code @BeforeTransaction} methods declared in superclasses or as interface
 * default methods will be executed before those of the current test class.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p>{@code @BeforeTransaction} may also be
 * declared on Java 8 based interface default methods.
 *
 * @author Sam Brannen
 * @see infra.transaction.annotation.Transactional
 * @see AfterTransaction
 * @since 4.0
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BeforeTransaction {
}
