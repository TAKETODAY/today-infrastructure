/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.config.ScheduledTaskRegistrar;

/**
 * Enables  scheduled task execution capability, similar to
 * functionality found in  {@code <task:*>} XML namespace. To be used
 * on {@link Configuration @Configuration} classes as follows:
 *
 * <pre>{@code
 * @Configuration
 * @EnableScheduling
 * public class AppConfig {
 *
 *     // various @Bean definitions
 * }
 * }</pre>
 *
 * <p>This enables detection of {@link Scheduled @Scheduled} annotations on any
 * Framework-managed bean in the container. For example, given a class {@code MyTask}:
 *
 * <pre>{@code
 * package com.myco.tasks;
 *
 * public class MyTask {
 *
 *     @Scheduled(fixedRate=1000)
 *     public void work() {
 *         // task execution logic
 *     }
 * }}</pre>
 *
 * <p>the following configuration would ensure that {@code MyTask.work()} is called
 * once every 1000 ms:
 *
 * <pre>{@code
 * @Configuration
 * @EnableScheduling
 * public class AppConfig {
 *
 *     @Bean
 *     public MyTask task() {
 *         return new MyTask();
 *     }
 * }}</pre>
 *
 * <p>Alternatively, if {@code MyTask} were annotated with {@code @Component}, the
 * following configuration would ensure that its {@code @Scheduled} method is
 * invoked at the desired interval:
 *
 * <pre>{@code
 * @Configuration
 * @EnableScheduling
 * @ComponentScan(basePackages="com.myco.tasks")
 * public class AppConfig {
 *
 * }
 * }</pre>
 *
 * <p>Methods annotated with {@code @Scheduled} may even be declared directly within
 * {@code @Configuration} classes:
 *
 * <pre>{@code
 * @Configuration
 * @EnableScheduling
 * public class AppConfig {
 *
 *     @Scheduled(fixedRate=1000)
 *     public void work() {
 *         // task execution logic
 *     }
 * }
 * }</pre>
 *
 * <p>By default, Framework will search for an associated scheduler definition: either
 * a unique {@link cn.taketoday.scheduling.TaskScheduler} bean in the context,
 * or a {@code TaskScheduler} bean named "taskScheduler" otherwise; the same lookup
 * will also be performed for a {@link java.util.concurrent.ScheduledExecutorService}
 * bean. If neither of the two is resolvable, a local single-threaded default
 * scheduler will be created and used within the registrar.
 *
 * <p>When more control is desired, a {@code @Configuration} class may implement
 * {@link SchedulingConfigurer}. This allows access to the underlying
 * {@link ScheduledTaskRegistrar} instance. For example, the following example
 * demonstrates how to customize the {@link Executor} used to execute scheduled
 * tasks:
 *
 * <pre>{@code
 * @Configuration
 * @EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 *
 *     @Override
 *     public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 *         taskRegistrar.setScheduler(taskExecutor());
 *     }
 *
 *     @Bean(destroyMethod="shutdown")
 *     public Executor taskExecutor() {
 *         return Executors.newScheduledThreadPool(100);
 *     }
 * }
 * }</pre>
 *
 * <p>Note in the example above the use of {@code @Component(destroyMethod="shutdown")}.
 * This ensures that the task executor is properly shut down when the Framework
 * application context itself is closed.
 *
 * <p>Implementing {@code SchedulingConfigurer} also allows for fine-grained
 * control over task registration via the {@code ScheduledTaskRegistrar}.
 * For example, the following configures the execution of a particular bean
 * method per a custom {@code Trigger} implementation:
 *
 * <pre>{@code
 * @Configuration
 * @EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 *
 *     @Override
 *     public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 *         taskRegistrar.setScheduler(taskScheduler());
 *         taskRegistrar.addTriggerTask(
 *             () -> myTask().work(),
 *             new CustomTrigger()
 *         );
 *     }
 *
 *     @Bean(destroyMethod="shutdown")
 *     public Executor taskScheduler() {
 *         return Executors.newScheduledThreadPool(42);
 *     }
 *
 *     @Bean
 *     public MyTask myTask() {
 *         return new MyTask();
 *     }
 * }}</pre>
 *
 * <p>For reference, the example above can be compared to the following Framework XML
 * configuration:
 *
 * <pre>{@code
 * <beans>
 *
 *     <task:annotation-driven scheduler="taskScheduler"/>
 *
 *     <task:scheduler id="taskScheduler" pool-size="42"/>
 *
 *     <task:scheduled-tasks scheduler="taskScheduler">
 *         <task:scheduled ref="myTask" method="work" fixed-rate="1000"/>
 *     </task:scheduled-tasks>
 *
 *     <bean id="myTask" class="com.foo.MyTask"/>
 *
 * </beans>
 * }</pre>
 *
 * <p>The examples are equivalent save that in XML a <em>fixed-rate</em> period is used
 * instead of a custom <em>{@code Trigger}</em> implementation; this is because the
 * {@code task:} namespace {@code scheduled} cannot easily expose such support. This is
 * but one demonstration how the code-based approach allows for maximum configurability
 * through direct access to the actual component.
 *
 * <p><b>Note: {@code @EnableScheduling} applies to its local application context only,
 * allowing for selective scheduling of beans at different levels.</b> Please redeclare
 * {@code @EnableScheduling} in each individual context, e.g. the common root web
 * application context and any separate {@code DispatcherServlet} application contexts,
 * if you need to apply its behavior at multiple levels.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Scheduled
 * @see SchedulingConfiguration
 * @see SchedulingConfigurer
 * @see ScheduledTaskRegistrar
 * @see Trigger
 * @see ScheduledAnnotationBeanPostProcessor
 * @since 4.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SchedulingConfiguration.class)
public @interface EnableScheduling {

}
