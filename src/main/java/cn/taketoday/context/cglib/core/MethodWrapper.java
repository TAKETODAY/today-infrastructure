/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.context.cglib.core;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author TODAY <br>
 *         2019-09-03 12:58
 */
public class MethodWrapper {

    private static final MethodWrapperKey KEY_FACTORY = (MethodWrapperKey) KeyFactory.create(MethodWrapperKey.class);

    /** Internal interface, only public due to ClassLoader issues. */
    public interface MethodWrapperKey {
        Object newInstance(String name, String[] parameterTypes, String returnType);
    }

    private MethodWrapper() {
    }

    public static Object create(Method method) {
        return KEY_FACTORY.newInstance(method.getName(), ReflectUtils.getNames(method.getParameterTypes()),
                method.getReturnType().getName());
    }

    public static Set<Object> createSet(Collection<Method> methods) {
        final Set<Object> ret = new HashSet<>();

        for (final Method method : methods) {
            ret.add(create(method));
        }
        return ret;
    }
}
