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

package infra.cache.aspectj;

import infra.context.ApplicationContext;
import infra.context.support.GenericXmlApplicationContext;
import infra.contextsupport.testfixture.jcache.AbstractJCacheAnnotationTests;

/**
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public class JCacheAspectJNamespaceConfigTests extends AbstractJCacheAnnotationTests {

  @Override
  protected ApplicationContext getApplicationContext() {
    GenericXmlApplicationContext context = new GenericXmlApplicationContext();
    // Disallow bean definition overriding to test https://github.com/spring-projects/spring-framework/pull/27499
    context.setAllowBeanDefinitionOverriding(false);
    context.load("/infra/cache/config/annotation-jcache-aspectj.xml");
    context.refresh();
    return context;
  }

}
