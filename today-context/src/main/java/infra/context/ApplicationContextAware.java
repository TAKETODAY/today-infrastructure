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
package infra.context;

import infra.beans.BeansException;
import infra.beans.factory.Aware;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.BeanInitializationException;
import infra.beans.factory.InitializingBean;
import infra.context.support.ApplicationObjectSupport;
import infra.core.io.Resource;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the {@link ApplicationContext} that it runs in.
 *
 * <p>Implementing this interface makes sense for example when an object
 * requires access to a set of collaborating beans. Note that configuration
 * via bean references is preferable to implementing this interface just
 * for bean lookup purposes.
 *
 * <p>This interface can also be implemented if an object needs access to file
 * resources, i.e. wants to call {@code getResource}, wants to publish
 * an application event, or requires access to the MessageSource. However,
 * it is preferable to implement the more specific {@link ResourceLoaderAware},
 * {@link ApplicationEventPublisherAware} or {@link MessageSourceAware} interface
 * in such a specific scenario.
 *
 * <p>Note that file resource dependencies can also be exposed as bean properties
 * of type {@link Resource}, populated via Strings
 * with automatic type conversion by the bean factory. This removes the need
 * for implementing any callback interface just for the purpose of accessing
 * a specific file resource.
 *
 * <p>{@link ApplicationObjectSupport} is a
 * convenience base class for application objects, implementing this interface.
 *
 * <p>For a list of all bean lifecycle methods, see the
 * {@link BeanFactory BeanFactory javadocs}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author TODAY <br>
 * 2018-07-17 21:35:52
 * @see ResourceLoaderAware
 * @see ApplicationEventPublisherAware
 * @see MessageSourceAware
 * @see BeanFactoryAware
 */
public interface ApplicationContextAware extends Aware {

  /**
   * Set the ApplicationContext that this object runs in.
   * Normally this call will be used to initialize the object.
   * <p>Invoked after population of normal bean properties but before an init callback such
   * as {@link InitializingBean#afterPropertiesSet()}
   * or a custom init-method. Invoked after {@link ResourceLoaderAware#setResourceLoader},
   * {@link ApplicationEventPublisherAware#setApplicationEventPublisher} and
   * {@link MessageSourceAware}, if applicable.
   *
   * @param applicationContext the ApplicationContext object to be used by this object
   * @throws ApplicationContextException in case of context initialization errors
   * @throws BeansException if thrown by application context methods
   * @see BeanInitializationException
   */
  void setApplicationContext(ApplicationContext applicationContext) throws BeansException;

}
