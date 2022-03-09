/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

/**
 * Implementation package for {@code java.util.concurrent} based caches.
 * Provides a {@link cn.taketoday.cache.CacheManager CacheManager}
 * and {@link cn.taketoday.cache.Cache Cache} implementation for
 * use in a Framework context, using a JDK based thread pool at runtime.
 */
@NonNullApi
@NonNullFields
package cn.taketoday.cache.concurrent;

import cn.taketoday.lang.NonNullApi;
import cn.taketoday.lang.NonNullFields;
