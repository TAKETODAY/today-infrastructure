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

import java.util.Collections;

import infra.core.TypeDescriptor;
import infra.core.annotation.AnnotationUtils;
import infra.format.annotation.DataSizeUnit;
import infra.util.DataSize;
import infra.util.DataUnit;

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
