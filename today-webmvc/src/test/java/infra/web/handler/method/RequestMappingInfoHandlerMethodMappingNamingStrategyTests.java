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
