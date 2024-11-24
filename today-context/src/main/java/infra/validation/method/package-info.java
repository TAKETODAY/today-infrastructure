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
 * Abstractions and support classes for method validation, independent of the
 * underlying validation library.
 *
 * <p>The main abstractions:
 * <ul>
 * <li>{@link infra.validation.method.MethodValidator} to apply
 * method validation, and return or handle the results.
 * <li>{@link infra.validation.method.MethodValidationResult} and
 * related types to represent the results.
 * <li>{@link infra.validation.method.MethodValidationException}
 * to expose method validation results.
 * </ul>
 */

@NonNullApi
@NonNullFields
package infra.validation.method;

import infra.lang.NonNullApi;
import infra.lang.NonNullFields;