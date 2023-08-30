/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation which marks a {@link java.util.Collection} or {@link java.util.Map} type
 * as unmodifiable. A collection or a map is unmodifiable if any mutator methods
 * (e.g. {@link java.util.Collection#add(Object)}) throw exception or have no effect,
 * and the object references stored as collection elements, map keys, and map values
 * are never changed. The referenced objects themselves still could be changed if they
 * are mutable.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/30 09:25
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE_USE })
public @interface Unmodifiable {

}