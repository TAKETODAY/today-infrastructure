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

package cn.taketoday.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a bean qualifies as a fallback autowire candidate.
 * This is a companion and alternative to the {@link Primary} annotation.
 *
 * <p>If all beans but one among multiple matching candidates are marked
 * as a fallback, the remaining bean will be selected.
 *
 * <p>Just like primary beans, fallback beans only have an effect when
 * finding multiple candidates for single injection points.
 * All type-matching beans are included when autowiring arrays,
 * collections, maps, or ObjectProvider streams.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Primary
 * @see Lazy
 * @see Bean
 * @see cn.taketoday.beans.factory.config.BeanDefinition#setFallback
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Fallback {

}
