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

package infra.test.context.aot;

import infra.aot.hint.RuntimeHints;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.support.GenericApplicationContext;
import infra.context.support.StaticApplicationContext;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.support.AbstractContextLoader;

/**
 * @author Sam Brannen
 */
class AbstractAotContextLoader extends AbstractContextLoader implements AotContextLoader {

  @Override
  public final GenericApplicationContext loadContext(MergedContextConfiguration mergedConfig) {
    return new StaticApplicationContext();
  }

  @Override
  public ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig, RuntimeHints runtimeHints) throws Exception {
    return loadContext(mergedConfig);
  }

  @Override
  public final GenericApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig, ApplicationContextInitializer initializer) {
    return loadContext(mergedConfig);
  }

  @Override
  protected final String getResourceSuffix() {
    throw new UnsupportedOperationException();
  }

}
