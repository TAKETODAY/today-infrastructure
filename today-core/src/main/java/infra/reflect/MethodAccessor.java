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

package infra.reflect;

import java.lang.reflect.Method;

/**
 * Interface for accessing and invoking methods through reflection.
 * <p>
 * Provides a unified way to handle method invocation with additional capabilities.
 *
 * @author TODAY
 * @since 2020/9/11 10:39
 */
public interface MethodAccessor extends Invoker, Accessor {

  /**
   * Returns the method object that this accessor is associated with.
   *
   * @return the method object
   */
  Method getMethod();

}
