/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.conversion.support;

import java.time.Instant;
import java.util.Date;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalConverter;
import infra.core.conversion.Converter;

/**
 * Convert a {@link Instant} to a {@link Date}.
 *
 * <p>This does not include conversion support for target types which are subtypes
 * of {@code java.util.Date}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Date#from(Instant)
 * @see DateToInstantConverter
 * @since 5.0
 */
final class InstantToDateConverter implements ConditionalConverter, Converter<Instant, Date> {

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return targetType.getType().equals(Date.class);
  }

  @Override
  public Date convert(Instant instant) {
    return Date.from(instant);
  }

}
