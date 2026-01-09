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

package infra.core.io;

import java.util.Set;

import infra.lang.Modifiable;

/**
 * Extended ResourceConsumer
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/28 14:54
 */
public interface SmartResourceConsumer extends ResourceConsumer {

  /**
   * Check if the resource has been added
   *
   * @param resource resource to check
   * @return Check if the resource has been added
   */
  boolean contains(Resource resource);

  /**
   * All Resources
   */
  @Modifiable
  Set<Resource> getResources();

}
