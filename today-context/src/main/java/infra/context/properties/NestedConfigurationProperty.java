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

package infra.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.properties.bind.Nested;

/**
 * Indicates that a field in a {@link ConfigurationProperties @ConfigurationProperties}
 * object should be treated as if it were a nested type. This annotation has no bearing on
 * the actual binding processes, but it is used by the
 * {@code infra-configuration-processor} as a hint that a field is not bound as a
 * single value. When this is specified, a nested group is created for the field and its
 * type is harvested.
 * <p>
 * This has no effect on collections and maps as these types are automatically identified.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nested
public @interface NestedConfigurationProperty {

}
