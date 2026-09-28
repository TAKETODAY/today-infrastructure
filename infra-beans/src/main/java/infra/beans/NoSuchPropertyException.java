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

package infra.beans;

/**
 * Exception thrown when a requested property does not exist on a bean class.
 * Carries the bean class and missing property name through
 * {@link InvalidPropertyException}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2018-08-05 10:08
 */
public class NoSuchPropertyException extends InvalidPropertyException {

  /**
   * Create an exception for a property that could not be found on the target class.
   *
   * @param target the bean class on which the property was requested
   * @param name the name of the missing property
   */
  public NoSuchPropertyException(Class<?> target, String name) {
    super(target, name, "Property not found");
  }

}
