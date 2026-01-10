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

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import infra.core.TypeDescriptor;
import infra.core.annotation.AnnotationUtils;
import infra.format.annotation.PeriodFormat;
import infra.format.annotation.PeriodStyle;
import infra.format.annotation.PeriodUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Create a mock {@link TypeDescriptor} with optional {@link PeriodUnit @PeriodUnit} and
 * {@link PeriodFormat @PeriodFormat} annotations.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 */
public final class MockPeriodTypeDescriptor {

  private MockPeriodTypeDescriptor() {
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static TypeDescriptor get(ChronoUnit unit, PeriodStyle style) {
    TypeDescriptor descriptor = mock(TypeDescriptor.class);
    if (unit != null) {
      PeriodUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(Collections.singletonMap("value", unit),
              PeriodUnit.class, null);
      given(descriptor.getAnnotation(PeriodUnit.class)).willReturn(unitAnnotation);
    }
    if (style != null) {
      PeriodFormat formatAnnotation = AnnotationUtils
              .synthesizeAnnotation(Collections.singletonMap("value", style), PeriodFormat.class, null);
      given(descriptor.getAnnotation(PeriodFormat.class)).willReturn(formatAnnotation);
    }
    given(descriptor.getType()).willReturn((Class) Period.class);
    given(descriptor.getObjectType()).willReturn((Class) Period.class);
    return descriptor;
  }

}
