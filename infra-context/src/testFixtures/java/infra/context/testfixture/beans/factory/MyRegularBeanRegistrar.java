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

package infra.context.testfixture.beans.factory;

import infra.beans.factory.BeanRegistrar;
import infra.beans.factory.BeanRegistry;
import infra.beans.testfixture.beans.TestBean;
import infra.core.ParameterizedTypeReference;
import infra.core.env.Environment;

public class MyRegularBeanRegistrar implements BeanRegistrar {

  @Override
  public void register(BeanRegistry registry, Environment env) {
    if (registry.containsBean("testBean") &&
            registry.containsBean(TestBean.class) &&
            registry.containsBean(new ParameterizedTypeReference<Comparable<Object>>() {
            })) {
      registry.registerBean("myTestBean", TestBean.class);
    }
  }
}
