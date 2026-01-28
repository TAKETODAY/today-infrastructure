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

package infra.sql.init.dependency;

import java.util.HashSet;
import java.util.Set;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryUtils;

/**
 * Helper class for detecting beans of particular types in a bean factory.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
abstract class BeansOfTypeDetector {

  public static Set<String> detect(Set<Class<?>> types, BeanFactory beanFactory) {
    HashSet<String> beanNames = new HashSet<>();
    for (Class<?> type : types) {
      try {
        String[] names = beanFactory.getBeanNamesForType(type, true, false);
        for (String name : names) {
          name = BeanFactoryUtils.transformedBeanName(name);
          beanNames.add(name);
        }
      }
      catch (Throwable ex) {
        // Continue
      }
    }
    return beanNames;
  }

}
