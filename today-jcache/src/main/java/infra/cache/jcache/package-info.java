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

/**
 * Implementation package for JSR-107 (javax.cache aka "JCache") based caches.
 * Provides a {@link infra.cache.CacheManager CacheManager}
 * and {@link infra.cache.Cache Cache} implementation for
 * use in a Framework context, using a JSR-107 compliant cache provider.
 */
@NullMarked
package infra.cache.jcache;

import org.jspecify.annotations.NullMarked;
