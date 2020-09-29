/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.aop.listener;

import cn.taketoday.aop.advice.AspectsRegistry;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.logger.LoggerFactory;

/**
 * @author TODAY <br>
 * 2019-02-14 20:48
 */
public class AspectsDestroyListener
        extends OrderedSupport implements ApplicationListener<ContextCloseEvent> {

  public AspectsDestroyListener() {
    super(Ordered.LOWEST_PRECEDENCE);
  }

  @Override
  public void onApplicationEvent(ContextCloseEvent event) {

    LoggerFactory.getLogger(getClass()).info("Destroying Aspects Objects");

    final AspectsRegistry aspectsRegistry = AspectsRegistry.getInstance();
    aspectsRegistry.getAspects().clear();
    aspectsRegistry.setAspectsLoaded(false);
  }

}
