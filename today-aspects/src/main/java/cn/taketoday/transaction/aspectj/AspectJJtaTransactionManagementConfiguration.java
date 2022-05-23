/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.transaction.aspectj;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.lang.Component;
import cn.taketoday.transaction.annotation.EnableTransactionManagement;
import cn.taketoday.transaction.annotation.TransactionManagementConfigurationSelector;
import cn.taketoday.transaction.config.TransactionManagementConfigUtils;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans necessary
 * to enable AspectJ-based annotation-driven transaction management for the JTA 1.2
 * {@link jakarta.transaction.Transactional} annotation in addition to Framework's own
 * {@link cn.taketoday.transaction.annotation.Transactional} annotation.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableTransactionManagement
 * @see TransactionManagementConfigurationSelector
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJJtaTransactionManagementConfiguration extends AspectJTransactionManagementConfiguration {

  @Component(TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public JtaAnnotationTransactionAspect jtaTransactionAspect() {
    JtaAnnotationTransactionAspect txAspect = JtaAnnotationTransactionAspect.aspectOf();
    if (this.txManager != null) {
      txAspect.setTransactionManager(this.txManager);
    }
    return txAspect;
  }

}
