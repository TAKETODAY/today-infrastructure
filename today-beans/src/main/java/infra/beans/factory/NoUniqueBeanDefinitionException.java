/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.beans.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import infra.core.ResolvableType;
import infra.lang.Nullable;
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
