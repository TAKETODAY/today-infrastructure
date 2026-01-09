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

package infra.util.concurrent;

import java.util.ArrayList;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 17:30
 */
final class FutureListeners {

  public final ArrayList<FutureListener<?>> listeners;

  FutureListeners(FutureListener<? extends Future<?>> first, FutureListener<? extends Future<?>> second) {
    ArrayList<FutureListener<?>> listeners = new ArrayList<>(4);
    listeners.add(first);
    listeners.add(second);
    this.listeners = listeners;
  }

  public void add(FutureListener<? extends Future<?>> l) {
    listeners.add(l);
  }

}
