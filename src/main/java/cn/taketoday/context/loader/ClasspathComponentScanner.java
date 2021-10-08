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

package cn.taketoday.context.loader;

import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.util.ObjectUtils;

/**
 * @author TODAY 2021/10/2 23:38
 * @since 4.0
 */
public class ClasspathComponentScanner {

  /** @since 2.1.7 Scan candidates */
  private CandidateComponentScanner componentScanner;

  protected CandidateComponentScanner createCandidateComponentScanner() {
    return CandidateComponentScanner.getSharedInstance();
  }

  protected Set<Class<?>> getComponentCandidates() {
    CandidateComponentScanner scanner = getCandidateComponentScanner();
    if (ObjectUtils.isEmpty(locations)) {
      // Candidates have not been set or scanned
      if (scanner.getCandidates() == null) {
        return scanner.scan();// scan all class path
      }
      return scanner.getScanningCandidates();
    }
    return scanner.scan(locations);
  }

  private CandidateComponentScanner getCandidateComponentScanner() {

  }

  /**
   * Load {@link BeanDefinition}s from input package locations
   *
   * <p>
   * {@link CandidateComponentScanner} will scan the classes from given package
   * locations. And register the {@link BeanDefinition}s using
   * loadBeanDefinition(Class)
   *
   * @param locations
   *         package locations
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @since 4.0
   */
  public void scan(String... locations) throws BeanDefinitionStoreException {
    // Loading candidates components
    log.info("Loading candidates components");
    Set<Class<?>> candidates = getComponentCandidates();
    log.info("There are [{}] candidates components in [{}]", candidates.size(), this);

  }

}
