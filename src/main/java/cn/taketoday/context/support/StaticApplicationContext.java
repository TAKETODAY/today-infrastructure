/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.support;

import java.util.Locale;

import cn.taketoday.beans.BeansException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Nullable;

/**
 * {@link cn.taketoday.context.ApplicationContext} implementation
 * which supports programmatic registration of beans and messages,
 * rather than reading bean definitions from external configuration sources.
 * Mainly useful for testing.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #registerSingleton
 * @see #registerPrototype
 * @see #registerBeanDefinition
 * @see #refresh
 */
public class StaticApplicationContext extends GenericApplicationContext {

  private final StaticMessageSource staticMessageSource;

  /**
   * Create a new StaticApplicationContext.
   *
   * @see #registerSingleton
   * @see #registerPrototype
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public StaticApplicationContext() throws BeansException {
    this(null);
  }

  /**
   * Create a new StaticApplicationContext with the given parent.
   *
   * @see #registerSingleton
   * @see #registerPrototype
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public StaticApplicationContext(@Nullable ApplicationContext parent) throws BeansException {
    super(parent);

    // Initialize and register a StaticMessageSource.
    this.staticMessageSource = new StaticMessageSource();
    getBeanFactory().registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.staticMessageSource);
  }

  /**
   * Overridden to turn it into a no-op, to be more lenient towards test cases.
   */
  @Override
  protected void assertBeanFactoryActive() { }

  /**
   * Return the internal StaticMessageSource used by this context.
   * Can be used to register messages on it.
   *
   * @see #addMessage
   */
  public final StaticMessageSource getStaticMessageSource() {
    return this.staticMessageSource;
  }

  /**
   * Associate the given message with the given code.
   *
   * @param code lookup code
   * @param locale the locale message should be found within
   * @param defaultMessage message associated with this lookup code
   * @see #getStaticMessageSource
   */
  public void addMessage(String code, Locale locale, String defaultMessage) {
    getStaticMessageSource().addMessage(code, locale, defaultMessage);
  }

}
