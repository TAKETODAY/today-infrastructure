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

package cn.taketoday.core.conversion.support.annotation;

import java.util.Collections;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.util.DataSize;
import cn.taketoday.util.DataUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Create a mock {@link TypeDescriptor} with optional {@link DataSizeUnit @DataSizeUnit}
 * annotation.
 *
 * @author Stephane Nicoll
 */
public final class MockDataSizeTypeDescriptor {

  private MockDataSizeTypeDescriptor() {
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static TypeDescriptor get(DataUnit unit) {
    TypeDescriptor descriptor = mock(TypeDescriptor.class);
    if (unit != null) {
      DataSizeUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(Collections.singletonMap("value", unit),
              DataSizeUnit.class, null);
      given(descriptor.getAnnotation(DataSizeUnit.class)).willReturn(unitAnnotation);
    }
    given(descriptor.getType()).willReturn((Class) DataSize.class);
    given(descriptor.getObjectType()).willReturn((Class) DataSize.class);
    return descriptor;
  }

}
