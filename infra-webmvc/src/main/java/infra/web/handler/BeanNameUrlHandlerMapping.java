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

package infra.web.handler;

import java.util.ArrayList;
import java.util.List;

import infra.util.StringUtils;
import infra.web.DispatcherHandler;
import infra.web.HandlerMapping;

/**
 * Implementation of the {@link HandlerMapping} interface that maps from URLs
 * to beans with names that start with a slash ("/"), similar to how Struts
 * maps URLs to action names.
 *
 * <p>This is the default implementation used by the
 * {@link DispatcherHandler}, along with
 * {@link infra.web.handler.method.RequestMappingHandlerMapping}.
 * Alternatively, {@link SimpleUrlHandlerMapping}
 * allows for customizing a handler mapping declaratively.
 *
 * <p>The mapping is from URL to bean name. Thus an incoming URL "/foo" would map
 * to a handler named "/foo", or to "/foo /foo2" in case of multiple mappings to
 * a single handler.
 *
 * <p>Supports direct matches (given "/test" -&gt; registered "/test") and "*"
 * matches (given "/test" -&gt; registered "/t*"). For details on the pattern
 * options, see the {@link infra.web.util.pattern.PathPattern} javadoc.
 *
 * <p>
 * Supports alias placeholder resolving
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SimpleUrlHandlerMapping
 * @since 4.0 2022/1/29 00:30
 */
public class BeanNameUrlHandlerMapping extends AbstractDetectingUrlHandlerMapping {

  /**
   * Checks name and aliases of the given bean for URLs, starting with "/".
   */
  @Override
  protected String[] determineUrlsForHandler(String beanName) {
    List<String> urls = new ArrayList<>();
    if (beanName.startsWith("/")) {
      urls.add(beanName);
    }
    String[] aliases = applicationContext().getAliases(beanName);
    for (String alias : aliases) {
      if (alias.startsWith("/")) {
        urls.add(alias);
      }
      else {
        String path = resolveEmbeddedVariables(alias);
        if (path != null && path.startsWith("/")) {
          urls.add(path);
        }
      }
    }
    return StringUtils.toStringArray(urls);
  }

}
