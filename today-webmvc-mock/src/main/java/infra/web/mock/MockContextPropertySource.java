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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import infra.core.env.EnumerablePropertySource;
import infra.core.env.PropertySource;
import infra.lang.Constant;
import infra.mock.api.MockContext;
import infra.util.CollectionUtils;

/**
 * {@link PropertySource} that reads init parameters from a {@link MockContext} object.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockConfigPropertySource
 * @since 4.0 2022/2/20 17:10
 */
public class MockContextPropertySource extends EnumerablePropertySource<MockContext> {

  public MockContextPropertySource(String name, MockContext mockContext) {
    super(name, mockContext);
  }

  @Override
  public String[] getPropertyNames() {
    return CollectionUtils.toArray(source.getInitParameterNames(), Constant.EMPTY_STRING_ARRAY);
  }

  @Override
  @Nullable
  public String getProperty(String name) {
    return this.source.getInitParameter(name);
  }

}
