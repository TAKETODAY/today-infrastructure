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

package infra.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Conditional;
import infra.app.cloud.CloudPlatform;

/**
 * {@link Conditional @Conditional} that matches when the specified cloud platform is
 * active.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/4 12:20
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnCloudPlatformCondition.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnCloudPlatform {

  /**
   * The {@link CloudPlatform cloud platform} that must be active.
   *
   * @return the expected cloud platform
   */
  CloudPlatform value();

}
