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

package cn.taketoday.buildpack.platform.build;

/**
 * Strategy interface used to resolve a {@link BuildpackReference} to a {@link Buildpack}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BuildpackResolvers
 * @since 4.0
 */
interface BuildpackResolver {

  /**
   * Attempt to resolve the given {@link BuildpackReference}.
   *
   * @param context the resolver context
   * @param reference the reference to resolve
   * @return a resolved {@link Buildpack} instance or {@code null}
   */
  Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference);

}
