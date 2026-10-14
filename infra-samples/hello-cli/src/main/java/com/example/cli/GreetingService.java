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

import infra.beans.factory.annotation.Value;
import infra.stereotype.Service;

/**
 * 使用外部化配置生成问候语的业务组件。
 */
@Service
public class GreetingService {

  private final String greeting;

  /**
   * 创建问候服务。
   * @param greeting 问候语前缀
   */
  public GreetingService(@Value("${cli.greeting:Hello}") String greeting) {
    this.greeting = greeting;
  }

  /**
   * 将问候语输出到标准输出。
   * @param name 需要问候的姓名
   */
  public void greet(String name) {
    System.out.println(greeting + ", " + name + "!");
  }

}
