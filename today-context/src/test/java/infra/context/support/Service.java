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

package infra.context.support;

import java.util.Set;

import infra.beans.factory.BeanCreationNotAllowedException;
import infra.beans.factory.DisposableBean;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.MessageSource;
import infra.context.MessageSourceAware;
import infra.core.io.Resource;
import infra.lang.Assert;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 */
public class Service implements ApplicationContextAware, MessageSourceAware, DisposableBean {

  private ApplicationContext applicationContext;

  private MessageSource messageSource;

  private Resource[] resources;

  private Set<Resource> resourceSet;

  private boolean properlyDestroyed = false;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setMessageSource(MessageSource messageSource) {
    if (this.messageSource != null) {
      throw new IllegalArgumentException("MessageSource should not be set twice");
    }
    this.messageSource = messageSource;
  }

  public MessageSource getMessageSource() {
    return messageSource;
  }

  public void setResources(Resource[] resources) {
    this.resources = resources;
  }

  public Resource[] getResources() {
    return resources;
  }

  public void setResourceSet(Set<Resource> resourceSet) {
    this.resourceSet = resourceSet;
  }

  public Set<Resource> getResourceSet() {
    return resourceSet;
  }

  @Override
  public void destroy() {
    this.properlyDestroyed = true;
    Thread thread = new Thread(() -> {
      Assert.state(applicationContext.getBean("messageSource") instanceof StaticMessageSource,
              "Invalid MessageSource bean");
      try {
        // Should not throw BeanCreationNotAllowedException on 6.2 anymore
        applicationContext.getBean("service2");
      }
      catch (BeanCreationNotAllowedException ex) {
        properlyDestroyed = false;
      }
    });
    thread.start();
    try {
      thread.join();
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  public boolean isProperlyDestroyed() {
    return properlyDestroyed;
  }

}
