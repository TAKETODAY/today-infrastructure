/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.factory;

import java.util.Arrays;
import java.util.Collection;

import cn.taketoday.beans.BeansException;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.util.StringUtils;

/**
 * Exception thrown when a {@code BeanFactory} is asked for a bean instance for which
 * multiple matching candidates have been found when only one matching bean was expected.
 *
 * @author TODAY 2021/10/1 20:06
 * @see BeanFactory#getBean(Class)
 * @since 4.0
 */
public class NoUniqueBeanException extends BeansException {
  private static final long serialVersionUID = 1L;

  private final int numberOfBeansFound;

  @Nullable
  private final ResolvableType resolvableType;

  @Nullable
  private final Collection<String> beanNamesFound;

  /**
   * Create a new {@code NoUniqueBeanException}.
   *
   * @param type
   *         required type of the non-unique bean
   * @param numberOfBeansFound
   *         the number of matching beans
   * @param message
   *         detailed message describing the problem
   */
  public NoUniqueBeanException(Class<?> type, int numberOfBeansFound, String message) {
    super(message);
    this.beanNamesFound = null;
    this.numberOfBeansFound = numberOfBeansFound;
    this.resolvableType = ResolvableType.fromClass(type);
  }

  /**
   * Create a new {@code NoUniqueBeanException}.
   *
   * @param type
   *         required type of the non-unique bean
   * @param beanNamesFound
   *         the names of all matching beans (as a Collection)
   */
  public NoUniqueBeanException(Class<?> type, Collection<String> beanNamesFound) {
    super("expected single matching bean but found " + beanNamesFound.size() + ": " +
                  StringUtils.collectionToString(beanNamesFound));
    this.numberOfBeansFound = beanNamesFound.size();
    this.beanNamesFound = beanNamesFound;
    this.resolvableType = ResolvableType.fromClass(type);
  }

  /**
   * Create a new {@code NoUniqueBeanException}.
   *
   * @param type
   *         required type of the non-unique bean
   * @param beanNamesFound
   *         the names of all matching beans (as an array)
   */
  public NoUniqueBeanException(Class<?> type, String... beanNamesFound) {
    this(type, Arrays.asList(beanNamesFound));
  }

  /**
   * Create a new {@code NoUniqueBeanException}.
   *
   * @param type
   *         required type of the non-unique bean
   * @param beanNamesFound
   *         the names of all matching beans (as a Collection)
   */
  public NoUniqueBeanException(@Nullable ResolvableType type, Collection<String> beanNamesFound) {
    super("expected single matching bean but found " + beanNamesFound.size() + ": " +
                  StringUtils.collectionToString(beanNamesFound));
    this.resolvableType = type;
    this.beanNamesFound = beanNamesFound;
    this.numberOfBeansFound = beanNamesFound.size();
  }

  /**
   * Create a new {@code NoUniqueBeanException}.
   *
   * @param type
   *         required type of the non-unique bean
   * @param beanNamesFound
   *         the names of all matching beans (as an array)
   */
  public NoUniqueBeanException(ResolvableType type, String... beanNamesFound) {
    this(type, Arrays.asList(beanNamesFound));
  }

  /**
   * Return the number of beans found when only one matching bean was expected.
   * For a NoUniqueBeanException, this will usually be higher than 1.
   */
  public int getNumberOfBeansFound() {
    return this.numberOfBeansFound;
  }

  /**
   * Return the names of all beans found when only one matching bean was expected.
   * Note that this may be {@code null} if not specified at construction time.
   */
  @Nullable
  public Collection<String> getBeanNamesFound() {
    return this.beanNamesFound;
  }

  /**
   * Return the required {@link ResolvableType} of the missing bean, if it was a lookup
   * <em>by type</em> that failed.
   */
  @Nullable
  public ResolvableType getResolvableType() {
    return this.resolvableType;
  }

}
