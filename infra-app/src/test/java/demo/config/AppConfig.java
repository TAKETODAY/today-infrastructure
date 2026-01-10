/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package demo.config;

import infra.context.annotation.ComponentScan;
import infra.context.annotation.Configuration;
import infra.web.HandlerInterceptor;
import infra.web.config.annotation.InterceptorRegistry;
import infra.web.config.annotation.WebMvcConfigurer;

/**
 * @author TODAY 2021/8/29 22:20
 */
@Configuration
@ComponentScan("infra.web.demo")
public class AppConfig implements WebMvcConfigurer {

  HandlerInterceptor interceptor;

  public void configureInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(interceptor)
            .addPathPatterns("/app/*", "/api/**");
  }

}
