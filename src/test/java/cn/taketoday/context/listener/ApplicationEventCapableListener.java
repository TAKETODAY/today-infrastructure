/**
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
package cn.taketoday.context.listener;

import java.util.EventObject;

import cn.taketoday.context.event.EventProvider;
import cn.taketoday.context.event.ApplicationListener;

/**
 * @author TODAY <br>
 *         2019-11-05 23:12
 */
public class ApplicationEventCapableListener implements ApplicationListener<EventObject>, EventProvider {

    @Override
    public void onApplicationEvent(EventObject event) {

        System.err.println("current event: " + event);
    }

    @Override
    public Class<?>[] getSupportedEvent() {
        return factory(BeanDefinitionLoadingEvent.class, BeanDefinitionLoadedEvent.class);
    }

}
