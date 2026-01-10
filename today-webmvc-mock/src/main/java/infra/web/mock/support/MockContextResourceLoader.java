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

package infra.web.mock.support;

import infra.core.io.DefaultResourceLoader;
import infra.core.io.Resource;
import infra.mock.api.MockContext;
import infra.web.mock.WebApplicationContext;
import infra.web.mock.filter.GenericFilterBean;

/**
 * ResourceLoader implementation that resolves paths as MockContext
 * resources, for use outside a WebApplicationContext (for example,
 * in an HttpServletBean or GenericFilterBean subclass).
 *
 * <p>Within a WebApplicationContext, resource paths are automatically
 * resolved as MockContext resources by the context implementation.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getResourceByPath
 * @see MockContextResource
 * @see WebApplicationContext
 * @see GenericFilterBean
 * @since 4.0 2022/2/20 16:16
 */
public class MockContextResourceLoader extends DefaultResourceLoader {

  private final MockContext mockContext;

  /**
   * Create a new MockContextResourceLoader.
   *
   * @param mockContext the MockContext to load resources with
   */
  public MockContextResourceLoader(MockContext mockContext) {
    this.mockContext = mockContext;
  }

  /**
   * This implementation supports file paths beneath the root of the web application.
   *
   * @see MockContextResource
   */
  @Override
  protected Resource getResourceByPath(String path) {
    return new MockContextResource(mockContext, path);
  }

}

