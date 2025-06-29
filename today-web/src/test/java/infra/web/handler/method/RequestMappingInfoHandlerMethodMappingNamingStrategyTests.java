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

package infra.web.handler.method;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.util.ReflectionUtils;
import infra.web.annotation.RequestMapping;
import infra.web.handler.HandlerMethodMappingNamingStrategy;
import infra.web.handler.condition.ConsumesRequestCondition;
import infra.web.handler.condition.HeadersRequestCondition;
import infra.web.handler.condition.ParamsRequestCondition;
import infra.web.handler.condition.PathPatternsRequestCondition;
import infra.web.handler.condition.ProducesRequestCondition;
import infra.web.handler.condition.RequestConditionHolder;
import infra.web.handler.condition.RequestMethodsRequestCondition;
import infra.web.handler.condition.VersionRequestCondition;
import infra.web.handler.method.RequestMappingInfo.BuilderConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/9 20:01
 */
public class RequestMappingInfoHandlerMethodMappingNamingStrategyTests {

  @Test
  public void getNameExplicit() {
    Method method = ReflectionUtils.getMethod(TestController.class, "handle");
    HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);

    RequestMappingInfo rmi = new RequestMappingInfo("foo",
            new PathPatternsRequestCondition(), new RequestMethodsRequestCondition(),
            new ParamsRequestCondition(), new HeadersRequestCondition(),
            new ConsumesRequestCondition(), new ProducesRequestCondition(), new VersionRequestCondition(null, null),
            new RequestConditionHolder(null), new BuilderConfiguration(), true);

    HandlerMethodMappingNamingStrategy<RequestMappingInfo> strategy = new RequestMappingInfoHandlerMethodMappingNamingStrategy();

    assertThat(strategy.getName(handlerMethod, rmi)).isEqualTo("foo");
  }

  @Test
  public void getNameConvention() {
    Method method = ReflectionUtils.getMethod(TestController.class, "handle");
    HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);

    RequestMappingInfo rmi = new RequestMappingInfo(null,
            new PathPatternsRequestCondition(), new RequestMethodsRequestCondition(),
            new ParamsRequestCondition(), new HeadersRequestCondition(),
            new ConsumesRequestCondition(), new ProducesRequestCondition(),
            new VersionRequestCondition(null, null),
            new RequestConditionHolder(null), new BuilderConfiguration(), true);

    var strategy = new RequestMappingInfoHandlerMethodMappingNamingStrategy();

    assertThat(strategy.getName(handlerMethod, rmi)).isEqualTo("TC#handle");
  }

  private static class TestController {

    @RequestMapping
    public void handle() {
    }
  }

}
