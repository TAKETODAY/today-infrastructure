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

package cn.taketoday.context.annotation;

import java.util.Set;

import cn.taketoday.aop.config.AopConfigUtils;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Registers an auto proxy creator against the current {@link BeanDefinitionRegistry}
 * as appropriate based on an {@code @Enable*} annotation having {@code mode} and
 * {@code proxyTargetClass} attributes set to the correct values.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableCaching
 * @see cn.taketoday.transaction.annotation.EnableTransactionManagement
 * @since 4.0 2022/1/11 23:38
 */
public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {
  private static final Logger log = LoggerFactory.getLogger(AutoProxyRegistrar.class);

  /**
   * Register, escalate, and configure the standard auto proxy creator (APC) against the
   * given registry. Works by finding the nearest annotation declared on the importing
   * {@code @Configuration} class that has both {@code mode} and {@code proxyTargetClass}
   * attributes. If {@code mode} is set to {@code PROXY}, the APC is registered; if
   * {@code proxyTargetClass} is set to {@code true}, then the APC is forced to use
   * subclass (CGLIB) proxying.
   * <p>Several {@code @Enable*} annotations expose both {@code mode} and
   * {@code proxyTargetClass} attributes. It is important to note that most of these
   * capabilities end up sharing a {@linkplain AopConfigUtils#AUTO_PROXY_CREATOR_BEAN_NAME
   * single APC}. For this reason, this implementation doesn't "care" exactly which
   * annotation it finds -- as long as it exposes the right {@code mode} and
   * {@code proxyTargetClass} attributes, the APC can be registered and configured all
   * the same.
   */
  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BootstrapContext context) {
    boolean candidateFound = false;
    Set<String> annTypes = importingClassMetadata.getAnnotationTypes();
    for (String annType : annTypes) {
      AnnotationAttributes candidate = AnnotationAttributes.fromMetadata(importingClassMetadata, annType);
      if (candidate == null) {
        continue;
      }
      Object mode = candidate.get("mode");
      Object proxyTargetClass = candidate.get("proxyTargetClass");
      if (mode != null && proxyTargetClass != null && AdviceMode.class == mode.getClass() &&
              Boolean.class == proxyTargetClass.getClass()) {
        candidateFound = true;
        if (mode == AdviceMode.PROXY) {
          AopConfigUtils.registerAutoProxyCreatorIfNecessary(context.getRegistry());
          if ((Boolean) proxyTargetClass) {
            AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(context.getRegistry());
            return;
          }
        }
      }
    }
    if (!candidateFound && log.isInfoEnabled()) {
      String name = getClass().getSimpleName();
      log.info(String.format("%s was imported but no annotations were found " +
              "having both 'mode' and 'proxyTargetClass' attributes of type " +
              "AdviceMode and boolean respectively. This means that auto proxy " +
              "creator registration and configuration may not have occurred as " +
              "intended, and components may not be proxied as expected. Check to " +
              "ensure that %s has been @Import'ed on the same class where these " +
              "annotations are declared; otherwise remove the import of %s " +
              "altogether.", name, name, name));
    }
  }

}
