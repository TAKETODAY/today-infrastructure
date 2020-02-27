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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import java.util.ArrayList;

import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.ObjectFactory;

/**
 * @author TODAY <br>
 *         2018-07-02 22:38:57
 */
public interface Scope {

    String SINGLETON = Constant.SINGLETON;

    String PROTOTYPE = Constant.PROTOTYPE;

    ArrayList<Scope> values = new ArrayList<>();

    /**
     * Get scope name
     */
    String getName();

    Object get(String name, BeanDefinition def);

    /**
     * Return the object with the given name from the underlying scope,
     * {@link ObjectFactory#getObject() creating it} if not found in the underlying
     * storage mechanism.
     * <p>
     * This is the central operation of a Scope, and the only operation that is
     * absolutely required.
     * 
     * @param name
     *            the name of the object to retrieve
     * @param objectFactory
     *            the {@link ObjectFactory} to use to create the scoped object if it
     *            is not present in the underlying storage mechanism
     * @return the desired object (never {@code null})
     * @throws IllegalStateException
     *             if the underlying scope is not currently active
     */
    Object get(String name, ObjectFactory<?> objectFactory);

    Object remove(String name);

    static ArrayList<Scope> values() {
        return values;
    }

    static Scope valueOf(String name) {
        for (final Scope scope : values) {
            if (scope.getName().equals(name)) {
                return scope;
            }
        }
        return null;
    }
}
