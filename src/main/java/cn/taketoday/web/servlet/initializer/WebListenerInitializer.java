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
package cn.taketoday.web.servlet.initializer;

import java.util.EventListener;

import javax.servlet.ServletContext;

import cn.taketoday.context.logger.LoggerFactory;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-02-25 17:59
 */
@Setter
@Getter
public class WebListenerInitializer<T extends EventListener> implements OrderedServletContextInitializer {

    private T listener;
    private int order = LOWEST_PRECEDENCE;

    public WebListenerInitializer() {

    }

    public WebListenerInitializer(T listener) {
        this.listener = listener;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws Throwable {
        final T listener = getListener();
        if (listener != null) {
            LoggerFactory.getLogger(WebListenerInitializer.class).debug("Configure listener registration: [{}]", this);
            servletContext.addListener(listener);
        }
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n\t\"listener\":\"");
        builder.append(listener);
        builder.append("\",\n\t\"order\":\"");
        builder.append(order);
        builder.append("\"\n}");
        return builder.toString();
    }
}
