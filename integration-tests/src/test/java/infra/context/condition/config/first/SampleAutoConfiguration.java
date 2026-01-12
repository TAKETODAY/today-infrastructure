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

package infra.context.condition.config.first;

import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionEvaluationReport;
import infra.context.condition.ConditionalOnProperty;

/**
 * Sample auto-configuration for {@link ConditionEvaluationReport} tests.
 *
 * @author Madhura Bhave
 */
@AutoConfiguration("autoConfigOne")
@ConditionalOnProperty("sample.first")
public class SampleAutoConfiguration {

  @Bean
  public String one() {
    return "one";
  }

}
