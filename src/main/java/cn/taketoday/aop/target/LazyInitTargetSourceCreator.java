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

package cn.taketoday.aop.target;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.lang.Nullable;

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
 * &lt;bean class="cn.taketoday.aop.proxy.BeanNameAutoProxyCreator"&gt;
 *   &lt;property name="beanNames" value="*" /&gt; &lt;!-- apply to all beans --&gt;
 *   &lt;property name="customTargetSourceCreators"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="cn.taketoday.aop.proxy.target.LazyInitTargetSourceCreator" /&gt;
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
