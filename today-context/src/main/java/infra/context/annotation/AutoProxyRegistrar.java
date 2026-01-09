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

package infra.context.annotation;

import java.util.Set;

import infra.aop.config.AopConfigUtils;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.cache.annotation.EnableCaching;
import infra.context.BootstrapContext;
import infra.core.annotation.AnnotationAttributes;
import infra.core.type.AnnotationMetadata;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Registers an auto proxy creator against the current {@link BeanDefinitionRegistry}
 * as appropriate based on an {@code @Enable*} annotation having {@code mode} and
 * {@code proxyTargetClass} attributes set to the correct values.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableCaching
 * @see infra.transaction.annotation.EnableTransactionManagement
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
      if (mode != null
              && proxyTargetClass != null
              && AdviceMode.class == mode.getClass()
              && Boolean.class == proxyTargetClass.getClass()) {
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
