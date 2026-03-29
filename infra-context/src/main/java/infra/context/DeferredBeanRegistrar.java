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

package infra.context;

import infra.beans.factory.BeanRegistrar;
import infra.beans.factory.BeanRegistry;
import infra.core.ParameterizedTypeReference;
import infra.core.env.Environment;

/**
 * A variant of {@link BeanRegistrar} which aims to be invoked
 * at the end of the bean registration phase, coming after regular
 * bean definition reading and configuration class processing.
 *
 * <p>This allows for seeing all user-registered beans, potentially
 * reacting to their presence. The {@code containsBean} methods on
 * {@link BeanRegistry} will provide reliable answers, independent
 * of the order of user bean registration versus {@code BeanRegistrar}
 * import/registration.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see #register(BeanRegistry, Environment)
 * @see BeanRegistry#containsBean(String)
 * @see BeanRegistry#containsBean(Class)
 * @see BeanRegistry#containsBean(ParameterizedTypeReference)
 * @see infra.context.support.GenericApplicationContext#register(BeanRegistrar...)
 * @see infra.context.annotation.Import
 * @since 5.0
 */
public interface DeferredBeanRegistrar extends BeanRegistrar {

}
