/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.support;

import org.crac.CheckpointException;
import org.crac.Core;
import org.crac.RestoreException;
import org.crac.management.CRaCMXBean;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContextException;
import infra.context.Lifecycle;
import infra.context.LifecycleProcessor;
import infra.context.Phased;
import infra.context.SmartLifecycle;
import infra.core.NativeDetector;
import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.concurrent.Future;

/**
 * Default implementation of the {@link LifecycleProcessor} strategy.
 *
 * <p>Provides interaction with {@link Lifecycle} and {@link SmartLifecycle} beans in
 * groups for specific phases, on startup/shutdown as well as for explicit start/stop
 * interactions on a {@link infra.context.ConfigurableApplicationContext}.
 *
 * <p>Provides interaction with {@link Lifecycle} and {@link SmartLifecycle} beans in
 * groups for specific phases, on startup/shutdown as well as for explicit start/stop
 * interactions on a {@link infra.context.ConfigurableApplicationContext}.
 *
 * <p>this also includes support for JVM checkpoint/restore (Project CRaC)
 * when the {@code org.crac:crac} dependency on the classpath.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {

  private static final Logger log = LoggerFactory.getLogger(DefaultLifecycleProcessor.class);

  /**
   * Property name for a common context checkpoint: {@value}.
   *
   * @see #ON_REFRESH_VALUE
   * @see org.crac.Core#checkpointRestore()
   */
  public static final String CHECKPOINT_PROPERTY_NAME = "infra.context.checkpoint";

  /**
   * Property name for terminating the JVM when the context reaches a specific phase: {@value}.
   *
   * @see #ON_REFRESH_VALUE
   */
  public static final String EXIT_PROPERTY_NAME = "infra.context.exit";

  /**
   * Recognized value for the context checkpoint and exit properties: {@value}.
   *
   * @see #CHECKPOINT_PROPERTY_NAME
   * @see #EXIT_PROPERTY_NAME
   */
  public static final String ON_REFRESH_VALUE = "onRefresh";

  private static boolean checkpointOnRefresh =
          ON_REFRESH_VALUE.equalsIgnoreCase(TodayStrategies.getProperty(CHECKPOINT_PROPERTY_NAME));

  private static final boolean exitOnRefresh =
          ON_REFRESH_VALUE.equalsIgnoreCase(TodayStrategies.getProperty(EXIT_PROPERTY_NAME));

  private final ConcurrentHashMap<Integer, Long> concurrentStartupForPhases = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<Integer, Long> timeoutsForShutdownPhases = new ConcurrentHashMap<>();

  private volatile long timeoutPerShutdownPhase = 10000;

  private volatile boolean running;

  @Nullable
  private volatile ConfigurableBeanFactory beanFactory;

  @Nullable
  private volatile Set<String> stoppedBeans;

  // Just for keeping a strong reference to the registered CRaC Resource, if any

  @Nullable
  private Object cracResource;

  public DefaultLifecycleProcessor() {
    if (!NativeDetector.inNativeImage() && ClassUtils.isPresent("org.crac.Core", getClass().getClassLoader())) {
      this.cracResource = new CracDelegate().registerResource();
    }
    else if (checkpointOnRefresh) {
      throw new IllegalStateException(
              "Checkpoint on refresh requires a CRaC-enabled JVM and 'org.crac:crac' on the classpath");
    }
  }

  /**
   * Switch to concurrent startup for each given phase (group of {@link SmartLifecycle}
   * beans with the same 'phase' value) with corresponding timeouts.
   * <p><b>Note: By default, the startup for every phase will be sequential without
   * a timeout. Calling this setter with timeouts for the given phases switches to a
   * mode where the beans in these phases will be started concurrently, cancelling
   * the startup if the corresponding timeout is not met for any of these phases.</b>
   * <p>For an actual concurrent startup, a bootstrap {@code Executor} needs to be
   * set for the application context, typically through a "bootstrapExecutor" bean.
   *
   * @param phasesWithTimeouts a map of phase values (matching
   * {@link SmartLifecycle#getPhase()}) and corresponding timeout values
   * (in milliseconds)
   * @see SmartLifecycle#getPhase()
   * @see infra.beans.factory.config.ConfigurableBeanFactory#getBootstrapExecutor()
   * @since 5.0
   */
  public void setConcurrentStartupForPhases(Map<Integer, Long> phasesWithTimeouts) {
    this.concurrentStartupForPhases.putAll(phasesWithTimeouts);
  }

  /**
   * Switch to concurrent startup for a specific phase (group of {@link SmartLifecycle}
   * beans with the same 'phase' value) with a corresponding timeout.
   * <p><b>Note: By default, the startup for every phase will be sequential without
   * a timeout. Calling this setter with a timeout for the given phase switches to a
   * mode where the beans in this phase will be started concurrently, cancelling
   * the startup if the corresponding timeout is not met for this phase.</b>
   * <p>For an actual concurrent startup, a bootstrap {@code Executor} needs to be
   * set for the application context, typically through a "bootstrapExecutor" bean.
   *
   * @param phase the phase value (matching {@link SmartLifecycle#getPhase()})
   * @param timeout the corresponding timeout value (in milliseconds)
   * @see SmartLifecycle#getPhase()
   * @see infra.beans.factory.config.ConfigurableBeanFactory#getBootstrapExecutor()
   * @since 5.0
   */
  public void setConcurrentStartupForPhase(int phase, long timeout) {
    this.concurrentStartupForPhases.put(phase, timeout);
  }

  /**
   * Specify the maximum time allotted for the shutdown of each given phase
   * (group of {@link SmartLifecycle} beans with the same 'phase' value).
   * <p>In case of no specific timeout configured, the default timeout per
   * shutdown phase will apply: 10000 milliseconds (10 seconds).
   *
   * @param phasesWithTimeouts a map of phase values (matching
   * {@link SmartLifecycle#getPhase()}) and corresponding timeout values
   * (in milliseconds)
   * @see SmartLifecycle#getPhase()
   * @see #setTimeoutPerShutdownPhase
   * @since 5.0
   */
  public void setTimeoutsForShutdownPhases(Map<Integer, Long> phasesWithTimeouts) {
    this.timeoutsForShutdownPhases.putAll(phasesWithTimeouts);
  }

  /**
   * Specify the maximum time allotted for the shutdown of a specific phase
   * (group of {@link SmartLifecycle} beans with the same 'phase' value).
   * <p>In case of no specific timeout configured, the default timeout per
   * shutdown phase will apply: 10000 milliseconds (10 seconds).
   *
   * @param phase the phase value (matching {@link SmartLifecycle#getPhase()})
   * @param timeout the corresponding timeout value (in milliseconds)
   * @see SmartLifecycle#getPhase()
   * @see #setTimeoutPerShutdownPhase
   * @since 5.0
   */
  public void setTimeoutForShutdownPhase(int phase, long timeout) {
    this.timeoutsForShutdownPhases.put(phase, timeout);
  }

  /**
   * Specify the maximum time allotted in milliseconds for the shutdown of any
   * phase (group of {@link SmartLifecycle} beans with the same 'phase' value).
   * <p>The default value is 10000 milliseconds (10 seconds).
   *
   * @see SmartLifecycle#getPhase()
   */
  public void setTimeoutPerShutdownPhase(long timeoutPerShutdownPhase) {
    this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (!(beanFactory instanceof ConfigurableBeanFactory cbf)) {
      throw new IllegalArgumentException(
              "DefaultLifecycleProcessor requires a ConfigurableBeanFactory: " + beanFactory);
    }
    if (!this.concurrentStartupForPhases.isEmpty() && cbf.getBootstrapExecutor() == null) {
      throw new IllegalStateException("'bootstrapExecutor' needs to be configured for concurrent startup");
    }
    this.beanFactory = cbf;
  }

  private ConfigurableBeanFactory getBeanFactory() {
    ConfigurableBeanFactory beanFactory = this.beanFactory;
    Assert.state(beanFactory != null, "No BeanFactory available");
    return beanFactory;
  }

  private Executor getBootstrapExecutor() {
    Executor executor = getBeanFactory().getBootstrapExecutor();
    Assert.state(executor != null, "No 'bootstrapExecutor' available");
    return executor;
  }

  @Nullable
  private Long determineConcurrentStartup(int phase) {
    return this.concurrentStartupForPhases.get(phase);
  }

  private long determineShutdownTimeout(int phase) {
    Long timeout = this.timeoutsForShutdownPhases.get(phase);
    return timeout != null ? timeout : this.timeoutPerShutdownPhase;
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
    this.stoppedBeans = null;
    startBeans(false);
    // If any bean failed to explicitly start, the exception propagates here.
    // The caller may choose to subsequently call stop() if appropriate.
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
    stopBeans(false);
    this.running = false;
  }

  @Override
  public void onRefresh() {
    if (checkpointOnRefresh) {
      checkpointOnRefresh = false;
      new CracDelegate().checkpointRestore();
    }
    if (exitOnRefresh) {
      Runtime.getRuntime().halt(0);
    }

    this.stoppedBeans = null;
    try {
      startBeans(true);
    }
    catch (ApplicationContextException ex) {
      // Some bean failed to auto-start within context refresh:
      // stop already started beans on context refresh failure.
      stopBeans(false);
      throw ex;
    }
    this.running = true;
  }

  @Override
  public void onClose() {
    stopBeans(false);
    this.running = false;
  }

  @Override
  public void onRestart() {
    this.stoppedBeans = null;
    if (this.running) {
      stopBeans(true);
    }
    startBeans(true);
    this.running = true;
  }

  @Override
  public void onPause() {
    if (this.running) {
      stopBeans(true);
      this.running = false;
    }
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

  // Internal helpers

  void stopForRestart() {
    if (this.running) {
      this.stoppedBeans = ConcurrentHashMap.newKeySet();
      stopBeans(false);
      this.running = false;
    }
  }

  void restartAfterStop() {
    if (this.stoppedBeans != null) {
      startBeans(true);
      this.stoppedBeans = null;
      this.running = true;
    }
  }

  private void startBeans(boolean autoStartupOnly) {
    var lifecycleBeans = getLifecycleBeans();
    var phases = new TreeMap<Integer, LifecycleGroup>();

    for (var entry : lifecycleBeans.entrySet()) {
      String beanName = entry.getKey();
      Lifecycle bean = entry.getValue();
      if (!autoStartupOnly || isAutoStartupCandidate(beanName, bean)) {
        int startupPhase = getPhase(bean);
        phases.computeIfAbsent(startupPhase, phase -> new LifecycleGroup(phase, lifecycleBeans, autoStartupOnly, false))
                .add(beanName, bean);
      }
    }

    if (!phases.isEmpty()) {
      for (LifecycleGroup group : phases.values()) {
        group.start();
      }
    }
  }

  private boolean isAutoStartupCandidate(String beanName, Lifecycle bean) {
    Set<String> stoppedBeans = this.stoppedBeans;
    return stoppedBeans != null ? stoppedBeans.contains(beanName) :
            (bean instanceof SmartLifecycle sl && sl.isAutoStartup());
  }

  /**
   * Start the specified bean as part of the given set of Lifecycle beans,
   * making sure that any beans that it depends on are started first.
   *
   * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
   * @param beanName the name of the bean to start
   */
  private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName,
          boolean autoStartupOnly, @Nullable List<Future<?>> futures) {

    Lifecycle bean = lifecycleBeans.remove(beanName);
    if (bean != null && bean != this) {
      String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);
      for (String dependency : dependenciesForBean) {
        doStart(lifecycleBeans, dependency, autoStartupOnly, futures);
      }

      if (!bean.isRunning() && (!autoStartupOnly || toBeStarted(beanName, bean))) {
        if (futures != null) {
          futures.add(Future.run(() -> doStart(beanName, bean), getBootstrapExecutor()));
        }
        else {
          doStart(beanName, bean);
        }
      }
    }
  }

  private void doStart(String beanName, Lifecycle bean) {
    if (log.isTraceEnabled()) {
      log.trace("Starting bean '{}' of type [{}]", beanName, bean.getClass().getName());
    }
    try {
      bean.start();
    }
    catch (Throwable ex) {
      throw new ApplicationContextException("Failed to start bean '%s'".formatted(beanName), ex);
    }

    log.debug("Successfully started bean '{}'", beanName);
  }

  private boolean toBeStarted(String beanName, Lifecycle bean) {
    Set<String> stoppedBeans = this.stoppedBeans;
    return (stoppedBeans != null ? stoppedBeans.contains(beanName) :
            (!(bean instanceof SmartLifecycle smartLifecycle) || smartLifecycle.isAutoStartup()));
  }

  private void stopBeans(boolean pausableOnly) {
    Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
    Map<Integer, LifecycleGroup> phases = new TreeMap<>(Comparator.reverseOrder());

    lifecycleBeans.forEach((beanName, bean) -> {
      int shutdownPhase = getPhase(bean);
      phases.computeIfAbsent(shutdownPhase, phase -> new LifecycleGroup(phase, lifecycleBeans, false, pausableOnly))
              .add(beanName, bean);
    });

    if (!phases.isEmpty()) {
      phases.values().forEach(LifecycleGroup::stop);
    }
  }

  /**
   * Stop the specified bean as part of the given set of Lifecycle beans,
   * making sure that any beans that depends on it are stopped first.
   *
   * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
   * @param beanName the name of the bean to stop
   */
  private void doStop(Map<String, ? extends Lifecycle> lifecycleBeans, final String beanName,
          final CountDownLatch latch, final Set<String> countDownBeanNames, boolean pausableOnly) {

    Lifecycle bean = lifecycleBeans.remove(beanName);
    if (bean != null) {
      String[] dependentBeans = getBeanFactory().getDependentBeans(beanName);
      for (String dependentBean : dependentBeans) {
        doStop(lifecycleBeans, dependentBean, latch, countDownBeanNames, pausableOnly);
      }
      try {
        if (bean.isRunning()) {
          Set<String> stoppedBeans = this.stoppedBeans;
          if (stoppedBeans != null) {
            stoppedBeans.add(beanName);
          }
          if (bean instanceof SmartLifecycle smartLifecycle) {
            if (!pausableOnly || smartLifecycle.isPausable()) {
              if (log.isTraceEnabled()) {
                log.trace("Asking bean '{}' of type [{}] to stop", beanName, bean.getClass().getName());
              }
              countDownBeanNames.add(beanName);
              smartLifecycle.stop(() -> {
                latch.countDown();
                countDownBeanNames.remove(beanName);
                if (log.isDebugEnabled()) {
                  log.debug("Bean '{}' completed its stop procedure", beanName);
                }
              });
            }
            else {
              // Don't wait for beans that aren't pauseable...
              latch.countDown();
            }
          }
          else if (!pausableOnly) {
            if (log.isTraceEnabled()) {
              log.trace("Stopping bean '{}' of type [{}]", beanName, bean.getClass().getName());
            }
            bean.stop();
            if (log.isDebugEnabled()) {
              log.debug("Successfully stopped bean '{}'", beanName);
            }
          }
        }
        else if (bean instanceof SmartLifecycle) {
          // Don't wait for beans that aren't running...
          latch.countDown();
        }
      }
      catch (Throwable ex) {
        log.warn("Failed to stop bean '{}'", beanName, ex);
        if (bean instanceof SmartLifecycle) {
          latch.countDown();
        }
      }
    }
  }

  // Overridable hooks

  /**
   * Retrieve all applicable Lifecycle beans: all singletons that have already been created,
   * as well as all SmartLifecycle beans (even if they are marked as lazy-init).
   *
   * @return the Map of applicable beans, with bean names as keys and bean instances as values
   */
  protected Map<String, Lifecycle> getLifecycleBeans() {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    LinkedHashMap<String, Lifecycle> beans = new LinkedHashMap<>();
    var beanNames = beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
    for (String beanName : beanNames) {
      String beanNameToRegister = BeanFactoryUtils.transformedBeanName(beanName);
      boolean isFactoryBean = beanFactory.isFactoryBean(beanNameToRegister);
      String beanNameToCheck = (isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
      if ((beanFactory.containsSingleton(beanNameToRegister)
              && (!isFactoryBean || matchesBeanType(Lifecycle.class, beanNameToCheck, beanFactory)))
              || matchesBeanType(SmartLifecycle.class, beanNameToCheck, beanFactory)) {
        Object bean = beanFactory.getBean(beanNameToCheck);
        if (bean != this && bean instanceof Lifecycle lifecycle) {
          beans.put(beanNameToRegister, lifecycle);
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
    return bean instanceof Phased phased ? phased.getPhase() : 0;
  }

  /**
   * Helper class for maintaining a group of Lifecycle beans that should be started
   * and stopped together based on their 'phase' value (or the default value of 0).
   * The group is expected to be created in an ad-hoc fashion and group members are
   * expected to always have the same 'phase' value.
   */
  private class LifecycleGroup {

    private final int phase;

    private final Map<String, ? extends Lifecycle> lifecycleBeans;

    private final boolean autoStartupOnly;

    private final boolean pausableOnly;

    private final ArrayList<LifecycleGroupMember> members = new ArrayList<>();

    private int smartMemberCount;

    public LifecycleGroup(int phase, Map<String, ? extends Lifecycle> lifecycleBeans,
            boolean autoStartupOnly, boolean pausableOnly) {
      this.phase = phase;
      this.lifecycleBeans = lifecycleBeans;
      this.autoStartupOnly = autoStartupOnly;
      this.pausableOnly = pausableOnly;
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

      Long concurrentStartup = determineConcurrentStartup(phase);
      List<Future<?>> futures = concurrentStartup != null ? new ArrayList<>() : null;
      for (LifecycleGroupMember member : members) {
        doStart(lifecycleBeans, member.name, autoStartupOnly, futures);
      }
      if (concurrentStartup != null && CollectionUtils.isNotEmpty(futures)) {
        try {
          Future.combine(futures).asVoid().get(concurrentStartup, TimeUnit.MILLISECONDS);
        }
        catch (Exception ex) {
          if (ex instanceof ExecutionException exEx) {
            Throwable cause = exEx.getCause();
            if (cause instanceof ApplicationContextException acEx) {
              throw acEx;
            }
          }
          throw new ApplicationContextException("Failed to start beans in phase %d within timeout of %dms"
                  .formatted(this.phase, concurrentStartup), ex);
        }
      }
    }

    public void stop() {
      if (members.isEmpty()) {
        return;
      }
      log.debug("Stopping beans in phase {}", phase);

      CountDownLatch latch = new CountDownLatch(smartMemberCount);
      Set<String> countDownBeanNames = Collections.synchronizedSet(new LinkedHashSet<>());
      HashSet<String> lifecycleBeanNames = new HashSet<>(lifecycleBeans.keySet());
      for (LifecycleGroupMember member : members) {
        if (lifecycleBeanNames.contains(member.name)) {
          doStop(lifecycleBeans, member.name, latch, countDownBeanNames, pausableOnly);
        }
        else if (member.bean instanceof SmartLifecycle) {
          // Already removed: must have been a dependent bean from another phase
          latch.countDown();
        }
      }
      try {
        long shutdownTimeout = determineShutdownTimeout(this.phase);
        if (!latch.await(shutdownTimeout, TimeUnit.MILLISECONDS)) {
          // Count is still >0 after timeout
          if (!countDownBeanNames.isEmpty() && log.isInfoEnabled()) {
            log.info("Shutdown phase {} ends with {} bean%s still running after timeout of {}ms: {}",
                    this.phase, countDownBeanNames.size(), countDownBeanNames.size() > 1 ? "s" : "", shutdownTimeout, countDownBeanNames);
          }
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

  /**
   * Inner class to avoid a hard dependency on Project CRaC at runtime.
   *
   * @see org.crac.Core
   */
  private class CracDelegate {

    public Object registerResource() {
      log.debug("Registering JVM checkpoint/restore callback for Infra-managed lifecycle beans");
      CracResourceAdapter resourceAdapter = new CracResourceAdapter();
      org.crac.Core.getGlobalContext().register(resourceAdapter);
      return resourceAdapter;
    }

    public void checkpointRestore() {
      log.info("Triggering JVM checkpoint/restore");
      try {
        Core.checkpointRestore();
      }
      catch (UnsupportedOperationException ex) {
        throw new ApplicationContextException("CRaC checkpoint not supported on current JVM", ex);
      }
      catch (CheckpointException ex) {
        throw new ApplicationContextException("Failed to take CRaC checkpoint on refresh", ex);
      }
      catch (RestoreException ex) {
        throw new ApplicationContextException("Failed to restore CRaC checkpoint on refresh", ex);
      }
    }
  }

  /**
   * Resource adapter for Project CRaC, triggering a stop-and-restart cycle
   * for Infra-managed lifecycle beans around a JVM checkpoint/restore.
   *
   * @see #stopForRestart()
   * @see #restartAfterStop()
   */
  private class CracResourceAdapter implements org.crac.Resource {

    private final CyclicBarrier afterRestoreBarrier = new CyclicBarrier(2);

    private final CyclicBarrier beforeCheckpointBarrier = new CyclicBarrier(2);

    @Override
    public void beforeCheckpoint(org.crac.Context<? extends org.crac.Resource> context) {
      Thread thread = new Thread(this::preventShutdown, "prevent-shutdown");
      thread.setDaemon(false);
      thread.start();

      log.debug("Stopping Infra-managed lifecycle beans before JVM checkpoint");
      stopForRestart();
    }

    private void preventShutdown() {
      awaitBarrier(this.beforeCheckpointBarrier);
      // Checkpoint happens here
      awaitBarrier(this.afterRestoreBarrier);
    }

    @Override
    public void afterRestore(org.crac.Context<? extends org.crac.Resource> context) {
      // Unlock barrier for beforeCheckpoint
      awaitBarrier(this.beforeCheckpointBarrier);

      log.info("Restarting Infra-managed lifecycle beans after JVM restore");
      restartAfterStop();

      // Unlock barrier for afterRestore to shutdown "prevent-shutdown" thread
      awaitBarrier(this.afterRestoreBarrier);

      if (!checkpointOnRefresh) {
        log.info("Infra-managed lifecycle restart completed (restored JVM running for {} ms)",
                CRaCMXBean.getCRaCMXBean().getUptimeSinceRestore());
      }
    }

    private void awaitBarrier(CyclicBarrier barrier) {
      try {
        barrier.await();
      }
      catch (Exception ex) {
        log.trace("Exception from barrier", ex);
      }
    }
  }

}
