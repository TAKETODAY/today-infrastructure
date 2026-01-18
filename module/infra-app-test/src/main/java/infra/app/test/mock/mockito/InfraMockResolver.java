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

package infra.app.test.mock.mockito;

import org.mockito.plugins.MockResolver;

import infra.test.util.AopTestUtils;

/**
 * A {@link MockResolver} for testing Infra applications with  Resolves
 * mocks by returning the {@link AopTestUtils#getUltimateTargetObject(Object) ultimate
 * target object} of the instance.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InfraMockResolver implements MockResolver {

  @Override
  public Object resolve(Object instance) {
    return AopTestUtils.getUltimateTargetObject(instance);
  }

}
