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

package infra.context.properties.bind;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import infra.context.properties.source.ConfigurationPropertyName;
import infra.core.ResolvableType;

/**
 * {@link AggregateBinder} for arrays.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class ArrayBinder extends IndexedElementsBinder<Object> {

  ArrayBinder(Binder.Context context) {
    super(context);
  }

  @Nullable
  @Override
  protected Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target, AggregateElementBinder elementBinder) {
    IndexedCollectionSupplier result = new IndexedCollectionSupplier(ArrayList::new);
    ResolvableType aggregateType = target.getType();
    ResolvableType elementType = target.getType().getComponentType();
    bindIndexed(name, target, elementBinder, aggregateType, elementType, result);
    if (result.wasSupplied()) {
      List<Object> list = (List<Object>) result.get();
      int size = list.size();
      Object array = Array.newInstance(elementType.resolve(), size);
      for (int i = 0; i < size; i++) {
        Array.set(array, i, list.get(i));
      }
      return array;
    }
    return null;
  }

  @Override
  protected Object merge(Supplier<Object> existing, Object additional) {
    return additional;
  }

}
