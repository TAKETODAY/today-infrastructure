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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.format.annotation.DurationFormat;
import cn.taketoday.format.annotation.DurationStyle;
import cn.taketoday.format.annotation.DurationUnit;
import cn.taketoday.lang.Nullable;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Create a mock {@link TypeDescriptor} with optional {@link DurationUnit @DurationUnit}
 * and {@link DurationFormat @DurationFormat} annotations.
 *
 * @author Phillip Webb
 */
public final class MockDurationTypeDescriptor {

  private MockDurationTypeDescriptor() {
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static TypeDescriptor get(@Nullable ChronoUnit unit, @Nullable DurationStyle style) {
    TypeDescriptor descriptor = mock(TypeDescriptor.class);
    if (unit != null) {
      DurationUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(Collections.singletonMap("value", unit),
              DurationUnit.class, null);
      given(descriptor.getAnnotation(DurationUnit.class)).willReturn(unitAnnotation);
    }
    if (style != null) {
      DurationFormat formatAnnotation = AnnotationUtils
              .synthesizeAnnotation(Collections.singletonMap("value", style), DurationFormat.class, null);
      given(descriptor.getAnnotation(DurationFormat.class)).willReturn(formatAnnotation);
    }
    given(descriptor.getType()).willReturn((Class) Duration.class);
    given(descriptor.getObjectType()).willReturn((Class) Duration.class);
    return descriptor;
  }

}
