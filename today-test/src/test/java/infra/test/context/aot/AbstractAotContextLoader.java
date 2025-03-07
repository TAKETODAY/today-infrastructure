/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

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
