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

package infra.beans.factory;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import infra.core.ResolvableType;
import infra.util.StringUtils;

/**
 * Exception thrown when a {@code BeanFactory} is asked for a bean instance for which
 * multiple matching candidates have been found when only one matching bean was expected.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanFactory#getBean(Class)
 * @since 4.0 2021/10/1 20:06
 */
public class NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException {

  private final int numberOfBeansFound;

  @Nullable
  private final Collection<String> beanNamesFound;

  /**
   * Create a new {@code NoUniqueBeanDefinitionException}.
   *
   * @param type required type of the non-unique bean
   * @param numberOfBeansFound the number of matching beans
   * @param message detailed message describing the problem
   */
  public NoUniqueBeanDefinitionException(Class<?> type, int numberOfBeansFound, String message) {
    super(type, message);
    this.numberOfBeansFound = numberOfBeansFound;
    this.beanNamesFound = null;
  }

  /**
   * Create a new {@code NoUniqueBeanDefinitionException}.
   *
   * @param type required type of the non-unique bean
   * @param beanNamesFound the names of all matching beans (as a Collection)
   */
  public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound) {
    this(type, beanNamesFound, "expected single matching bean but found %d: %s"
            .formatted(beanNamesFound.size(), StringUtils.collectionToCommaDelimitedString(beanNamesFound)));
  }

  /**
   * Create a new {@code NoUniqueBeanDefinitionException}.
   *
   * @param type required type of the non-unique bean
   * @param beanNamesFound the names of all matching beans (as an array)
   */
  public NoUniqueBeanDefinitionException(Class<?> type, String... beanNamesFound) {
    this(type, Arrays.asList(beanNamesFound));
  }

  /**
   * Create a new {@code NoUniqueBeanDefinitionException}.
   *
   * @param type required type of the non-unique bean
   * @param beanNamesFound the names of all matching beans (as a Collection)
   */
  public NoUniqueBeanDefinitionException(ResolvableType type, Collection<String> beanNamesFound) {
    super(type, buildMessage(beanNamesFound));
    this.numberOfBeansFound = beanNamesFound.size();
    this.beanNamesFound = new ArrayList<>(beanNamesFound);
  }

  /**
   * Create a new {@code NoUniqueBeanDefinitionException}.
   *
   * @param type required type of the non-unique bean
   * @param beanNamesFound the names of all matching beans (as an array)
   */
  public NoUniqueBeanDefinitionException(ResolvableType type, String... beanNamesFound) {
    this(type, Arrays.asList(beanNamesFound));
  }

  /**
   * Create a new {@code NoUniqueBeanDefinitionException}.
   *
   * @param type required type of the non-unique bean
   * @param beanNamesFound the names of all matching beans (as a Collection)
   * @param message detailed message describing the problem
   * @since 5.0
   */
  public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound, String message) {
    super(type, message);
    this.numberOfBeansFound = beanNamesFound.size();
    this.beanNamesFound = new ArrayList<>(beanNamesFound);
  }

  /**
   * Return the number of beans found when only one matching bean was expected.
   * For a NoUniqueBeanDefinitionException, this will usually be higher than 1.
   *
   * @see #getBeanType()
   */
  @Override
  public int getNumberOfBeansFound() {
    return this.numberOfBeansFound;
  }

  /**
   * Return the names of all beans found when only one matching bean was expected.
   * Note that this may be {@code null} if not specified at construction time.
   *
   * @see #getBeanType()
   */
  @Nullable
  public Collection<String> getBeanNamesFound() {
    return this.beanNamesFound;
  }

  private static String buildMessage(Collection<String> beanNamesFound) {
    return "expected single matching bean but found %s: %s"
            .formatted(beanNamesFound.size(), StringUtils.collectionToCommaDelimitedString(beanNamesFound));
  }

}
