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
 * Properties editors used to convert from String values to object
 * types such as java.util.Properties.
 *
 * <p>Some of these editors are registered automatically by BeanWrapperImpl.
 * "CustomXxxEditor" classes are intended for manual registration in
 * specific binding processes, as they are localized or the like.
 */
@NonNullApi
@NonNullFields
package infra.beans.propertyeditors;

import infra.lang.NonNullApi;
import infra.lang.NonNullFields;
