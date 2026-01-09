/*
 * Copyright 2002-present the original author or authors.
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

package infra.core;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Executable;
import java.util.ArrayList;

import infra.util.CollectionUtils;

/**
 * {@link ParameterNameDiscoverer} implementation that tries several discoverer
 * delegates in succession. Those added first in the {@code addDiscoverer} method
 * have high-est priority. If one returns {@code null}, the next will be tried.
 *
 * <p>The default behavior is to return {@code null} if no discoverer matches.
 *
 * @author TODAY 2021/9/10 23:02
 * @since 4.0
 */
public class CompositeParameterNameDiscoverer extends ParameterNameDiscoverer implements ArraySizeTrimmer {

  private final ArrayList<ParameterNameDiscoverer> discoverers = new ArrayList<>();

  /**
   * add ParameterNameDiscoverer
   *
   * @param discoverer ParameterNameDiscoverers
   */
  public void addDiscoverer(@Nullable ParameterNameDiscoverer... discoverer) {
    CollectionUtils.addAll(discoverers, discoverer);
    trimToSize();
  }

  @Override
  public String @Nullable [] getParameterNames(Executable executable) {
    for (ParameterNameDiscoverer discoverer : discoverers) {
      String[] parameterNames = discoverer.getParameterNames(executable);
      if (parameterNames != null) {
        return parameterNames;
      }
    }
    // cannot resolve
    return null;
  }

  @Override
  public void trimToSize() {
    discoverers.trimToSize();
  }

}
