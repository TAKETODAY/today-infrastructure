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

package infra.test.web.mock;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Bean;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.mock.MockRequest;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for the registration of the {@link ApplicationContext} onto the
 * request by {@link MockMvc#perform}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public class MockMvcApplicationContextTests {

  @Test
  public void performRegistersApplicationContextOnRequest() throws Exception {
    AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
    wac.register(WebConfig.class);
    wac.refresh();

    MockMvc mockMvc = webAppContextSetup(wac).build();
    MockRequest request = mockMvc.perform(get("/")).andReturn().getRequest();

    assertThat(request.getApplicationContext()).isSameAs(wac);
  }

  @Configuration
  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Bean
    public Object controller() {
      return new Object();
    }

  }

}
