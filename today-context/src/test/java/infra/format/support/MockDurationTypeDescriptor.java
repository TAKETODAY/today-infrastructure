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

package infra.format.support;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import infra.core.TypeDescriptor;
import infra.core.annotation.AnnotationUtils;
import infra.format.annotation.DurationFormat;
import infra.format.annotation.DurationFormat.Style;
import infra.format.annotation.DurationUnit;
import infra.lang.Nullable;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Create a mock {@link TypeDescriptor} with optional {@link DurationUnit @DurationUnit}
 * and {@link DurationFormat @DurationFormat} annotations.
 *
 * @author Phillip Webb
 */
public final class MockDurationTypeDescriptor {

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static TypeDescriptor get(@Nullable ChronoUnit unit, @Nullable Style style) {
    TypeDescriptor descriptor = mock(TypeDescriptor.class);
    if (unit != null) {
      DurationUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(Collections.singletonMap("value", unit),
              DurationUnit.class, null);
      given(descriptor.getAnnotation(DurationUnit.class)).willReturn(unitAnnotation);
    }
    if (style != null) {
      DurationFormat formatAnnotation = AnnotationUtils.synthesizeAnnotation(
              Map.of("style", style, "unit", DurationFormat.Unit.fromChronoUnit(unit)), DurationFormat.class, null);

      given(descriptor.getAnnotation(DurationFormat.class)).willReturn(formatAnnotation);
    }
    given(descriptor.getType()).willReturn((Class) Duration.class);
    given(descriptor.getObjectType()).willReturn((Class) Duration.class);
    return descriptor;
  }

}
