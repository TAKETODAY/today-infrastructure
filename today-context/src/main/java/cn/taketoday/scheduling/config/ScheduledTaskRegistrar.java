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

package cn.taketoday.scheduling.config;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.concurrent.ConcurrentTaskScheduler;
import cn.taketoday.scheduling.support.CronTrigger;
import cn.taketoday.util.CollectionUtils;

/**
 * Helper bean for registering tasks with a {@link TaskScheduler}, typically using cron
 * expressions.
 *
 * <p>{@code ScheduledTaskRegistrar} has a more prominent user-facing
 * role when used in conjunction with the {@link
 * cn.taketoday.scheduling.annotation.EnableAsync @EnableAsync} annotation and its
 * {@link cn.taketoday.scheduling.annotation.SchedulingConfigurer
 * SchedulingConfigurer} callback interface.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Tobias Montagna-Hay
 * @author Sam Brannen
 * @see cn.taketoday.scheduling.annotation.EnableAsync
 * @see cn.taketoday.scheduling.annotation.SchedulingConfigurer
 * @since 4.0
 */
public class ScheduledTaskRegistrar implements ScheduledTaskHolder, InitializingBean, DisposableBean {

  /**
   * A special cron expression value that indicates a disabled trigger: {@value}.
   * <p>This is primarily meant for use with {@link #addCronTask(Runnable, String)}
   * when the value for the supplied {@code expression} is retrieved from an
   * external source &mdash; for example, from a property in the
   * {@link cn.taketoday.core.env.Environment Environment}.
   *
   * @see cn.taketoday.scheduling.annotation.Scheduled#CRON_DISABLED
   */
  public static final String CRON_DISABLED = "-";

  @Nullable
  private TaskScheduler taskScheduler;

  @Nullable
  private ScheduledExecutorService localExecutor;

  @Nullable
  private List<TriggerTask> triggerTasks;

  @Nullable
  private List<CronTask> cronTasks;

  @Nullable
  private List<IntervalTask> fixedRateTasks;

  @Nullable
  private List<IntervalTask> fixedDelayTasks;

  private final HashMap<Task, ScheduledTask> unresolvedTasks = new HashMap<>(16);
  private final LinkedHashSet<ScheduledTask> scheduledTasks = new LinkedHashSet<>(16);

  /**
   * Set the {@link TaskScheduler} to register scheduled tasks with.
   */
  public void setTaskScheduler(TaskScheduler taskScheduler) {
    Assert.notNull(taskScheduler, "TaskScheduler must not be null");
    this.taskScheduler = taskScheduler;
  }

  /**
   * Set the {@link TaskScheduler} to register scheduled tasks with, or a
   * {@link ScheduledExecutorService} to be wrapped as a
   * {@code TaskScheduler}.
   */
  public void setScheduler(@Nullable Object scheduler) {
    if (scheduler == null) {
      this.taskScheduler = null;
    }
    else if (scheduler instanceof TaskScheduler) {
      this.taskScheduler = (TaskScheduler) scheduler;
    }
    else if (scheduler instanceof ScheduledExecutorService) {
      this.taskScheduler = new ConcurrentTaskScheduler(((ScheduledExecutorService) scheduler));
    }
    else {
      throw new IllegalArgumentException("Unsupported scheduler type: " + scheduler.getClass());
    }
  }

  /**
   * Return the {@link TaskScheduler} instance for this registrar (may be {@code null}).
   */
  @Nullable
  public TaskScheduler getScheduler() {
    return this.taskScheduler;
  }

  /**
   * Specify triggered tasks as a Map of Runnables (the tasks) and Trigger objects
   * (typically custom implementations of the {@link Trigger} interface).
   */
  public void setTriggerTasks(Map<Runnable, Trigger> triggerTasks) {
    this.triggerTasks = new ArrayList<>();
    for (Map.Entry<Runnable, Trigger> entry : triggerTasks.entrySet()) {
      addTriggerTask(new TriggerTask(entry.getKey(), entry.getValue()));
    }
  }

  /**
   * Specify triggered tasks as a list of {@link TriggerTask} objects. Primarily used
   * by {@code <task:*>} namespace parsing.
   */
  public void setTriggerTasksList(List<TriggerTask> triggerTasks) {
    this.triggerTasks = triggerTasks;
  }

  /**
   * Get the trigger tasks as an unmodifiable list of {@link TriggerTask} objects.
   *
   * @return the list of tasks (never {@code null})
   */
  public List<TriggerTask> getTriggerTaskList() {
    return this.triggerTasks != null
           ? Collections.unmodifiableList(this.triggerTasks) : Collections.emptyList();
  }

  /**
   * Specify triggered tasks as a Map of Runnables (the tasks) and cron expressions.
   *
   * @see CronTrigger
   */
  public void setCronTasks(Map<Runnable, String> cronTasks) {
    this.cronTasks = new ArrayList<>();
    for (Map.Entry<Runnable, String> entry : cronTasks.entrySet()) {
      addCronTask(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Specify triggered tasks as a list of {@link CronTask} objects. Primarily used by
   * {@code <task:*>} namespace parsing.
   */
  public void setCronTasksList(List<CronTask> cronTasks) {
    this.cronTasks = cronTasks;
  }

  /**
   * Get the cron tasks as an unmodifiable list of {@link CronTask} objects.
   *
   * @return the list of tasks (never {@code null})
   */
  public List<CronTask> getCronTaskList() {
    return this.cronTasks != null
           ? Collections.unmodifiableList(this.cronTasks) : Collections.emptyList();
  }

  /**
   * Specify triggered tasks as a Map of Runnables (the tasks) and fixed-rate values.
   *
   * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
   */
  public void setFixedRateTasks(Map<Runnable, Long> fixedRateTasks) {
    this.fixedRateTasks = new ArrayList<>();
    for (Map.Entry<Runnable, Long> entry : fixedRateTasks.entrySet()) {
      addFixedRateTask(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Specify fixed-rate tasks as a list of {@link IntervalTask} objects. Primarily used
   * by {@code <task:*>} namespace parsing.
   */
  public void setFixedRateTasksList(List<IntervalTask> fixedRateTasks) {
    this.fixedRateTasks = fixedRateTasks;
  }

  /**
   * Get the fixed-rate tasks as an unmodifiable list of {@link IntervalTask} objects.
   *
   * @return the list of tasks (never {@code null})
   */
  public List<IntervalTask> getFixedRateTaskList() {
    return this.fixedRateTasks != null
           ? Collections.unmodifiableList(this.fixedRateTasks) : Collections.emptyList();
  }

  /**
   * Specify triggered tasks as a Map of Runnables (the tasks) and fixed-delay values.
   *
   * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
   */
  public void setFixedDelayTasks(Map<Runnable, Long> fixedDelayTasks) {
    this.fixedDelayTasks = new ArrayList<>();
    for (Map.Entry<Runnable, Long> entry : fixedDelayTasks.entrySet()) {
      addFixedDelayTask(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Specify fixed-delay tasks as a list of {@link IntervalTask} objects. Primarily used
   * by {@code <task:*>} namespace parsing.
   */
  public void setFixedDelayTasksList(List<IntervalTask> fixedDelayTasks) {
    this.fixedDelayTasks = fixedDelayTasks;
  }

  /**
   * Get the fixed-delay tasks as an unmodifiable list of {@link IntervalTask} objects.
   *
   * @return the list of tasks (never {@code null})
   */
  public List<IntervalTask> getFixedDelayTaskList() {
    return this.fixedDelayTasks != null
           ? Collections.unmodifiableList(this.fixedDelayTasks) : Collections.emptyList();
  }

  /**
   * Add a Runnable task to be triggered per the given {@link Trigger}.
   *
   * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
   */
  public void addTriggerTask(Runnable task, Trigger trigger) {
    addTriggerTask(new TriggerTask(task, trigger));
  }

  /**
   * Add a {@code TriggerTask}.
   *
   * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
   */
  public void addTriggerTask(TriggerTask task) {
    if (this.triggerTasks == null) {
      this.triggerTasks = new ArrayList<>();
    }
    this.triggerTasks.add(task);
  }

  /**
   * Add a {@link Runnable} task to be triggered per the given cron {@code expression}.
   * <p>this method will not register the task if the {@code expression} is equal to {@link #CRON_DISABLED}.
   */
  public void addCronTask(Runnable task, String expression) {
    if (!CRON_DISABLED.equals(expression)) {
      addCronTask(new CronTask(task, expression));
    }
  }

  /**
   * Add a {@link CronTask}.
   */
  public void addCronTask(CronTask task) {
    if (this.cronTasks == null) {
      this.cronTasks = new ArrayList<>();
    }
    this.cronTasks.add(task);
  }

  /**
   * Add a {@code Runnable} task to be triggered at the given fixed-rate interval.
   *
   * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
   */
  public void addFixedRateTask(Runnable task, long interval) {
    addFixedRateTask(new IntervalTask(task, interval, 0));
  }

  /**
   * Add a {@code Runnable} task to be triggered at the given fixed-rate interval.
   *
   * @see TaskScheduler#scheduleAtFixedRate(Runnable, Duration)
   */
  public void addFixedRateTask(Runnable task, Duration interval) {
    addFixedRateTask(new IntervalTask(task, interval));
  }

  /**
   * Add a fixed-rate {@link IntervalTask}.
   *
   * @see TaskScheduler#scheduleAtFixedRate(Runnable, long)
   */
  public void addFixedRateTask(IntervalTask task) {
    if (this.fixedRateTasks == null) {
      this.fixedRateTasks = new ArrayList<>();
    }
    this.fixedRateTasks.add(task);
  }

  /**
   * Add a Runnable task to be triggered with the given fixed delay.
   *
   * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
   */
  public void addFixedDelayTask(Runnable task, long delay) {
    addFixedDelayTask(new IntervalTask(task, delay, 0));
  }

  /**
   * Add a Runnable task to be triggered with the given fixed delay.
   *
   * @see TaskScheduler#scheduleWithFixedDelay(Runnable, Duration)
   */
  public void addFixedDelayTask(Runnable task, Duration delay) {
    addFixedDelayTask(new IntervalTask(task, delay));
  }

  /**
   * Add a fixed-delay {@link IntervalTask}.
   *
   * @see TaskScheduler#scheduleWithFixedDelay(Runnable, long)
   */
  public void addFixedDelayTask(IntervalTask task) {
    if (this.fixedDelayTasks == null) {
      this.fixedDelayTasks = new ArrayList<>();
    }
    this.fixedDelayTasks.add(task);
  }

  /**
   * Return whether this {@code ScheduledTaskRegistrar} has any tasks registered.
   */
  public boolean hasTasks() {
    return CollectionUtils.isNotEmpty(this.triggerTasks)
            || CollectionUtils.isNotEmpty(this.cronTasks)
            || CollectionUtils.isNotEmpty(this.fixedRateTasks)
            || CollectionUtils.isNotEmpty(this.fixedDelayTasks);
  }

  /**
   * Calls {@link #scheduleTasks()} at bean construction time.
   */
  @Override
  public void afterPropertiesSet() {
    scheduleTasks();
  }

  /**
   * Schedule all registered tasks against the underlying
   * {@linkplain #setTaskScheduler(TaskScheduler) task scheduler}.
   */
  protected void scheduleTasks() {
    if (this.taskScheduler == null) {
      this.localExecutor = Executors.newSingleThreadScheduledExecutor();
      this.taskScheduler = new ConcurrentTaskScheduler(this.localExecutor);
    }
    if (this.triggerTasks != null) {
      for (TriggerTask task : this.triggerTasks) {
        addScheduledTask(scheduleTriggerTask(task));
      }
    }
    if (this.cronTasks != null) {
      for (CronTask task : this.cronTasks) {
        addScheduledTask(scheduleCronTask(task));
      }
    }
    if (this.fixedRateTasks != null) {
      for (IntervalTask task : this.fixedRateTasks) {
        if (task instanceof FixedRateTask fixedRateTask) {
          addScheduledTask(scheduleFixedRateTask(fixedRateTask));
        }
        else {
          addScheduledTask(scheduleFixedRateTask(new FixedRateTask(task)));
        }
      }
    }
    if (this.fixedDelayTasks != null) {
      for (IntervalTask task : this.fixedDelayTasks) {
        if (task instanceof FixedDelayTask fixedDelayTask) {
          addScheduledTask(scheduleFixedDelayTask(fixedDelayTask));
        }
        else {
          addScheduledTask(scheduleFixedDelayTask(new FixedDelayTask(task)));
        }
      }
    }
  }

  private void addScheduledTask(@Nullable ScheduledTask task) {
    if (task != null) {
      this.scheduledTasks.add(task);
    }
  }

  /**
   * Schedule the specified trigger task, either right away if possible
   * or on initialization of the scheduler.
   *
   * @return a handle to the scheduled task, allowing to cancel it
   */
  @Nullable
  public ScheduledTask scheduleTriggerTask(TriggerTask task) {
    ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
    boolean newTask = false;
    if (scheduledTask == null) {
      scheduledTask = new ScheduledTask(task);
      newTask = true;
    }
    if (this.taskScheduler != null) {
      scheduledTask.future = this.taskScheduler.schedule(task.getRunnable(), task.getTrigger());
    }
    else {
      addTriggerTask(task);
      this.unresolvedTasks.put(task, scheduledTask);
    }
    return newTask ? scheduledTask : null;
  }

  /**
   * Schedule the specified cron task, either right away if possible
   * or on initialization of the scheduler.
   *
   * @return a handle to the scheduled task, allowing to cancel it
   * (or {@code null} if processing a previously registered task)
   */
  @Nullable
  public ScheduledTask scheduleCronTask(CronTask task) {
    ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
    boolean newTask = false;
    if (scheduledTask == null) {
      scheduledTask = new ScheduledTask(task);
      newTask = true;
    }
    if (this.taskScheduler != null) {
      scheduledTask.future = this.taskScheduler.schedule(task.getRunnable(), task.getTrigger());
    }
    else {
      addCronTask(task);
      this.unresolvedTasks.put(task, scheduledTask);
    }
    return newTask ? scheduledTask : null;
  }

  /**
   * Schedule the specified fixed-rate task, either right away if possible
   * or on initialization of the scheduler.
   *
   * @return a handle to the scheduled task, allowing to cancel it
   * (or {@code null} if processing a previously registered task)
   */
  @Nullable
  public ScheduledTask scheduleFixedRateTask(FixedRateTask task) {
    ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
    boolean newTask = false;
    if (scheduledTask == null) {
      scheduledTask = new ScheduledTask(task);
      newTask = true;
    }
    if (this.taskScheduler != null) {
      Duration initialDelay = task.getInitialDelayDuration();
      if (initialDelay.toMillis() > 0) {
        Instant startTime = taskScheduler.getClock().instant().plus(initialDelay);
        scheduledTask.future =
                this.taskScheduler.scheduleAtFixedRate(task.getRunnable(), startTime, task.getIntervalDuration());
      }
      else {
        scheduledTask.future =
                taskScheduler.scheduleAtFixedRate(task.getRunnable(), task.getIntervalDuration());
      }
    }
    else {
      addFixedRateTask(task);
      this.unresolvedTasks.put(task, scheduledTask);
    }
    return newTask ? scheduledTask : null;
  }

  /**
   * Schedule the specified fixed-delay task, either right away if possible
   * or on initialization of the scheduler.
   *
   * @return a handle to the scheduled task, allowing to cancel it
   * (or {@code null} if processing a previously registered task)
   */
  @Nullable
  public ScheduledTask scheduleFixedDelayTask(FixedDelayTask task) {
    ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
    boolean newTask = false;
    if (scheduledTask == null) {
      scheduledTask = new ScheduledTask(task);
      newTask = true;
    }
    if (this.taskScheduler != null) {
      Duration initialDelay = task.getInitialDelayDuration();
      if (!initialDelay.isNegative()) {
        Instant startTime = taskScheduler.getClock().instant().plus(task.getInitialDelayDuration());
        scheduledTask.future =
                taskScheduler.scheduleWithFixedDelay(task.getRunnable(), startTime, task.getIntervalDuration());
      }
      else {
        scheduledTask.future =
                taskScheduler.scheduleWithFixedDelay(task.getRunnable(), task.getIntervalDuration());
      }
    }
    else {
      addFixedDelayTask(task);
      this.unresolvedTasks.put(task, scheduledTask);
    }
    return newTask ? scheduledTask : null;
  }

  /**
   * Return all locally registered tasks that have been scheduled by this registrar.
   *
   * @see #addTriggerTask
   * @see #addCronTask
   * @see #addFixedRateTask
   * @see #addFixedDelayTask
   */
  @Override
  public Set<ScheduledTask> getScheduledTasks() {
    return Collections.unmodifiableSet(this.scheduledTasks);
  }

  @Override
  public void destroy() {
    for (ScheduledTask task : this.scheduledTasks) {
      task.cancel();
    }
    if (this.localExecutor != null) {
      this.localExecutor.shutdownNow();
    }
  }

}
