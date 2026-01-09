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

package infra.test.context.support;

import infra.test.context.SmartContextLoader;

/**
 * {@code DelegatingSmartContextLoader} is a concrete implementation of
 * {@link AbstractDelegatingSmartContextLoader} that delegates to a
 * {@link GenericXmlContextLoader} {@link AnnotationConfigContextLoader}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SmartContextLoader
 * @see AbstractDelegatingSmartContextLoader
 * @see GenericXmlContextLoader
 * @see AnnotationConfigContextLoader
 * @since 4.0
 */
public class DelegatingSmartContextLoader extends AbstractDelegatingSmartContextLoader {

  private final SmartContextLoader xmlLoader;
  private final SmartContextLoader annotationConfigLoader;

  public DelegatingSmartContextLoader() {
    this.xmlLoader = new GenericXmlContextLoader();
    this.annotationConfigLoader = new AnnotationConfigContextLoader();
  }

  @Override
  protected SmartContextLoader getXmlLoader() {
    return this.xmlLoader;
  }

  @Override
  protected SmartContextLoader getAnnotationConfigLoader() {
    return this.annotationConfigLoader;
  }

}
