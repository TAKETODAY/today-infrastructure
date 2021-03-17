/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import org.junit.Test;

import java.util.Collections;

import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Singleton;
import lombok.ToString;

/**
 * @author TODAY 2021/3/17 12:40
 */
public class MethodEventDrivenPostProcessorTests {

  @Test
  public void testMethodEventDrivenPostProcessor() {

    try (final StandardApplicationContext context = new StandardApplicationContext()) {
      context.addBeanPostProcessor(new MethodEventDrivenPostProcessor(context));

      context.importBeans(EventBean.class);

      context.publishEvent(new Event("test event"));
      context.publishEvent(new SubEvent("test SubEvent"));
      context.publishEvent(new StaticEvent());

    }
  }

  @EnableMethodEventDriven
  static class Config {

    @Singleton
    EventBean eventBean() {
      return new EventBean();
    }
  }

  @Test
  public void testEnableMethodEventDriven() {

    try (final StandardApplicationContext context = new StandardApplicationContext()) {
      context.importBeans(Config.class);

      context.load(Collections.emptyList());

      context.publishEvent(new Event("test event"));
      context.publishEvent(new SubEvent("test SubEvent"));
      context.publishEvent(new StaticEvent());

    }
  }

  static class EventBean {

    @EventListener
    public void listener(Event event) {
      System.out.println(event);
    }

    @EventListener(SubEvent.class)
    public void listener(Event event, EventBean bean) {
      System.out.println(event);
      System.out.println(bean);
    }

    @EventListener(StaticEvent.class)
    public static void listener(StaticEvent event, EventBean bean) {
      System.out.println(event);
      System.out.println(bean);
    }

  }

  @ToString
  static class Event {
    final String name;

    Event(String name) {
      this.name = name;
    }
  }

  @ToString
  static class SubEvent extends Event {

    SubEvent(String name) {
      super(name);
    }
  }

  @ToString
  static class StaticEvent {

  }

}
