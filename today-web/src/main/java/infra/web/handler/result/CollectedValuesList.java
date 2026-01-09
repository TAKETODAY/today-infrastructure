/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.result;

import java.util.ArrayList;
import java.util.List;

import infra.core.ResolvableType;

/**
 * List of collect values where all elements are a specified type.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/8/27 21:31
 */
@SuppressWarnings("serial")
public class CollectedValuesList extends ArrayList<Object> {

  private final ResolvableType elementType;

  CollectedValuesList(ResolvableType elementType) {
    this.elementType = elementType;
  }

  public ResolvableType getReturnType() {
    return ResolvableType.forClassWithGenerics(List.class, this.elementType);
  }

}