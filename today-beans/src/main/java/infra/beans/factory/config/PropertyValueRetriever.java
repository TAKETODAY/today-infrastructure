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

package infra.beans.factory.config;

import infra.beans.BeanWrapper;
import infra.beans.NoSuchPropertyException;

/**
 * interface for property value lazy loading
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/27 20:46</a>
 * @since 4.0
 */
public interface PropertyValueRetriever {

  /**
   * It shows that the value is not set
   */
  Object DO_NOT_SET = new Object();

  /**
   * retrieve property-path corresponding property-value
   *
   * @param propertyPath property name
   * @param binder BeanWrapper
   * @param beanFactory own bean factory
   * @return property-value maybe {@link #DO_NOT_SET} indicates that do not set property
   * @throws NoSuchPropertyException If no such property
   */
  Object retrieve(String propertyPath, BeanWrapper binder, AutowireCapableBeanFactory beanFactory);

}
