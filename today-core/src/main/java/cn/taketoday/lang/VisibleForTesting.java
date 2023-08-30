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
 * A member or type annotated with VisibleForTesting claims that its visibility is higher than necessary,
 * only for testing purposes. In particular:
 * <ul>
 *   <li>If public or protected member/type is annotated with VisibleForTesting,
 *   it's assumed that package-private access is enough for production code.</li>
 *   <li>If package-private member/type is annotated with VisibleForTesting,
 *   it's assumed that private access is enough for production code.</li>
 *   <li>It's illegal to annotate private member/type as VisibleForTesting.</li>
 * </ul>
 * <p>
 * This annotation means that the annotated element exposes internal data and breaks encapsulation
 * of the containing class; the annotation won't prevent its use from production code, developers
 * even won't see warnings if their IDE doesn't support the annotation. It's better to provide
 * proper API which can be used in production as well as in tests.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.TYPE })
public @interface VisibleForTesting {

}