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
 * Scheduling convenience classes for the {@code java.util.concurrent}
 * and {@code jakarta.enterprise.concurrent} packages, allowing to set up a
 * ThreadPoolExecutor or ScheduledThreadPoolExecutor as a bean in a Framework
 * context. Provides support for the native {@code java.util.concurrent}
 * interfaces as well as the {@code TaskExecutor} mechanism.
 */
@NonNullApi
@NonNullFields
package infra.scheduling.concurrent;

import infra.lang.NonNullApi;
import infra.lang.NonNullFields;
