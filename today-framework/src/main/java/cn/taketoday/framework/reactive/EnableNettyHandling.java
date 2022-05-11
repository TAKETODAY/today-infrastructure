/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.framework.reactive;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.loader.AnnotationImportSelector;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.framework.web.session.EnableWebSession;
import cn.taketoday.web.RequestContextHolder;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enable Netty, Enable {@link cn.taketoday.web.session.WebSession}
 *
 * @author TODAY 2019-11-22 00:30
 */
@Retention(RUNTIME)
@EnableWebSession
@Target({ TYPE, METHOD })
@Import(NettyConfigurationImportSelector.class)
public @interface EnableNettyHandling {

  /**
   * determine using which  {@link NettyDispatcher}
   *
   * @see AsyncNettyDispatcherHandler
   */
  boolean async() default true;

  /**
   * Using {@link io.netty.util.concurrent.FastThreadLocal}
   * to hold {@link cn.taketoday.web.RequestContext}
   */
  boolean fastThreadLocal() default true;

}

final class NettyConfigurationImportSelector implements AnnotationImportSelector<EnableNettyHandling> {

  /**
   * register a {@link NettyDispatcher} bean
   */
  @Override
  public String[] selectImports(
          EnableNettyHandling target, AnnotationMetadata annotatedMetadata) {
    // replace context holder
    if (target.fastThreadLocal()) {
      RequestContextHolder.replaceContextHolder(new FastRequestThreadLocal());
    }

    if (target.async()) {
      return new String[] {
              AsyncNettyDispatcherHandler.class.getName(),
              NettyConfiguration.class.getName()
      };
    }
    else {
      return new String[] {
              NettyDispatcher.class.getName(),
              NettyConfiguration.class.getName()
      };
    }
  }

}
