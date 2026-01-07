/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.context.condition.config.second;

import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionEvaluationReport;
import infra.context.condition.ConditionalOnProperty;

/**
 * Sample auto-configuration for {@link ConditionEvaluationReport} tests.
 *
 * @author Madhura Bhave
 */
@AutoConfiguration("autoConfigTwo")
@ConditionalOnProperty("sample.second")
public class SampleAutoConfiguration {

  @Bean
  public String two() {
    return "two";
  }

}
