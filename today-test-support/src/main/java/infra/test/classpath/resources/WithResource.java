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

package infra.test.classpath.resources;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes a resource directory available from the thread context class loader.
 * <p>
 * For cases where one resource needs to refer to another, the resource's content may
 * contain the placeholder <code>${resourceRoot}</code>. It will be replaced with the path
 * to the root of the resources. For example, a resource with the {@link #name}
 * {@code example.txt} can be referenced using <code>${resourceRoot}/example.txt</code>.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@Inherited
@Repeatable(WithResources.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@ExtendWith(ResourcesExtension.class)
public @interface WithResource {

  /**
   * The name of the resource.
   *
   * @return the name
   */
  String name();

  /**
   * The content of the resource. When omitted an empty resource will be created.
   *
   * @return the content
   */
  String content() default "";

  /**
   * Whether the resource should be available in addition to those that are already on
   * the classpath are instead of any existing resources with the same name.
   *
   * @return whether this is an additional resource
   */
  boolean additional() default true;

}
