/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.transaction.config;

import org.w3c.dom.Element;

import cn.taketoday.aop.config.AopNamespaceUtils;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.parsing.CompositeComponentDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.xml.BeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.event.TransactionalEventListenerFactory;
import cn.taketoday.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import cn.taketoday.transaction.interceptor.TransactionInterceptor;
import cn.taketoday.util.ClassUtils;

/**
 * {@link cn.taketoday.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} implementation that allows users to easily configure
 * all the infrastructure beans required to enable annotation-driven transaction
 * demarcation.
 *
 * <p>By default, all proxies are created as JDK proxies. This may cause some
 * problems if you are injecting objects as concrete classes rather than
 * interfaces. To overcome this restriction you can set the
 * '{@code proxy-target-class}' attribute to '{@code true}', which
 * will result in class-based proxies being created.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {

  /**
   * Parses the {@code <tx:annotation-driven/>} tag. Will
   * {@link AopNamespaceUtils#registerAutoProxyCreatorIfNecessary register an AutoProxyCreator}
   * with the container as necessary.
   */
  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    registerTransactionalEventListenerFactory(parserContext);
    String mode = element.getAttribute("mode");
    if ("aspectj".equals(mode)) {
      // mode="aspectj"
      registerTransactionAspect(element, parserContext);
      if (ClassUtils.isPresent("jakarta.transaction.Transactional", getClass().getClassLoader())) {
        registerJtaTransactionAspect(element, parserContext);
      }
    }
    else {
      // mode="proxy"
      AopAutoProxyConfigurer.configureAutoProxyCreator(element, parserContext);
    }
    return null;
  }

  private void registerTransactionAspect(Element element, ParserContext parserContext) {
    String txAspectBeanName = TransactionManagementConfigUtils.TRANSACTION_ASPECT_BEAN_NAME;
    String txAspectClassName = TransactionManagementConfigUtils.TRANSACTION_ASPECT_CLASS_NAME;
    if (!parserContext.getRegistry().containsBeanDefinition(txAspectBeanName)) {
      RootBeanDefinition def = new RootBeanDefinition();
      def.setBeanClassName(txAspectClassName);
      def.setFactoryMethodName("aspectOf");
      def.setEnableDependencyInjection(false);
      registerTransactionManager(element, def);
      parserContext.registerBeanComponent(new BeanComponentDefinition(def, txAspectBeanName));
    }
  }

  private void registerJtaTransactionAspect(Element element, ParserContext parserContext) {
    String txAspectBeanName = TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_BEAN_NAME;
    String txAspectClassName = TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_CLASS_NAME;
    if (!parserContext.getRegistry().containsBeanDefinition(txAspectBeanName)) {
      RootBeanDefinition def = new RootBeanDefinition();
      def.setBeanClassName(txAspectClassName);
      def.setFactoryMethodName("aspectOf");
      def.setEnableDependencyInjection(false);
      registerTransactionManager(element, def);
      parserContext.registerBeanComponent(new BeanComponentDefinition(def, txAspectBeanName));
    }
  }

  private static void registerTransactionManager(Element element, BeanDefinition def) {
    def.getPropertyValues().add("transactionManagerBeanName",
            TxNamespaceHandler.getTransactionManagerName(element));
  }

  private void registerTransactionalEventListenerFactory(ParserContext parserContext) {
    RootBeanDefinition def = new RootBeanDefinition();
    def.setBeanClass(TransactionalEventListenerFactory.class);
    def.setEnableDependencyInjection(false);
    parserContext.registerBeanComponent(new BeanComponentDefinition(def,
            TransactionManagementConfigUtils.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME));
  }

  /**
   * Inner class to just introduce an AOP framework dependency when actually in proxy mode.
   */
  private static class AopAutoProxyConfigurer {

    public static void configureAutoProxyCreator(Element element, ParserContext parserContext) {
      AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);

      String txAdvisorBeanName = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME;
      if (!parserContext.getRegistry().containsBeanDefinition(txAdvisorBeanName)) {
        Object eleSource = parserContext.extractSource(element);

        // Create the TransactionAttributeSource definition.
        RootBeanDefinition sourceDef = new RootBeanDefinition(
                "cn.taketoday.transaction.annotation.AnnotationTransactionAttributeSource");
        sourceDef.setSource(eleSource);
        sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        sourceDef.setEnableDependencyInjection(false);
        String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

        // Create the TransactionInterceptor definition.
        RootBeanDefinition interceptorDef = new RootBeanDefinition(TransactionInterceptor.class);
        interceptorDef.setSource(eleSource);
        interceptorDef.setEnableDependencyInjection(false);
        interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registerTransactionManager(element, interceptorDef);
        interceptorDef.getPropertyValues().add("transactionAttributeSource", new RuntimeBeanReference(sourceName));
        String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

        // Create the TransactionAttributeSourceAdvisor definition.
        RootBeanDefinition advisorDef = new RootBeanDefinition(BeanFactoryTransactionAttributeSourceAdvisor.class);
        advisorDef.setSource(eleSource);
        advisorDef.setEnableDependencyInjection(false);
        advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        advisorDef.getPropertyValues().add("transactionAttributeSource", new RuntimeBeanReference(sourceName));
        advisorDef.getPropertyValues().add("adviceBeanName", interceptorName);
        if (element.hasAttribute("order")) {
          advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
        }
        parserContext.getRegistry().registerBeanDefinition(txAdvisorBeanName, advisorDef);

        CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), eleSource);
        compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
        compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
        compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, txAdvisorBeanName));
        parserContext.registerComponent(compositeDef);
      }
    }
  }

}
