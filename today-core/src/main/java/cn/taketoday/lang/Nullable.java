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

package cn.taketoday.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

/**
 * A common annotation to declare that annotated elements can be {@code null} under
 * some circumstance.
 *
 * <p>Leverages JSR-305 meta-annotations to indicate nullability in Java to common
 * tools with JSR-305 support.
 *
 * <p>Should be used at parameter, return value, and field level. Methods override should
 * repeat parent {@code @Nullable} annotations unless they behave differently.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NonNull
 * @since 4.0 2021/8/15 17:19
 */
@Documented
@TypeQualifierNickname
@Nonnull(when = When.MAYBE)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER,
        ElementType.FIELD, ElementType.TYPE_PARAMETER, ElementType.TYPE_USE })
public @interface Nullable {

}
