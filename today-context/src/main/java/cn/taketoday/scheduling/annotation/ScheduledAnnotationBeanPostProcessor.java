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

package cn.taketoday.scheduling.annotation;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.taketoday.aop.AopInfrastructureBean;
import cn.taketoday.aop.framework.AopProxyUtils;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DestructionAwareBeanPostProcessor;
import cn.taketoday.beans.factory.config.NamedBeanHolder;
import cn.taketoday.beans.factory.support.MergedBeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.context.expression.EmbeddedValueResolverAware;
import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.config.CronTask;
import cn.taketoday.scheduling.config.FixedDelayTask;
import cn.taketoday.scheduling.config.FixedRateTask;
import cn.taketoday.scheduling.config.ScheduledTask;
import cn.taketoday.scheduling.config.ScheduledTaskHolder;
import cn.taketoday.scheduling.config.ScheduledTaskRegistrar;
import cn.taketoday.scheduling.support.CronTrigger;
import cn.taketoday.scheduling.support.ScheduledMethodRunnable;
import cn.taketoday.util.StringUtils;

/**
 * Bean post-processor that registers methods annotated with
 * {@link Scheduled @Scheduled} to be invoked by a
 * {@link TaskScheduler} according to the
 * "fixedRate", "fixedDelay", or "cron" expression provided via the annotation.
 *
 * <p>This post-processor is automatically registered by
 * {@code <task:annotation-driven>} XML element, and also by the
 * {@link EnableScheduling @EnableScheduling} annotation.
 *
 * <p>Autodetects any {@link SchedulingConfigurer} instances in the container,
 * allowing for customization of the scheduler to be used or for fine-grained
 * control over task registration (e.g. registration of {@link Trigger} tasks).
 * See the {@link EnableScheduling @EnableScheduling} javadocs for complete usage
 * details.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Elizabeth Chatman
 * @author Victor Brown
 * @author Sam Brannen
 * @see Scheduled
 * @see EnableScheduling
 * @see SchedulingConfigurer
 * @see TaskScheduler
 * @see ScheduledTaskRegistrar
 * @see AsyncAnnotationBeanPostProcessor
 * @since 4.0
 */
public class ScheduledAnnotationBeanPostProcessor implements ScheduledTaskHolder, Ordered,
        DestructionAwareBeanPostProcessor, InitializationBeanPostProcessor, BeanNameAware,
        DisposableBean, BeanFactoryAware, ApplicationContextAware, MergedBeanDefinitionPostProcessor,
        EmbeddedValueResolverAware, SmartInitializingSingleton, ApplicationListener<ContextRefreshedEvent> {

  /**
   * The default name of the {@link cn.taketoday.scheduling.TaskScheduler} bean to pick up: {@value}.
   * <p>Note that the initial lookup happens by type; this is just the fallback
   * in case of multiple scheduler beans found in the context.
   */
  public static final String DEFAULT_TASK_SCHEDULER_BEAN_NAME = "taskScheduler";

  private static final Logger log = LoggerFactory.getLogger(ScheduledAnnotationBeanPostProcessor.class);

  private final ScheduledTaskRegistrar registrar;

  @Nullable
  private Object scheduler;

  @Nullable
  private StringValueResolver embeddedValueResolver;

  @Nullable
  private String beanName;

  @Nullable
  private BeanFactory beanFactory;

  @Nullable
  private ApplicationContext applicationContext;

  private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

  private final IdentityHashMap<Object, Set<ScheduledTask>> scheduledTasks = new IdentityHashMap<>(16);

  /**
   * Create a default {@code ScheduledAnnotationBeanPostProcessor}.
   */
  public ScheduledAnnotationBeanPostProcessor() {
    this.registrar = new ScheduledTaskRegistrar();
  }

  /**
   * Create a {@code ScheduledAnnotationBeanPostProcessor} delegating to the
   * specified {@link cn.taketoday.scheduling.config.ScheduledTaskRegistrar}.
   *
   * @param registrar the ScheduledTaskRegistrar to register {@code @Scheduled}
   * tasks on
   */
  public ScheduledAnnotationBeanPostProcessor(ScheduledTaskRegistrar registrar) {
    Assert.notNull(registrar, "ScheduledTaskRegistrar is required");
    this.registrar = registrar;
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }

  /**
   * Set the {@link cn.taketoday.scheduling.TaskScheduler} that will invoke
   * the scheduled methods, or a {@link java.util.concurrent.ScheduledExecutorService}
   * to be wrapped as a TaskScheduler.
   * <p>If not specified, default scheduler resolution will apply: searching for a
   * unique {@link cn.taketoday.scheduling.TaskScheduler} bean in the context, or for a {@link cn.taketoday.scheduling.TaskScheduler}
   * bean named "taskScheduler" otherwise; the same lookup will also be performed for
   * a {@link java.util.concurrent.ScheduledExecutorService} bean. If neither of the two is resolvable,
   * a local single-threaded default scheduler will be created within the registrar.
   *
   * @see #DEFAULT_TASK_SCHEDULER_BEAN_NAME
   */
  public void setScheduler(Object scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
  }

  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  /**
   * Making a {@link BeanFactory} available is optional; if not set,
   * {@link cn.taketoday.scheduling.annotation.SchedulingConfigurer} beans won't get autodetected and
   * a {@link #setScheduler scheduler} has to be explicitly configured.
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  /**
   * Setting an {@link ApplicationContext} is optional: If set, registered
   * tasks will be activated in the {@link cn.taketoday.context.event.ContextRefreshedEvent} phase;
   * if not set, it will happen at {@link #afterSingletonsInstantiated} time.
   */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    if (this.beanFactory == null) {
      this.beanFactory = applicationContext;
    }
  }

  @Override
  public void afterSingletonsInstantiated() {
    // Remove resolved singleton classes from cache
    this.nonAnnotatedClasses.clear();

    if (this.applicationContext == null) {
      // Not running in an ApplicationContext -> register tasks early...
      finishRegistration();
    }
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (event.getSource() == this.applicationContext) {
      // Running in an ApplicationContext -> register tasks this late...
      // giving other ContextRefreshedEvent listeners a chance to perform
      // their work at the same time (e.g. Framework Batch's job registration).
      finishRegistration();
    }
  }

  private void finishRegistration() {
    if (this.scheduler != null) {
      this.registrar.setScheduler(this.scheduler);
    }

    if (this.beanFactory != null) {
      Map<String, SchedulingConfigurer> beans = this.beanFactory.getBeansOfType(SchedulingConfigurer.class);
      List<SchedulingConfigurer> configurers = new ArrayList<>(beans.values());
      AnnotationAwareOrderComparator.sort(configurers);
      for (SchedulingConfigurer configurer : configurers) {
        configurer.configureTasks(this.registrar);
      }
    }

    if (this.registrar.hasTasks() && this.registrar.getScheduler() == null) {
      Assert.state(this.beanFactory != null, "BeanFactory must be set to find scheduler by type");
      try {
        // Search for TaskScheduler bean...
        this.registrar.setTaskScheduler(resolveSchedulerBean(this.beanFactory, TaskScheduler.class, false));
      }
      catch (NoUniqueBeanDefinitionException ex) {
        if (log.isTraceEnabled()) {
          log.trace("Could not find unique TaskScheduler bean - attempting to resolve by name: {}",
                  ex.getMessage());
        }
        try {
          this.registrar.setTaskScheduler(resolveSchedulerBean(this.beanFactory, TaskScheduler.class, true));
        }
        catch (NoSuchBeanDefinitionException ex2) {
          if (log.isInfoEnabled()) {
            log.info("More than one TaskScheduler bean exists within the context, and " +
                    "none is named 'taskScheduler'. Mark one of them as primary or name it 'taskScheduler' " +
                    "(possibly as an alias); or implement the SchedulingConfigurer interface and call " +
                    "ScheduledTaskRegistrar#setScheduler explicitly within the configureTasks() callback: " +
                    ex.getBeanNamesFound());
          }
        }
      }
      catch (NoSuchBeanDefinitionException ex) {
        if (log.isTraceEnabled()) {
          log.trace("Could not find default TaskScheduler bean - attempting to find ScheduledExecutorService: {}",
                  ex.getMessage());
        }
        // Search for ScheduledExecutorService bean next...
        try {
          this.registrar.setScheduler(resolveSchedulerBean(this.beanFactory, ScheduledExecutorService.class, false));
        }
        catch (NoUniqueBeanDefinitionException ex2) {
          if (log.isTraceEnabled()) {
            log.trace("Could not find unique ScheduledExecutorService bean - attempting to resolve by name: {}",
                    ex2.getMessage());
          }
          try {
            this.registrar.setScheduler(resolveSchedulerBean(this.beanFactory, ScheduledExecutorService.class, true));
          }
          catch (NoSuchBeanDefinitionException ex3) {
            if (log.isInfoEnabled()) {
              log.info("More than one ScheduledExecutorService bean exists within the context, and " +
                      "none is named 'taskScheduler'. Mark one of them as primary or name it 'taskScheduler' " +
                      "(possibly as an alias); or implement the SchedulingConfigurer interface and call " +
                      "ScheduledTaskRegistrar#setScheduler explicitly within the configureTasks() callback: " +
                      ex2.getBeanNamesFound());
            }
          }
        }
        catch (NoSuchBeanDefinitionException ex2) {
          if (log.isTraceEnabled()) {
            log.trace("Could not find default ScheduledExecutorService bean - falling back to default: " +
                    ex2.getMessage());
          }
          // Giving up -> falling back to default scheduler within the registrar...
          log.info("No TaskScheduler/ScheduledExecutorService bean found for scheduled processing");
        }
      }
    }

    this.registrar.afterPropertiesSet();
  }

  private <T> T resolveSchedulerBean(BeanFactory beanFactory, Class<T> schedulerType, boolean byName) {
    if (byName) {
      T scheduler = beanFactory.getBean(DEFAULT_TASK_SCHEDULER_BEAN_NAME, schedulerType);
      if (beanName != null && beanFactory instanceof ConfigurableBeanFactory cbf) {
        cbf.registerDependentBean(DEFAULT_TASK_SCHEDULER_BEAN_NAME, beanName);
      }
      return scheduler;
    }
    else if (beanFactory instanceof AutowireCapableBeanFactory abf) {
      NamedBeanHolder<T> holder = abf.resolveNamedBean(schedulerType);
      if (beanName != null && beanFactory instanceof ConfigurableBeanFactory cbf) {
        cbf.registerDependentBean(holder.getBeanName(), beanName);
      }
      return holder.getBeanInstance();
    }
    else {
      return beanFactory.getBean(schedulerType);
    }
  }

  @Override
  public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Object bean, String beanName) {

  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean instanceof AopInfrastructureBean
            || bean instanceof TaskScheduler
            || bean instanceof ScheduledExecutorService) {
      // Ignore AOP infrastructure such as scoped proxies.
      return bean;
    }

    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
    if (!this.nonAnnotatedClasses.contains(targetClass)
            && AnnotationUtils.isCandidateClass(targetClass, Arrays.asList(Scheduled.class, Schedules.class))) {
      Map<Method, Set<Scheduled>> annotatedMethods = MethodIntrospector.selectMethods(
              targetClass, method -> {
                Set<Scheduled> scheduledAnnotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(
                        method, Scheduled.class, Schedules.class);
                return (!scheduledAnnotations.isEmpty() ? scheduledAnnotations : null);
              });
      if (annotatedMethods.isEmpty()) {
        this.nonAnnotatedClasses.add(targetClass);
        if (log.isTraceEnabled()) {
          log.trace("No @Scheduled annotations found on bean class: {}", targetClass);
        }
      }
      else {
        // Non-empty set of methods
        for (Map.Entry<Method, Set<Scheduled>> entry : annotatedMethods.entrySet()) {
          Method method = entry.getKey();
          for (Scheduled scheduled : entry.getValue()) {
            processScheduled(scheduled, method, bean);
          }
        }
        if (log.isTraceEnabled()) {
          log.trace("{} @Scheduled methods processed on bean '{}': {}",
                  annotatedMethods.size(), beanName, annotatedMethods);
        }
      }
    }
    return bean;
  }

  /**
   * Process the given {@code @Scheduled} method declaration on the given bean.
   *
   * @param scheduled the {@code @Scheduled} annotation
   * @param method the method that the annotation has been declared on
   * @param bean the target bean instance
   * @see #createRunnable(Object, Method)
   */
  protected void processScheduled(Scheduled scheduled, Method method, Object bean) {
    try {
      Runnable runnable = createRunnable(bean, method);
      boolean processedSchedule = false;
      String errorMessage = "Exactly one of the 'cron', 'fixedDelay(String)', or 'fixedRate(String)' attributes is required";

      LinkedHashSet<ScheduledTask> tasks = new LinkedHashSet<>(4);

      // Determine initial delay
      Duration initialDelay = toDuration(scheduled.initialDelay(), scheduled.timeUnit());
      String initialDelayString = scheduled.initialDelayString();
      if (StringUtils.hasText(initialDelayString)) {
        Assert.isTrue(initialDelay.isNegative(), "Specify 'initialDelay' or 'initialDelayString', not both");
        if (this.embeddedValueResolver != null) {
          initialDelayString = this.embeddedValueResolver.resolveStringValue(initialDelayString);
        }
        if (StringUtils.isNotEmpty(initialDelayString)) {
          try {
            initialDelay = toDuration(initialDelayString, scheduled.timeUnit());
          }
          catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Invalid initialDelayString value \"" + initialDelayString + "\" - cannot parse into long");
          }
        }
      }

      // Check cron expression
      String cron = scheduled.cron();
      if (StringUtils.hasText(cron)) {
        String zone = scheduled.zone();
        if (this.embeddedValueResolver != null) {
          cron = this.embeddedValueResolver.resolveStringValue(cron);
          zone = this.embeddedValueResolver.resolveStringValue(zone);
        }
        if (StringUtils.isNotEmpty(cron)) {
          Assert.isTrue(initialDelay.isNegative(), "'initialDelay' not supported for cron triggers");
          processedSchedule = true;
          if (!Scheduled.CRON_DISABLED.equals(cron)) {
            TimeZone timeZone;
            if (StringUtils.hasText(zone)) {
              timeZone = StringUtils.parseTimeZoneString(zone);
            }
            else {
              timeZone = TimeZone.getDefault();
            }
            tasks.add(this.registrar.scheduleCronTask(new CronTask(runnable, new CronTrigger(cron, timeZone))));
          }
        }
      }

      // At this point we don't need to differentiate between initial delay set or not anymore
      if (initialDelay.isNegative()) {
        initialDelay = Duration.ZERO;
      }

      // Check fixed delay
      Duration fixedDelay = toDuration(scheduled.fixedDelay(), scheduled.timeUnit());
      if (!fixedDelay.isNegative()) {
        Assert.isTrue(!processedSchedule, errorMessage);
        processedSchedule = true;
        tasks.add(this.registrar.scheduleFixedDelayTask(new FixedDelayTask(runnable, fixedDelay, initialDelay)));
      }

      String fixedDelayString = scheduled.fixedDelayString();
      if (StringUtils.hasText(fixedDelayString)) {
        if (this.embeddedValueResolver != null) {
          fixedDelayString = this.embeddedValueResolver.resolveStringValue(fixedDelayString);
        }
        if (StringUtils.isNotEmpty(fixedDelayString)) {
          Assert.isTrue(!processedSchedule, errorMessage);
          processedSchedule = true;
          try {
            fixedDelay = toDuration(fixedDelayString, scheduled.timeUnit());
          }
          catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Invalid fixedDelayString value \"" + fixedDelayString + "\" - cannot parse into long");
          }
          tasks.add(this.registrar.scheduleFixedDelayTask(new FixedDelayTask(runnable, fixedDelay, initialDelay)));
        }
      }

      // Check fixed rate
      Duration fixedRate = toDuration(scheduled.fixedRate(), scheduled.timeUnit());
      if (!fixedRate.isNegative()) {
        Assert.isTrue(!processedSchedule, errorMessage);
        processedSchedule = true;
        tasks.add(this.registrar.scheduleFixedRateTask(new FixedRateTask(runnable, fixedRate, initialDelay)));
      }
      String fixedRateString = scheduled.fixedRateString();
      if (StringUtils.hasText(fixedRateString)) {
        if (this.embeddedValueResolver != null) {
          fixedRateString = this.embeddedValueResolver.resolveStringValue(fixedRateString);
        }
        if (StringUtils.isNotEmpty(fixedRateString)) {
          Assert.isTrue(!processedSchedule, errorMessage);
          processedSchedule = true;
          try {
            fixedRate = toDuration(fixedRateString, scheduled.timeUnit());
          }
          catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Invalid fixedRateString value \"" + fixedRateString + "\" - cannot parse into long");
          }
          tasks.add(this.registrar.scheduleFixedRateTask(new FixedRateTask(runnable, fixedRate, initialDelay)));
        }
      }

      // Check whether we had any attribute set
      Assert.isTrue(processedSchedule, errorMessage);

      // Finally register the scheduled tasks
      synchronized(this.scheduledTasks) {
        Set<ScheduledTask> regTasks = this.scheduledTasks.computeIfAbsent(bean, key -> new LinkedHashSet<>(4));
        regTasks.addAll(tasks);
      }
    }
    catch (IllegalArgumentException ex) {
      throw new IllegalStateException(
              "Encountered invalid @Scheduled method '" + method.getName() + "': " + ex.getMessage());
    }
  }

  /**
   * Create a {@link java.lang.Runnable} for the given bean instance,
   * calling the specified scheduled method.
   * <p>The default implementation creates a {@link cn.taketoday.scheduling.support.ScheduledMethodRunnable}.
   *
   * @param target the target bean instance
   * @param method the scheduled method to call
   * @see cn.taketoday.scheduling.support.ScheduledMethodRunnable#ScheduledMethodRunnable(Object, Method)
   */
  protected Runnable createRunnable(Object target, Method method) {
    Assert.isTrue(method.getParameterCount() == 0, "Only no-arg methods may be annotated with @Scheduled");
    Method invocableMethod = AopUtils.selectInvocableMethod(method, target.getClass());
    return new ScheduledMethodRunnable(target, invocableMethod);
  }

  private static Duration toDuration(long value, TimeUnit timeUnit) {
    return Duration.of(value, timeUnit.toChronoUnit());
  }

  private static Duration toDuration(String value, TimeUnit timeUnit) {
    if (isDurationString(value)) {
      return Duration.parse(value);
    }
    return toDuration(Long.parseLong(value), timeUnit);
  }

  private static boolean isDurationString(String value) {
    return (value.length() > 1 && (isP(value.charAt(0)) || isP(value.charAt(1))));
  }

  private static boolean isP(char ch) {
    return (ch == 'P' || ch == 'p');
  }

  /**
   * Return all currently scheduled tasks, from {@link cn.taketoday.scheduling.annotation.Scheduled} methods
   * as well as from programmatic {@link cn.taketoday.scheduling.annotation.SchedulingConfigurer} interaction.
   */
  @Override
  public Set<ScheduledTask> getScheduledTasks() {
    Set<ScheduledTask> result = new LinkedHashSet<>();
    synchronized(this.scheduledTasks) {
      Collection<Set<ScheduledTask>> allTasks = this.scheduledTasks.values();
      for (Set<ScheduledTask> tasks : allTasks) {
        result.addAll(tasks);
      }
    }
    result.addAll(this.registrar.getScheduledTasks());
    return result;
  }

  @Override
  public void postProcessBeforeDestruction(Object bean, String beanName) {
    Set<ScheduledTask> tasks;
    synchronized(this.scheduledTasks) {
      tasks = this.scheduledTasks.remove(bean);
    }
    if (tasks != null) {
      for (ScheduledTask task : tasks) {
        task.cancel();
      }
    }
  }

  @Override
  public boolean requiresDestruction(Object bean) {
    synchronized(this.scheduledTasks) {
      return this.scheduledTasks.containsKey(bean);
    }
  }

  @Override
  public void destroy() {
    synchronized(this.scheduledTasks) {
      Collection<Set<ScheduledTask>> allTasks = this.scheduledTasks.values();
      for (Set<ScheduledTask> tasks : allTasks) {
        for (ScheduledTask task : tasks) {
          task.cancel();
        }
      }
      this.scheduledTasks.clear();
    }
    this.registrar.destroy();
  }

}
