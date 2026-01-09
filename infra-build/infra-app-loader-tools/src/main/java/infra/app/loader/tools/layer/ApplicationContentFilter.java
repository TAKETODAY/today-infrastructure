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

package infra.app.loader.tools.layer;

import infra.util.AntPathMatcher;
import infra.lang.Assert;

/**
 * {@link ContentFilter} that matches application items based on an Ant-style path
 * pattern.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ApplicationContentFilter implements ContentFilter<String> {

  private static final AntPathMatcher MATCHER = new AntPathMatcher();

  private final String pattern;

  public ApplicationContentFilter(String pattern) {
    Assert.hasText(pattern, "Pattern must not be empty");
    this.pattern = pattern;
  }

  @Override
  public boolean matches(String path) {
    return MATCHER.match(this.pattern, path);
  }

}
