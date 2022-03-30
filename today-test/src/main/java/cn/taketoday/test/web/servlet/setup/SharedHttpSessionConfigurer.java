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

package cn.taketoday.test.web.servlet.setup;

import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.servlet.request.RequestPostProcessor;
import cn.taketoday.web.context.WebApplicationContext;
import jakarta.servlet.http.HttpSession;

/**
 * {@link MockMvcConfigurer} that stores and re-uses the HTTP session across
 * multiple requests performed through the same {@code MockMvc} instance.
 *
 * <p>Example use:
 * <pre class="code">
 * import static cn.taketoday.test.web.servlet.setup.SharedHttpSessionConfigurer.sharedHttpSession;
 *
 * // ...
 *
 * MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
 *         .apply(sharedHttpSession())
 *         .build();
 *
 * // Use mockMvc to perform requests ...
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class SharedHttpSessionConfigurer implements MockMvcConfigurer {

  @Nullable
  private HttpSession session;

  @Override
  public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
    builder.alwaysDo(result -> this.session = result.getRequest().getSession(false));
  }

  @Override
  public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder,
          WebApplicationContext context) {

    return request -> {
      if (this.session != null) {
        request.setSession(this.session);
      }
      return request;
    };
  }

  public static SharedHttpSessionConfigurer sharedHttpSession() {
    return new SharedHttpSessionConfigurer();
  }

}
