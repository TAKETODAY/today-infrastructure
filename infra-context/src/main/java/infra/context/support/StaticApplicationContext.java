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

package infra.context.support;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import infra.beans.BeansException;
import infra.context.ApplicationContext;

/**
 * {@link infra.context.ApplicationContext} implementation
 * which supports programmatic registration of beans and messages,
 * rather than reading bean definitions from external configuration sources.
 * Mainly useful for testing.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
