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

package infra.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.aop.Advisor;
import infra.aop.framework.AopConfigException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryUtils;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Framework Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationAwareAspectJAutoProxyCreator
 * @since 4.0
 */
public class BeanFactoryAspectJAdvisorsBuilder {

  private static final Logger logger = LoggerFactory.getLogger(BeanFactoryAspectJAdvisorsBuilder.class);

  private final BeanFactory beanFactory;

  private final AspectJAdvisorFactory advisorFactory;

  @Nullable
  private volatile List<String> aspectBeanNames;

  private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

  private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();

  /**
   * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
   *
   * @param beanFactory the BeanFactory to scan
   */
  public BeanFactoryAspectJAdvisorsBuilder(BeanFactory beanFactory) {
    this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
  }

  /**
   * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
   *
   * @param beanFactory the BeanFactory to scan
   * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
   */
  public BeanFactoryAspectJAdvisorsBuilder(BeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
    Assert.notNull(beanFactory, "BeanFactory is required");
    Assert.notNull(advisorFactory, "AspectJAdvisorFactory is required");
    this.beanFactory = beanFactory;
    this.advisorFactory = advisorFactory;
  }

  /**
   * Look for AspectJ-annotated aspect beans in the current bean factory,
   * and return to a list of Framework AOP Advisors representing them.
   * <p>Creates a Framework Advisor for each AspectJ advice method.
   *
   * @return the list of {@link infra.aop.Advisor} beans
   * @see #isEligibleBean
   */
  @SuppressWarnings("NullAway")
  public List<Advisor> buildAspectJAdvisors() {
    List<String> aspectNames = this.aspectBeanNames;

    if (aspectNames == null) {
      synchronized(this) {
        aspectNames = this.aspectBeanNames;
        if (aspectNames == null) {
          List<Advisor> advisors = new ArrayList<>();
          aspectNames = new ArrayList<>();
          var beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                  this.beanFactory, Object.class, true, false);
          for (String beanName : beanNames) {
            if (!isEligibleBean(beanName)) {
              continue;
            }
            // We must be careful not to instantiate beans eagerly as in this case they
            // would be cached by the Infra container but would not have been weaved.
            Class<?> beanType = this.beanFactory.getType(beanName, false);
            if (beanType == null) {
              continue;
            }
            if (this.advisorFactory.isAspect(beanType)) {
              try {
                AspectMetadata amd = new AspectMetadata(beanType, beanName);
                if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
                  MetadataAwareAspectInstanceFactory factory =
                          new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
                  List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
                  if (this.beanFactory.isSingleton(beanName)) {
                    this.advisorsCache.put(beanName, classAdvisors);
                  }
                  else {
                    this.aspectFactoryCache.put(beanName, factory);
                  }
                  advisors.addAll(classAdvisors);
                }
                else {
                  // Per target or per this.
                  if (this.beanFactory.isSingleton(beanName)) {
                    throw new IllegalArgumentException("Bean with name '" + beanName +
                            "' is a singleton, but aspect instantiation model is not singleton");
                  }
                  MetadataAwareAspectInstanceFactory factory =
                          new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
                  this.aspectFactoryCache.put(beanName, factory);
                  advisors.addAll(this.advisorFactory.getAdvisors(factory));
                }
                aspectNames.add(beanName);
              }
              catch (IllegalArgumentException | IllegalStateException | AopConfigException ex) {
                if (logger.isDebugEnabled()) {
                  logger.debug("Ignoring incompatible aspect [%s]: %s".formatted(beanType.getName(), ex));
                }
              }
            }
          }
          this.aspectBeanNames = aspectNames;
          return advisors;
        }
      }
    }

    if (aspectNames.isEmpty()) {
      return Collections.emptyList();
    }
    List<Advisor> advisors = new ArrayList<>();
    for (String aspectName : aspectNames) {
      List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
      if (cachedAdvisors != null) {
        advisors.addAll(cachedAdvisors);
      }
      else {
        MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
        advisors.addAll(this.advisorFactory.getAdvisors(factory));
      }
    }
    return advisors;
  }

  /**
   * Return whether the aspect bean with the given name is eligible.
   *
   * @param beanName the name of the aspect bean
   * @return whether the bean is eligible
   */
  protected boolean isEligibleBean(String beanName) {
    return true;
  }

}
