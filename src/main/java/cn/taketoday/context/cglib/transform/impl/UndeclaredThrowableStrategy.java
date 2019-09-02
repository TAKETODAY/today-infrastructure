/*
 * Copyright 2003 The Apache Software Foundation
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
package cn.taketoday.context.cglib.transform.impl;

import cn.taketoday.context.cglib.core.ClassGenerator;
import cn.taketoday.context.cglib.core.DefaultGeneratorStrategy;
import cn.taketoday.context.cglib.core.GeneratorStrategy;
import cn.taketoday.context.cglib.core.TypeUtils;
import cn.taketoday.context.cglib.transform.ClassTransformer;
import cn.taketoday.context.cglib.transform.MethodFilter;
import cn.taketoday.context.cglib.transform.MethodFilterTransformer;
import cn.taketoday.context.cglib.transform.TransformingClassGenerator;

/**
 * A {@link GeneratorStrategy} suitable for use with
 * {@link cn.taketoday.context.cglib.proxy.Enhancer} which causes all undeclared
 * exceptions thrown from within a proxied method to be wrapped in an
 * alternative exception of your choice.
 */
@SuppressWarnings("all")
public class UndeclaredThrowableStrategy extends DefaultGeneratorStrategy {

    private final Class wrapper;

    /**
     * Create a new instance of this strategy.
     * 
     * @param wrapper
     *            a class which extends either directly or indirectly from
     *            <code>Throwable</code> and which has at least one constructor that
     *            takes a single argument of type <code>Throwable</code>, for
     *            example
     *            <code>java.lang.reflect.UndeclaredThrowableException.class</code>
     */
    public UndeclaredThrowableStrategy(Class wrapper) {
        this.wrapper = wrapper;
    }

    private static final MethodFilter TRANSFORM_FILTER = new MethodFilter() {
        public boolean accept(int access, String name, String desc, String signature, String[] exceptions) {
            return !TypeUtils.isPrivate(access) && name.indexOf('$') < 0;
        }
    };

    protected ClassGenerator transform(ClassGenerator cg) throws Exception {
        ClassTransformer tr = new UndeclaredThrowableTransformer(wrapper);
        tr = new MethodFilterTransformer(TRANSFORM_FILTER, tr);
        return new TransformingClassGenerator(cg, tr);
    }
}
