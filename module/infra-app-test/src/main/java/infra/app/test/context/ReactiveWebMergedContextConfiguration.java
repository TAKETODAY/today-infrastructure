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

package infra.app.test.context;

import java.io.Serial;

import infra.test.context.MergedContextConfiguration;

/**
 * Encapsulates the <em>merged</em> context configuration declared on a test class and all
 * of its superclasses for a reactive web application.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactiveWebMergedContextConfiguration extends MergedContextConfiguration {

  @Serial
  private static final long serialVersionUID = 1L;

  public ReactiveWebMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
    super(mergedConfig);
  }

}
