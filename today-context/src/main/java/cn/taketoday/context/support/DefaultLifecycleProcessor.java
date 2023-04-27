/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.Lifecycle;
import cn.taketoday.context.LifecycleProcessor;
import cn.taketoday.context.Phased;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Default implementation of the {@link LifecycleProcessor} strategy.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {
  private static final Logger log = LoggerFactory.getLogger(DefaultLifecycleProcessor.class);

  private volatile long timeoutPerShutdownPhase = 30000;
  private volatile boolean running;

  @Nullable
  private volatile ConfigurableBeanFactory beanFactory;

  /**
   * Specify the maximum time allotted in milliseconds for the shutdown of
   * any phase (group of SmartLifecycle beans with the same 'phase' value).
   * <p>The default value is 30 seconds.
   */
  public void setTimeoutPerShutdownPhase(long timeoutPerShutdownPhase) {
    this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (!(beanFactory instanceof ConfigurableBeanFactory)) {
      throw new IllegalArgumentException(
              "DefaultLifecycleProcessor requires a ConfigurableBeanFactory: " + beanFactory);
    }
    this.beanFactory = (ConfigurableBeanFactory) beanFactory;
  }

  private ConfigurableBeanFactory getBeanFactory() {
    ConfigurableBeanFactory beanFactory = this.beanFactory;
    Assert.state(beanFactory != null, "No BeanFactory available");
    return beanFactory;
  }

  // Lifecycle implementation

  /**
   * Start all registered beans that implement {@link Lifecycle} and are <i>not</i>
   * already running. Any bean that implements {@link SmartLifecycle} will be
   * started within its 'phase', and all phases will be ordered from lowest to
   * highest value. All beans that do not implement {@link SmartLifecycle} will be
   * started in the default phase 0. A bean declared as a dependency of another bean
   * will be started before the dependent bean regardless of the declared phase.
   */
  @Override
  public void start() {
    startBeans(false);
    this.running = true;
  }

  /**
   * Stop all registered beans that implement {@link Lifecycle} and <i>are</i>
   * currently running. Any bean that implements {@link SmartLifecycle} will be
   * stopped within its 'phase', and all phases will be ordered from highest to
   * lowest value. All beans that do not implement {@link SmartLifecycle} will be
   * stopped in the default phase 0. A bean declared as dependent on another bean
   * will be stopped before the dependency bean regardless of the declared phase.
   */
  @Override
  public void stop() {
    stopBeans();
    this.running = false;
  }

  @Override
  public void onRefresh() {
    startBeans(true);
    this.running = true;
  }

  @Override
  public void onClose() {
    stopBeans();
    this.running = false;
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

  // Internal helpers

  private void startBeans(boolean autoStartupOnly) {
    Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
    TreeMap<Integer, LifecycleGroup> phases = new TreeMap<>();
    for (Map.Entry<String, Lifecycle> entry : lifecycleBeans.entrySet()) {
      String beanName = entry.getKey();
      Lifecycle bean = entry.getValue();
      if (!autoStartupOnly || (bean instanceof SmartLifecycle smartLifecycle && smartLifecycle.isAutoStartup())) {
        int phase = getPhase(bean);
        LifecycleGroup lifecycleGroup = phases.get(phase);
        if (lifecycleGroup == null) {
          lifecycleGroup = new LifecycleGroup(phase, this.timeoutPerShutdownPhase, lifecycleBeans, autoStartupOnly);
          phases.put(phase, lifecycleGroup);
        }
        lifecycleGroup.add(beanName, bean);
      }
    }

    if (!phases.isEmpty()) {
      for (LifecycleGroup group : phases.values()) {
        group.start();
      }
    }
  }

  /**
   * Start the specified bean as part of the given set of Lifecycle beans,
   * making sure that any beans that it depends on are started first.
   *
   * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
   * @param beanName the name of the bean to start
   */
  private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName, boolean autoStartupOnly) {
    Lifecycle bean = lifecycleBeans.remove(beanName);
    if (bean != null && bean != this) {
      String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);
      for (String dependency : dependenciesForBean) {
        doStart(lifecycleBeans, dependency, autoStartupOnly);
      }

      if (!bean.isRunning()
              && (!autoStartupOnly || !(bean instanceof SmartLifecycle smartLifecycle) || smartLifecycle.isAutoStartup())) {
        if (log.isTraceEnabled()) {
          log.trace("Starting bean '{}' of type [{}]", beanName, bean.getClass().getName());
        }
        try {
          bean.start();
        }
        catch (Throwable ex) {
          throw new ApplicationContextException("Failed to start bean '" + beanName + "'", ex);
        }
        log.debug("Successfully started bean '{}'", beanName);
      }
    }
  }

  private void stopBeans() {
    Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
    TreeMap<Integer, LifecycleGroup> phases = new TreeMap<>(Comparator.reverseOrder());
    for (Map.Entry<String, Lifecycle> entry : lifecycleBeans.entrySet()) {
      String beanName = entry.getKey();
      Lifecycle bean = entry.getValue();
      int shutdownPhase = getPhase(bean);
      LifecycleGroup group = phases.get(shutdownPhase);
      if (group == null) {
        group = new LifecycleGroup(shutdownPhase,
                timeoutPerShutdownPhase, lifecycleBeans, false);
        phases.put(shutdownPhase, group);
      }
      group.add(beanName, bean);
    }
    if (!phases.isEmpty()) {
      for (LifecycleGroup group : phases.values()) {
        group.stop();
      }
    }
  }

  /**
   * Stop the specified bean as part of the given set of Lifecycle beans,
   * making sure that any beans that depends on it are stopped first.
   *
   * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
   * @param beanName the name of the bean to stop
   */
  private void doStop(
          Map<String, ? extends Lifecycle> lifecycleBeans,
          String beanName, CountDownLatch latch, Set<String> countDownBeanNames) {
    Lifecycle bean = lifecycleBeans.remove(beanName);
    if (bean != null) {
      String[] dependentBeans = getBeanFactory().getDependentBeans(beanName);
      for (String dependentBean : dependentBeans) {
        doStop(lifecycleBeans, dependentBean, latch, countDownBeanNames);
      }

      try {
        if (bean.isRunning()) {
          if (bean instanceof SmartLifecycle smartLifecycle) {
            if (log.isTraceEnabled()) {
              log.trace("Asking bean '{}' of type [{}] to stop", beanName, bean.getClass().getName());
            }
            countDownBeanNames.add(beanName);
            smartLifecycle.stop(() -> {
              latch.countDown();
              countDownBeanNames.remove(beanName);
              log.debug("Bean '{}' completed its stop procedure", beanName);
            });
          }
          else {
            if (log.isTraceEnabled()) {
              log.trace("Stopping bean '{}' of type [{}]", beanName, bean.getClass().getName());
            }
            bean.stop();
            log.debug("Successfully stopped bean '{}'", beanName);
          }
        }
        else if (bean instanceof SmartLifecycle) {
          // Don't wait for beans that aren't running...
          latch.countDown();
        }
      }
      catch (Throwable ex) {
        log.warn("Failed to stop bean '{}'", beanName, ex);
      }
    }
  }

  // overridable hooks

  /**
   * Retrieve all applicable Lifecycle beans: all singletons that have already been created,
   * as well as all SmartLifecycle beans (even if they are marked as lazy-init).
   *
   * @return the Map of applicable beans, with bean names as keys and bean instances as values
   */
  protected Map<String, Lifecycle> getLifecycleBeans() {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    LinkedHashMap<String, Lifecycle> beans = new LinkedHashMap<>();
    Set<String> beanNames = beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
    for (String beanName : beanNames) {
      String beanNameToRegister = BeanFactoryUtils.transformedBeanName(beanName);
      boolean isFactoryBean = beanFactory.isFactoryBean(beanNameToRegister);
      String beanNameToCheck = (isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
      if ((beanFactory.containsSingleton(beanNameToRegister) &&
              (!isFactoryBean || matchesBeanType(Lifecycle.class, beanNameToCheck, beanFactory))) ||
              matchesBeanType(SmartLifecycle.class, beanNameToCheck, beanFactory)) {
        Object bean = beanFactory.getBean(beanNameToCheck);
        if (bean != this && bean instanceof Lifecycle) {
          beans.put(beanNameToRegister, (Lifecycle) bean);
        }
      }
    }
    return beans;
  }

  private boolean matchesBeanType(Class<?> targetType, String beanName, BeanFactory beanFactory) {
    Class<?> beanType = beanFactory.getType(beanName);
    return beanType != null && targetType.isAssignableFrom(beanType);
  }

  /**
   * Determine the lifecycle phase of the given bean.
   * <p>The default implementation checks for the {@link Phased} interface, using
   * a default of 0 otherwise. Can be overridden to apply other/further policies.
   *
   * @param bean the bean to introspect
   * @return the phase (an integer value)
   * @see Phased#getPhase()
   * @see SmartLifecycle
   */
  protected int getPhase(Lifecycle bean) {
    return bean instanceof Phased ? ((Phased) bean).getPhase() : 0;
  }

  /**
   * Helper class for maintaining a group of Lifecycle beans that should be started
   * and stopped together based on their 'phase' value (or the default value of 0).
   * The group is expected to be created in an ad-hoc fashion and group members are
   * expected to always have the same 'phase' value.
   */
  private class LifecycleGroup {

    private final int phase;
    private final long timeout;
    private int smartMemberCount;
    private final boolean autoStartupOnly;

    private final Map<String, ? extends Lifecycle> lifecycleBeans;
    private final ArrayList<LifecycleGroupMember> members = new ArrayList<>();

    public LifecycleGroup(int phase, long timeout,
            Map<String, ? extends Lifecycle> lifecycleBeans, boolean autoStartupOnly) {

      this.phase = phase;
      this.timeout = timeout;
      this.lifecycleBeans = lifecycleBeans;
      this.autoStartupOnly = autoStartupOnly;
    }

    public void add(String name, Lifecycle bean) {
      this.members.add(new LifecycleGroupMember(name, bean));
      if (bean instanceof SmartLifecycle) {
        this.smartMemberCount++;
      }
    }

    public void start() {
      if (members.isEmpty()) {
        return;
      }
      log.debug("Starting beans in phase {}", phase);
      for (LifecycleGroupMember member : members) {
        doStart(lifecycleBeans, member.name, autoStartupOnly);
      }
    }

    public void stop() {
      if (members.isEmpty()) {
        return;
      }
      log.debug("Stopping beans in phase {}", phase);

      CountDownLatch latch = new CountDownLatch(smartMemberCount);
      Set<String> countDownBeanNames = Collections.synchronizedSet(new LinkedHashSet<>());
      Set<String> lifecycleBeanNames = new HashSet<>(lifecycleBeans.keySet());
      for (LifecycleGroupMember member : members) {
        if (lifecycleBeanNames.contains(member.name)) {
          doStop(lifecycleBeans, member.name, latch, countDownBeanNames);
        }
        else if (member.bean instanceof SmartLifecycle) {
          // Already removed: must have been a dependent bean from another phase
          latch.countDown();
        }
      }
      try {
        latch.await(timeout, TimeUnit.MILLISECONDS);
        if (latch.getCount() > 0 && !countDownBeanNames.isEmpty() && log.isInfoEnabled()) {
          log.info("Failed to shut down {} bean {} with phase value {} within timeout of {}ms: {}",
                  countDownBeanNames.size(), (countDownBeanNames.size() > 1 ? "s" : ""), phase, timeout, countDownBeanNames);
        }
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * A simple record of a LifecycleGroup member.
   */
  private record LifecycleGroupMember(String name, Lifecycle bean) {

  }

}
