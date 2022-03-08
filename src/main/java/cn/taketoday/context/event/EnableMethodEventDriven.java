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

package cn.taketoday.context.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.lang.Component;

/**
 * Usage:
 *
 * <pre>
 * class Event { }
 * class SubEvent extends Event { }
 * class StaticEvent { }
 *
 * class EventBean {
 *
 *  {@code @EventListener}
 *  public void listener(Event event) {
 *    System.out.println(event);
 *  }
 *
 *  {@code @EventListener}
 *  public void listener(Event event, EventBean bean, @Value("#{env.props}") int props) {
 *    System.out.println(event);
 *    System.out.println(bean);
 *  }
 *
 *  {@code @EventListener}
 *  public static void listener(StaticEvent event, EventBean bean) {
 *    System.out.println(event);
 *    System.out.println(bean);
 *  }
 *
 * }
 * </pre>
 *
 * @author TODAY 2021/3/17 12:37
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Import(MethodEventDrivenConfig.class)
public @interface EnableMethodEventDriven {

}

@Configuration(proxyBeanMethods = false)
class MethodEventDrivenConfig {

  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Component(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)
  static MethodEventDrivenPostProcessor eventDrivenPostProcessor(ConfigurableApplicationContext context) {
    return new MethodEventDrivenPostProcessor(context);
  }

  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Component(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)
  static DefaultEventListenerFactory eventListenerFactory() {
    return new DefaultEventListenerFactory();
  }

}
