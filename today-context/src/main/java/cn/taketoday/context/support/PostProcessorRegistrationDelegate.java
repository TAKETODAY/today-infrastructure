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

package cn.taketoday.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.AbstractBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import cn.taketoday.beans.factory.support.BeanDefinitionValueResolver;
import cn.taketoday.beans.factory.support.MergedBeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.OrderComparator;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/8 14:59
 */
final class PostProcessorRegistrationDelegate {

  public static void invokeBeanFactoryPostProcessors(
          ConfigurableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

    // WARNING: Although it may appear that the body of this method can be easily
    // refactored to avoid the use of multiple loops and multiple lists, the use
    // of multiple lists and multiple passes over the names of processors is
    // intentional. We must ensure that we honor the contracts for PriorityOrdered
    // and Ordered processors. Specifically, we must NOT cause processors to be
    // instantiated (via getBean() invocations) or registered in the ApplicationContext
    // in the wrong order.

    // Invoke BeanDefinitionRegistryPostProcessors first, if any.
    HashSet<String> processedBeans = new HashSet<>();

    if (beanFactory instanceof BeanDefinitionRegistry registry) {
      ArrayList<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
      ArrayList<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

      for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
        if (postProcessor instanceof BeanDefinitionRegistryPostProcessor registryProcessor) {
          registryProcessor.postProcessBeanDefinitionRegistry(registry);
          registryProcessors.add(registryProcessor);
        }
        else {
          regularPostProcessors.add(postProcessor);
        }
      }

      // Do not initialize FactoryBeans here: We need to leave all regular beans
      // uninitialized to let the bean factory post-processors apply to them!
      // Separate between BeanDefinitionRegistryPostProcessors that implement
      // PriorityOrdered, Ordered, and the rest.
      ArrayList<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

      // First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
      Set<String> postProcessorNames =
              beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
      for (String ppName : postProcessorNames) {
        if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
          currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
          processedBeans.add(ppName);
        }
      }
      sortPostProcessors(currentRegistryProcessors, beanFactory);
      registryProcessors.addAll(currentRegistryProcessors);
      invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
      currentRegistryProcessors.clear();

      // Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
      postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
      for (String ppName : postProcessorNames) {
        if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
          currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
          processedBeans.add(ppName);
        }
      }
      sortPostProcessors(currentRegistryProcessors, beanFactory);
      registryProcessors.addAll(currentRegistryProcessors);
      invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
      currentRegistryProcessors.clear();

      // Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
      boolean reiterate = true;
      while (reiterate) {
        reiterate = false;
        postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
        for (String ppName : postProcessorNames) {
          if (!processedBeans.contains(ppName)) {
            currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
            processedBeans.add(ppName);
            reiterate = true;
          }
        }
        sortPostProcessors(currentRegistryProcessors, beanFactory);
        registryProcessors.addAll(currentRegistryProcessors);
        invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
        currentRegistryProcessors.clear();
      }

      // Now, invoke the postProcessBeanFactory callback of all processors handled so far.
      invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
      invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
    }

    else {
      // Invoke factory processors registered with the context instance.
      invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
    }

    // Do not initialize FactoryBeans here: We need to leave all regular beans
    // uninitialized to let the bean factory post-processors apply to them!
    Set<String> postProcessorNames =
            beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

    // Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
    // Ordered, and the rest.
    ArrayList<String> orderedPostProcessorNames = new ArrayList<>();
    ArrayList<String> nonOrderedPostProcessorNames = new ArrayList<>();
    ArrayList<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
    for (String ppName : postProcessorNames) {
      if (processedBeans.contains(ppName)) {
        // skip - already processed in first phase above
      }
      else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
        priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
      }
      else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
        orderedPostProcessorNames.add(ppName);
      }
      else {
        nonOrderedPostProcessorNames.add(ppName);
      }
    }

    // First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
    sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
    invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

    // Next, invoke the BeanFactoryPostProcessors that implement Ordered.
    ArrayList<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
    for (String postProcessorName : orderedPostProcessorNames) {
      orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
    }
    sortPostProcessors(orderedPostProcessors, beanFactory);
    invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

    // Finally, invoke all other BeanFactoryPostProcessors.
    ArrayList<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
    for (String postProcessorName : nonOrderedPostProcessorNames) {
      nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
    }
    invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

    // Clear cached merged bean definitions since the post-processors might have
    // modified the original metadata, e.g. replacing placeholders in values...
    beanFactory.clearMetadataCache();
  }

  public static void registerBeanPostProcessors(
          ConfigurableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

    // WARNING: Although it may appear that the body of this method can be easily
    // refactored to avoid the use of multiple loops and multiple lists, the use
    // of multiple lists and multiple passes over the names of processors is
    // intentional. We must ensure that we honor the contracts for PriorityOrdered
    // and Ordered processors. Specifically, we must NOT cause processors to be
    // instantiated (via getBean() invocations) or registered in the ApplicationContext
    // in the wrong order.

    Set<String> postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

    // Register BeanPostProcessorChecker that logs an info message when
    // a bean is created during BeanPostProcessor instantiation, i.e. when
    // a bean is not eligible for getting processed by all BeanPostProcessors.
    int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.size();
    beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

    // Separate between BeanPostProcessors that implement PriorityOrdered,
    // Ordered, and the rest.
    ArrayList<String> orderedPostProcessorNames = new ArrayList<>();
    ArrayList<String> nonOrderedPostProcessorNames = new ArrayList<>();
    ArrayList<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
    ArrayList<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
    for (String ppName : postProcessorNames) {
      if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
        BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
        priorityOrderedPostProcessors.add(pp);
        if (pp instanceof MergedBeanDefinitionPostProcessor) {
          internalPostProcessors.add(pp);
        }
      }
      else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
        orderedPostProcessorNames.add(ppName);
      }
      else {
        nonOrderedPostProcessorNames.add(ppName);
      }
    }

    // First, register the BeanPostProcessors that implement PriorityOrdered.
    sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

    // Next, register the BeanPostProcessors that implement Ordered.
    ArrayList<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
    for (String ppName : orderedPostProcessorNames) {
      BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
      orderedPostProcessors.add(pp);
      if (pp instanceof MergedBeanDefinitionPostProcessor) {
        internalPostProcessors.add(pp);
      }
    }
    sortPostProcessors(orderedPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, orderedPostProcessors);

    // Now, register all regular BeanPostProcessors.
    ArrayList<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
    for (String ppName : nonOrderedPostProcessorNames) {
      BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
      nonOrderedPostProcessors.add(pp);
      if (pp instanceof MergedBeanDefinitionPostProcessor) {
        internalPostProcessors.add(pp);
      }
    }
    registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

    // Finally, re-register all internal BeanPostProcessors.
    sortPostProcessors(internalPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, internalPostProcessors);

    // Re-register post-processor for detecting inner beans as ApplicationListeners,
    // moving it to the end of the processor chain (for picking up proxies etc).
    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
  }

  /**
   * Load and sort the post-processors of the specified type.
   *
   * @param beanFactory the bean factory to use
   * @param beanPostProcessorType the post-processor type
   * @param <T> the post-processor type
   * @return a list of sorted post-processors for the specified type
   */
  static <T extends BeanPostProcessor> List<T> loadBeanPostProcessors(
          ConfigurableBeanFactory beanFactory, Class<T> beanPostProcessorType) {

    Set<String> postProcessorNames = beanFactory.getBeanNamesForType(beanPostProcessorType, true, false);
    List<T> postProcessors = new ArrayList<>();
    for (String ppName : postProcessorNames) {
      postProcessors.add(beanFactory.getBean(ppName, beanPostProcessorType));
    }
    sortPostProcessors(postProcessors, beanFactory);
    return postProcessors;

  }

  /**
   * Selectively invoke {@link MergedBeanDefinitionPostProcessor} instances
   * registered in the specified bean factory, resolving bean definitions as
   * well as any inner bean definitions that they may contain.
   *
   * @param beanFactory the bean factory to use
   */
  static void invokeMergedBeanDefinitionPostProcessors(StandardBeanFactory beanFactory) {
    new MergedBeanDefinitionPostProcessorInvoker(beanFactory).invokeMergedBeanDefinitionPostProcessors();
  }

  private static void sortPostProcessors(List<?> postProcessors, ConfigurableBeanFactory beanFactory) {
    // Nothing to sort?
    if (postProcessors.size() <= 1) {
      return;
    }
    Comparator<Object> comparatorToUse = null;
    if (beanFactory instanceof StandardBeanFactory std) {
      comparatorToUse = std.getDependencyComparator();
    }
    if (comparatorToUse == null) {
      comparatorToUse = OrderComparator.INSTANCE;
    }
    postProcessors.sort(comparatorToUse);
  }

  /**
   * Invoke the given BeanDefinitionRegistryPostProcessor beans.
   */
  private static void invokeBeanDefinitionRegistryPostProcessors(
          Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

    for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
      postProcessor.postProcessBeanDefinitionRegistry(registry);
    }
  }

  /**
   * Invoke the given BeanFactoryPostProcessor beans.
   */
  private static void invokeBeanFactoryPostProcessors(
          Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableBeanFactory beanFactory) {

    for (BeanFactoryPostProcessor postProcessor : postProcessors) {
      postProcessor.postProcessBeanFactory(beanFactory);
    }
  }

  /**
   * Register the given BeanPostProcessor beans.
   */
  private static void registerBeanPostProcessors(
          ConfigurableBeanFactory beanFactory, List<? extends BeanPostProcessor> postProcessors) {

    if (beanFactory instanceof AbstractBeanFactory abstractBeanFactory) {
      // Bulk addition is more efficient against our list there
      abstractBeanFactory.addBeanPostProcessors(postProcessors);
    }
    else {
      for (BeanPostProcessor postProcessor : postProcessors) {
        beanFactory.addBeanPostProcessor(postProcessor);
      }
    }
  }

  /**
   * BeanPostProcessor that logs an info message when a bean is created during
   * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
   * getting processed by all BeanPostProcessors.
   */
  private static final class BeanPostProcessorChecker implements InitializationBeanPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(BeanPostProcessorChecker.class);

    private final ConfigurableBeanFactory beanFactory;

    private final int beanPostProcessorTargetCount;

    public BeanPostProcessorChecker(ConfigurableBeanFactory beanFactory, int count) {
      this.beanFactory = beanFactory;
      this.beanPostProcessorTargetCount = count;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
      if (!(bean instanceof BeanPostProcessor)
              && !isInfrastructureBean(beanName)
              && beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
        log.info("Bean '{}' of type [{}] is not eligible for getting processed by all BeanPostProcessors " +
                "(for example: not eligible for auto-proxying)", beanName, bean.getClass().getName());
      }
      return bean;
    }

    private boolean isInfrastructureBean(@Nullable String beanName) {
      if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
        BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
        return bd.getRole() == BeanDefinition.ROLE_INFRASTRUCTURE;
      }
      return false;
    }
  }

  private static final class MergedBeanDefinitionPostProcessorInvoker {

    private final StandardBeanFactory beanFactory;

    private MergedBeanDefinitionPostProcessorInvoker(StandardBeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    private void invokeMergedBeanDefinitionPostProcessors() {
      var postProcessors = PostProcessorRegistrationDelegate.loadBeanPostProcessors(
              this.beanFactory, MergedBeanDefinitionPostProcessor.class);
      for (String beanName : this.beanFactory.getBeanDefinitionNames()) {
        RootBeanDefinition bd = (RootBeanDefinition) this.beanFactory.getMergedBeanDefinition(beanName);
        Class<?> beanType = resolveBeanType(bd);
        postProcessRootBeanDefinition(postProcessors, beanName, beanType, bd);
        bd.markAsPostProcessed();
      }
      registerBeanPostProcessors(beanFactory, postProcessors);
    }

    private void postProcessRootBeanDefinition(List<MergedBeanDefinitionPostProcessor> postProcessors,
            String beanName, Class<?> beanType, RootBeanDefinition bd) {
      BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(beanFactory, beanName, bd);

      for (MergedBeanDefinitionPostProcessor postProcessor : postProcessors) {
        postProcessor.postProcessMergedBeanDefinition(bd, beanType, beanName);
      }

      if (bd.hasPropertyValues()) {
        for (PropertyValue propertyValue : bd.getPropertyValues().asList()) {
          Object value = propertyValue.getValue();
          if (value instanceof AbstractBeanDefinition innerBd) {
            Class<?> innerBeanType = resolveBeanType(innerBd);
            resolveInnerBeanDefinition(valueResolver, innerBd, (innerBeanName, innerBeanDefinition)
                    -> postProcessRootBeanDefinition(postProcessors, innerBeanName, innerBeanType, innerBeanDefinition));
          }
        }
      }

      if (bd.hasConstructorArgumentValues()) {
        for (ValueHolder valueHolder : bd.getConstructorArgumentValues().getIndexedArgumentValues().values()) {
          Object value = valueHolder.getValue();
          if (value instanceof AbstractBeanDefinition innerBd) {
            Class<?> innerBeanType = resolveBeanType(innerBd);
            resolveInnerBeanDefinition(valueResolver, innerBd, (innerBeanName, innerBeanDefinition)
                    -> postProcessRootBeanDefinition(postProcessors, innerBeanName, innerBeanType, innerBeanDefinition));
          }
        }
      }
    }

    private void resolveInnerBeanDefinition(BeanDefinitionValueResolver valueResolver,
            BeanDefinition innerBeanDefinition, BiConsumer<String, RootBeanDefinition> resolver) {

      valueResolver.resolveInnerBean(null, innerBeanDefinition, (name, rbd) -> {
        resolver.accept(name, rbd);
        return Void.class;
      });
    }

    private Class<?> resolveBeanType(AbstractBeanDefinition bd) {
      if (!bd.hasBeanClass()) {
        try {
          bd.resolveBeanClass(this.beanFactory.getBeanClassLoader());
        }
        catch (ClassNotFoundException ex) {
          // ignore
        }
      }
      return bd.getResolvableType().toClass();
    }
  }

}
