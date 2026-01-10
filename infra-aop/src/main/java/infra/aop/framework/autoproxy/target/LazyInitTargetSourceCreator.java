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

package infra.aop.framework.autoproxy.target;

import org.jspecify.annotations.Nullable;

import infra.aop.target.AbstractBeanFactoryTargetSource;
import infra.aop.target.LazyInitTargetSource;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;

/**
 * {@code TargetSourceCreator} that enforces a {@link LazyInitTargetSource} for
 * each bean that is defined as "lazy-init". This will lead to a proxy created for
 * each of those beans, allowing to fetch a reference to such a bean without
 * actually initializing the target bean instance.
 *
 * <p>To be registered as custom {@code TargetSourceCreator} for an auto-proxy
 * creator, in combination with custom interceptors for specific beans or for the
 * creation of lazy-init proxies only. For example, as an autodetected
 * infrastructure bean in an XML application context definition:
 *
 * <pre class="code">
 * &lt;bean class="infra.aop.proxy.BeanNameAutoProxyCreator"&gt;
 *   &lt;property name="beanNames" value="*" /&gt; &lt;!-- apply to all beans --&gt;
 *   &lt;property name="customTargetSourceCreators"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="infra.aop.proxy.target.LazyInitTargetSourceCreator" /&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myLazyInitBean" class="mypackage.MyBeanClass" lazy-init="true"&gt;
 *   &lt;!-- ... --&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanDefinition#isLazyInit
 * @since 4.0 2021/12/13 22:22
 */
public class LazyInitTargetSourceCreator extends AbstractBeanFactoryTargetSourceCreator {

  @Override
  protected boolean isPrototypeBased() {
    return false;
  }

  @Override
  @Nullable
  protected AbstractBeanFactoryTargetSource createBeanFactoryTargetSource(
          Class<?> beanClass, String beanName) {
    if (getBeanFactory() instanceof ConfigurableBeanFactory configurable) {
      if (configurable.getBeanDefinition(beanName).isLazyInit()) {
        return new LazyInitTargetSource();
      }
    }
    return null;
  }

}
