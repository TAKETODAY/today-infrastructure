/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.handler.method.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.annotation.RequestHeader;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.bind.resolver.RequestHeaderMethodArgumentResolver;
import cn.taketoday.web.bind.resolver.RequestParamMethodArgumentResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/18 20:33
 */
class CompositeUriComponentsContributorTests {

  @Test
  void supportsParameter() {
    List<ParameterResolvingStrategy> resolvers = new ArrayList<>();
    resolvers.add(new RequestParamMethodArgumentResolver(false));
    resolvers.add(new RequestHeaderMethodArgumentResolver(null));
    resolvers.add(new RequestParamMethodArgumentResolver(true));

    CompositeUriComponentsContributor contributor = new CompositeUriComponentsContributor(resolvers);
    Method method = ReflectionUtils.getMethod(this.getClass(), "handleRequest", String.class, String.class, String.class);
    assertThat(contributor.supportsParameter(new MethodParameter(method, 0))).isTrue();
    assertThat(contributor.supportsParameter(new MethodParameter(method, 1))).isTrue();
    assertThat(contributor.supportsParameter(new MethodParameter(method, 2))).isFalse();
  }

  @Test
  void hasContributors() {
    assertThat(new CompositeUriComponentsContributor().hasContributors()).isFalse();
    assertThat(new CompositeUriComponentsContributor(new RequestParamMethodArgumentResolver(true)).hasContributors()).isTrue();
  }

  public void handleRequest(@RequestParam String p1, String p2, @RequestHeader String h) {
  }

}