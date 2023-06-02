/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HandlerMapping;

/**
 * Implementation of the {@link HandlerMapping} interface that maps from URLs
 * to beans with names that start with a slash ("/"), similar to how Struts
 * maps URLs to action names.
 *
 * <p>This is the default implementation used by the
 * {@link cn.taketoday.web.handler.DispatcherHandler}, along with
 * {@link cn.taketoday.web.handler.method.RequestMappingHandlerMapping}.
 * Alternatively, {@link SimpleUrlHandlerMapping}
 * allows for customizing a handler mapping declaratively.
 *
 * <p>The mapping is from URL to bean name. Thus an incoming URL "/foo" would map
 * to a handler named "/foo", or to "/foo /foo2" in case of multiple mappings to
 * a single handler.
 *
 * <p>Supports direct matches (given "/test" -&gt; registered "/test") and "*"
 * matches (given "/test" -&gt; registered "/t*"). For details on the pattern
 * options, see the {@link cn.taketoday.web.util.pattern.PathPattern} javadoc.
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
    String[] aliases = obtainApplicationContext().getAliases(beanName);
    for (String alias : aliases) {
      if (alias.startsWith("/")) {
        urls.add(alias);
      }
    }
    return StringUtils.toStringArray(urls);
  }

}
