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

package cn.taketoday.transaction.annotation;

import cn.taketoday.context.annotation.Primary;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionManager;

/**
 * Interface to be implemented by @{@link cn.taketoday.context.annotation.Configuration
 * Configuration} classes annotated with @{@link EnableTransactionManagement} that wish to
 * (or need to) explicitly specify the default {@code PlatformTransactionManager} bean
 * (or {@code ReactiveTransactionManager} bean) to be used for annotation-driven
 * transaction management, as opposed to the default approach of a by-type lookup.
 * One reason this might be necessary is if there are two {@code PlatformTransactionManager}
 * beans (or two {@code ReactiveTransactionManager} beans) present in the container.
 *
 * <p>See @{@link EnableTransactionManagement} for general examples and context;
 * see {@link #annotationDrivenTransactionManager()} for detailed instructions.
 *
 * <p>Note that in by-type lookup disambiguation cases, an alternative approach to
 * implementing this interface is to simply mark one of the offending
 * {@code PlatformTransactionManager} {@code @Bean} methods (or
 * {@code ReactiveTransactionManager} {@code @Bean} methods) as
 * {@link Primary @Primary}.
 * This is even generally preferred since it doesn't lead to early initialization
 * of the {@code TransactionManager} bean.
 *
 * @author Chris Beams
 * @see EnableTransactionManagement
 * @see Primary
 * @see PlatformTransactionManager
 * @see cn.taketoday.transaction.ReactiveTransactionManager
 * @since 4.0
 */
public interface TransactionManagementConfigurer {

  /**
   * Return the default transaction manager bean to use for annotation-driven database
   * transaction management, i.e. when processing {@code @Transactional} methods.
   * <p>There are two basic approaches to implementing this method:
   * <h3>1. Implement the method and annotate it with {@code @Bean}</h3>
   * In this case, the implementing {@code @Configuration} class implements this method,
   * marks it with {@code @Bean}, and configures and returns the transaction manager
   * directly within the method body:
   * <pre class="code">
   * &#064;Bean
   * &#064;Override
   * public PlatformTransactionManager annotationDrivenTransactionManager() {
   *     return new DataSourceTransactionManager(dataSource());
   * }</pre>
   * <h3>2. Implement the method without {@code @Bean} and delegate to another existing
   * {@code @Bean} method</h3>
   * <pre class="code">
   * &#064;Bean
   * public PlatformTransactionManager txManager() {
   *     return new DataSourceTransactionManager(dataSource());
   * }
   *
   * &#064;Override
   * public PlatformTransactionManager annotationDrivenTransactionManager() {
   *     return txManager(); // reference the existing {@code @Bean} method above
   * }</pre>
   * If taking approach #2, be sure that <em>only one</em> of the methods is marked
   * with {@code @Bean}!
   * <p>In either scenario #1 or #2, it is important that the
   * {@code PlatformTransactionManager} instance is managed as a Framework bean within the
   * container since most {@code PlatformTransactionManager} implementations take advantage
   * of Framework lifecycle callbacks such as {@code InitializingBean} and
   * {@code BeanFactoryAware}. Note that the same guidelines apply to
   * {@code ReactiveTransactionManager} beans.
   *
   * @return a {@link PlatformTransactionManager} or
   * {@link cn.taketoday.transaction.ReactiveTransactionManager} implementation
   */
  TransactionManager annotationDrivenTransactionManager();

}
