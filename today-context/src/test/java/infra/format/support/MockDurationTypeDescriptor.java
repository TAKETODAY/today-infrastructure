/*
 * Copyright 2012-present the original author or authors.
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

package infra.format.support;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import infra.core.TypeDescriptor;
import infra.core.annotation.AnnotationUtils;
import infra.format.annotation.DurationFormat;
import infra.format.annotation.DurationFormat.Style;
import infra.format.annotation.DurationUnit;

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
