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

package infra.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Specifies that the method return value must be used.
 *
 * <p>Inspired by {@code org.jetbrains.annotations.CheckReturnValue}, this variant
 * has been introduced in the {@code infra.lang} package to avoid
 * requiring an extra dependency, while still following similar semantics.
 *
 * <p>This annotation should not be used if the return value of the method
 * provides only <i>additional</i> information. For example, the main purpose
 * of {@link java.util.Collection#add(Object)} is to modify the collection
 * and the return value is only interesting when adding an element to a set,
 * to see if the set already contained that element before.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface CheckReturnValue {
}
