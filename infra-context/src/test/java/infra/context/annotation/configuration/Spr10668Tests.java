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

package infra.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 */
public class Spr10668Tests {

  @Test
  public void testSelfInjectHierarchy() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ChildConfig.class);
    assertThat(context.getBean(MyComponent.class)).isNotNull();
    assertThat(context.getBean(ParentConfig.class)).isNotNull();
    assertThat(context.getBean(ParentConfig.class).component).isNotNull();
    context.close();
  }

  @Configuration
  public static class ParentConfig {

    @Autowired(required = false)
    MyComponent component;
  }

  @Configuration
//  @Lazy
  public static class ChildConfig extends ParentConfig {

    @Bean
    public MyComponentImpl myComponent() {
      return new MyComponentImpl();
    }
  }

  public interface MyComponent { }

  public static class MyComponentImpl implements MyComponent { }

}
