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
 * Support package for declarative JSR-107 caching configuration. Used
 * by the regular Framework's caching configuration when it detects the
 * JSR-107 API and Framework's JCache implementation.
 *
 * <p>Provide an extension of the {@code CachingConfigurer} that exposes
 * the exception cache resolver to use, see {@code JCacheConfigurer}.
 */
@NullMarked
package infra.cache.jcache.config;

import org.jspecify.annotations.NullMarked;
