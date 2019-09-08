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
package cn.taketoday.context.cglib.beans;

import static cn.taketoday.context.Constant.TYPE_OBJECT;
import static cn.taketoday.context.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.context.asm.Opcodes.JAVA_VERSION;

import java.beans.PropertyDescriptor;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.AbstractClassGenerator;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.KeyFactory;
import cn.taketoday.context.cglib.core.ReflectUtils;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
public class BeanGenerator extends AbstractClassGenerator<Object> {

    private static final Source SOURCE = new Source(BeanGenerator.class.getSimpleName());
    private static final BeanGeneratorKey KEY_FACTORY = (BeanGeneratorKey) KeyFactory.create(BeanGeneratorKey.class);

    interface BeanGeneratorKey {
        public Object newInstance(String superclass, Map<String, Type> props);
    }

    private boolean classOnly;
    private Class<?> superclass;
    private Map<String, Type> props = new HashMap<>();

    public BeanGenerator() {
        super(SOURCE);
    }

    /**
     * Set the class which the generated class will extend. The class must not be
     * declared as final, and must have a non-private no-argument constructor.
     * 
     * @param superclass
     *            class to extend, or null to extend Object
     */
    public void setSuperclass(Class<?> superclass) {
        if (superclass != null && superclass.equals(Object.class)) {
            superclass = null;
        }
        this.superclass = superclass;
    }

    public void addProperty(String name, Class<?> type) {
        if (props.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate property name \"" + name + "\"");
        }
        props.put(name, Type.getType(type));
    }

    @Override
    protected ClassLoader getDefaultClassLoader() {
        if (superclass != null) {
            return superclass.getClassLoader();
        }
        return null;
    }

    @Override
    protected ProtectionDomain getProtectionDomain() {
        return ReflectUtils.getProtectionDomain(superclass);
    }

    public Object create() {
        classOnly = false;
        return createHelper();
    }

    public Object createClass() {
        classOnly = true;
        return createHelper();
    }

    private Object createHelper() {
        if (superclass != null) {
            setNamePrefix(superclass.getName());
        }
        String superName = (superclass != null) ? superclass.getName() : "java.lang.Object";
        Object key = KEY_FACTORY.newInstance(superName, props);
        return super.create(key);
    }

    @Override
    public void generateClass(ClassVisitor v) throws Exception {

        int size = props.size();
        final Type[] types = new Type[size];

        int i = 0;
        for (final Entry<String, Type> entry : props.entrySet()) {
            types[i++] = entry.getValue();
        }

        ClassEmitter ce = new ClassEmitter(v);

        ce.beginClass(JAVA_VERSION, ACC_PUBLIC, getClassName(),
                      superclass != null ? Type.getType(superclass) : TYPE_OBJECT, null, null);

        EmitUtils.nullConstructor(ce);
        EmitUtils.addProperties(ce, props.keySet().toArray(Constant.EMPTY_STRING_ARRAY), types);
        ce.endClass();
    }

    @Override
    protected Object firstInstance(Class<Object> type) {
        if (classOnly) {
            return type;
        }
        return ReflectUtils.newInstance(type);
    }

    @Override
    protected Object nextInstance(Object instance) {
        Class<?> protoclass = (instance instanceof Class) ? (Class<?>) instance : instance.getClass();
        if (classOnly) {
            return protoclass;
        }
        return ReflectUtils.newInstance(protoclass);
    }

    public static void addProperties(BeanGenerator gen, Map<String, Class<?>> props) {
        props.forEach(gen::addProperty);
    }

    public static void addProperties(BeanGenerator gen, Class<?> type) {
        addProperties(gen, ReflectUtils.getBeanProperties(type));
    }

    public static void addProperties(BeanGenerator gen, PropertyDescriptor[] descriptors) {
        for (int i = 0; i < descriptors.length; i++) {
            gen.addProperty(descriptors[i].getName(), descriptors[i].getPropertyType());
        }
    }
}
