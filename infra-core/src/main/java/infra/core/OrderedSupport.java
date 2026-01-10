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

package infra.core;

import org.jspecify.annotations.Nullable;

/**
 * @author TODAY 2019-12-27 11:31
 */
public class OrderedSupport implements Ordered {

  @Nullable
  protected Integer order;  // default: same as non-Ordered

  public OrderedSupport() { }

  public OrderedSupport(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return order == null ? LOWEST_PRECEDENCE : order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

}
