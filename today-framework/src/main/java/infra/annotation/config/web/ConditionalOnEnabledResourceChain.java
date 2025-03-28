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

package infra.annotation.config.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks whether the Infra resource handling chain
 * is enabled. Matches if {@link WebProperties.Resources.Chain#getEnabled()} is
 * {@code true} or if one of {@code "org.webjars:webjars-locator-core"},
 * {@code "org.webjars:webjars-locator-lite"} is on the classpath.
 * <p>
 * Note that support for {@code "org.webjars:webjars-locator-core"} is deprecated.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/18 23:03
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Conditional(OnEnabledResourceChainCondition.class)
public @interface ConditionalOnEnabledResourceChain {

}

