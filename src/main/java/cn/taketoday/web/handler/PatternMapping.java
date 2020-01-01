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
package cn.taketoday.web.handler;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;

import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.utils.OrderUtils;

/**
 * @author TODAY <br>
 *         2019-12-25 14:51
 */
public class PatternMapping implements Serializable, Ordered {

    private static final long serialVersionUID = 1L;

    private final String pattern;
    private final Object handler;

    public PatternMapping(String pattern, Object handler) {
        this.pattern = nonNull(pattern, "pattern must not be null");
        this.handler = nonNull(handler, "handler must not be null");
    }

    public String getPattern() {
        return pattern;
    }

    public Object getHandler() {
        return handler;
    }

    @Override
    public int getOrder() {
        return OrderUtils.getOrder(handler);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PatternMapping && obj.getClass() == getClass()) {
            final PatternMapping other = (PatternMapping) obj;
            return Objects.equals(other.pattern, pattern)
                   && Objects.equals(other.handler, handler);
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("[pattern=")
                .append(pattern)
                .append(", handler=")
                .append(handler)
                .append(']')
                .toString();
    }

}
