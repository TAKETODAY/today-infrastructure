/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.web.view;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.OrderUtils;

/**
 * @author TODAY <br>
 *         2019-12-28 13:47
 */
public class ResultHandlers {

    private static final List<ResultHandler> resultHandlers = new LinkedList<>();

    public static void addHandler(ResultHandler... resolvers) {
        Collections.addAll(resultHandlers, resolvers);
        OrderUtils.reversedSort(resultHandlers);
    }

    public static void addHandler(List<ResultHandler> resolver) {
        resultHandlers.addAll(resolver);
        OrderUtils.reversedSort(resultHandlers);
    }

    public static void setHandler(List<ResultHandler> resolver) {
        resultHandlers.clear();
        resultHandlers.addAll(resolver);
        OrderUtils.reversedSort(resultHandlers);
    }

    public static List<ResultHandler> getHandlers() {
        return resultHandlers;
    }

    public static RuntimeResultHandler[] getRuntimeHandlers() {
        return resultHandlers
                .stream()
                .filter(res -> res instanceof RuntimeResultHandler)
                .toArray(RuntimeResultHandler[]::new);
    }

    /**
     * Get correspond view resolver, If there isn't a suitable resolver will be
     * throw {@link ConfigurationException}
     * 
     * @return A suitable {@link ResultHandler}
     */
    public static ResultHandler obtainHandler(final Object handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        for (final ResultHandler resolver : getHandlers()) {
            if (resolver.supports(handler)) {
                return resolver;
            }
        }
        throw new ConfigurationException("There isn't have a result resolver to resolve : [" + handler + "]");
    }

}
