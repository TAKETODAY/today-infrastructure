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

package cn.taketoday.format.support;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.format.annotation.PeriodFormat;
import cn.taketoday.format.annotation.PeriodStyle;
import cn.taketoday.format.annotation.PeriodUnit;

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
