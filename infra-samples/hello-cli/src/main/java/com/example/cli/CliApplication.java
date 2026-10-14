/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cli;

import infra.app.Application;
import infra.app.ApplicationRunner;
import infra.app.InfraApplication;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;

/**
 * 一次性命令行应用，执行启动任务后关闭应用上下文。
 */
@InfraApplication
public class CliApplication {

  /**
   * 启动普通应用并执行问候任务。
   *
   * @param args 配置选项和需要问候的姓名
   */
  public static void main(String[] args) {
    try (var context = Application.forNormalApplication(CliApplication.class).run(args)) {
      // 启动任务已经执行完成，离开作用域时关闭应用上下文。
    }
  }

  @Bean
  ApplicationRunner greet(GreetingService greetingService) {
    return args -> {
      if (args.getNonOptionArgs().isEmpty()) {
        greetingService.greet("World");
      }
      else {
        for (String name : args.getNonOptionArgs()) {
          greetingService.greet(name);
        }
      }
    };
  }

}
