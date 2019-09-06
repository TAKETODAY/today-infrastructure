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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.Type;

/**
 * 
 * @author TODAY <br>
 *         2019-09-03 14:19
 */
public abstract class TypeUtils {

    private static final Map<String, String> transforms = new HashMap<>();
    private static final Map<String, String> rtransforms = new HashMap<>();

    static {
        transforms.put("void", "V");
        transforms.put("byte", "B");
        transforms.put("char", "C");
        transforms.put("double", "D");
        transforms.put("float", "F");
        transforms.put("int", "I");
        transforms.put("long", "J");
        transforms.put("short", "S");
        transforms.put("boolean", "Z");

        CollectionUtils.reverse(transforms, rtransforms);
    }

    public static Type getType(String className) {
        return Type.getType('L' + className.replace('.', '/') + ';');
    }

    public static boolean isSynthetic(int access) {
        return (Constant.ACC_SYNTHETIC & access) != 0;
    }

    public static boolean isBridge(int access) {
        return (Constant.ACC_BRIDGE & access) != 0;
    }

    // getPackage returns null on JDK 1.2
    public static String getPackageName(Type type) {
        return getPackageName(getClassName(type));
    }

    public static String getPackageName(String className) {
        int idx = className.lastIndexOf('.');
        return (idx < 0) ? "" : className.substring(0, idx);
    }

    public static String upperFirst(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String getClassName(Type type) {
        if (isPrimitive(type)) {
            return rtransforms.get(type.getDescriptor());
        }
        else if (isArray(type)) {
            return getClassName(getComponentType(type)) + "[]";
        }
        else {
            return type.getClassName();
        }
    }

    public static Type[] add(Type[] types, Type extra) {
        if (types == null) {
            return new Type[] { extra };
        }
        else {
            List<Type> list = Arrays.asList(types);
            if (list.contains(extra)) {
                return types;
            }
            Type[] copy = new Type[types.length + 1];
            System.arraycopy(types, 0, copy, 0, types.length);
            copy[types.length] = extra;
            return copy;
        }
    }

    public static Type[] add(Type[] t1, Type[] t2) {
        // TODO: set semantics?
        Type[] all = new Type[t1.length + t2.length];
        System.arraycopy(t1, 0, all, 0, t1.length);
        System.arraycopy(t2, 0, all, t1.length, t2.length);
        return all;
    }

    public static Type fromInternalName(String name) {
        // TODO; primitives?
        return Type.getType('L' + name + ';');
    }

    public static Type[] fromInternalNames(String[] names) {
        if (names == null) {
            return null;
        }
        Type[] types = new Type[names.length];
        for (int i = 0; i < names.length; i++) {
            types[i] = fromInternalName(names[i]);
        }
        return types;
    }

    public static int getStackSize(Type[] types) {
        int size = 0;
        for (int i = 0; i < types.length; i++) {
            size += types[i].getSize();
        }
        return size;
    }

    public static String[] toInternalNames(Type[] types) {
        if (types == null) {
            return null;
        }
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].getInternalName();
        }
        return names;
    }

    public static Signature parseSignature(String s) {
        int space = s.indexOf(' ');
        int lparen = s.indexOf('(', space);
        int rparen = s.indexOf(')', lparen);
        String returnType = s.substring(0, space);
        String methodName = s.substring(space + 1, lparen);
        StringBuffer sb = new StringBuffer();
        sb.append('(');

        for (String type : parseTypes(s, lparen + 1, rparen)) {
            sb.append(type);
        }
        sb.append(')');
        sb.append(map(returnType));
        return new Signature(methodName, sb.toString());
    }

    public static Type parseType(Class<?> type) {
        return Type.getType(map(type.getName()));
    }

    public static Type parseType(String s) {
        return Type.getType(map(s));
    }

    public static Type[] parseTypes(String s) {
        List<String> names = parseTypes(s, 0, s.length());
        Type[] types = new Type[names.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = Type.getType(names.get(i));
        }
        return types;
    }

    public static Signature parseConstructor(Type... types) {
        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < types.length; i++) {
            sb.append(types[i].getDescriptor());
        }
        sb.append(')');
        sb.append('V');
        return new Signature(Constant.CONSTRUCTOR_NAME, sb.toString());
    }

    public static Signature parseConstructor(String sig) {
        return parseSignature("void <init>(" + sig + ')'); // TODO
    }

    private static List<String> parseTypes(String s, int mark, int end) {
        List<String> types = new ArrayList<>(5);
        for (;;) {
            int next = s.indexOf(',', mark);
            if (next < 0) {
                break;
            }
            types.add(map(s.substring(mark, next).trim()));
            mark = next + 1;
        }
        types.add(map(s.substring(mark, end).trim()));
        return types;
    }

    private static String map(String type) {
        if (Constant.BLANK.equals(type)) {
            return type;
        }
        String t = transforms.get(type);
        if (t != null) {
            return t;
        }
        else if (type.indexOf('.') < 0) {
            return map("java.lang." + type);
        }
        else {
            StringBuffer sb = new StringBuffer();
            int index = 0;
            while ((index = type.indexOf("[]", index) + 1) > 0) {
                sb.append('[');
            }
            type = type.substring(0, type.length() - sb.length() * 2);
            sb.append('L').append(type.replace('.', '/')).append(';');
            return sb.toString();
        }
    }

    public static Type getBoxedType(Type type) {
        switch (type.getSort())
        {
            case Type.CHAR :
                return Constant.TYPE_CHARACTER;
            case Type.BOOLEAN :
                return Constant.TYPE_BOOLEAN;
            case Type.DOUBLE :
                return Constant.TYPE_DOUBLE;
            case Type.FLOAT :
                return Constant.TYPE_FLOAT;
            case Type.LONG :
                return Constant.TYPE_LONG;
            case Type.INT :
                return Constant.TYPE_INTEGER;
            case Type.SHORT :
                return Constant.TYPE_SHORT;
            case Type.BYTE :
                return Constant.TYPE_BYTE;
            default:
                return type;
        }
    }

    public static Type getUnboxedType(Type type) {
        if (Constant.TYPE_INTEGER.equals(type)) {
            return Type.INT_TYPE;
        }
        else if (Constant.TYPE_BOOLEAN.equals(type)) {
            return Type.BOOLEAN_TYPE;
        }
        else if (Constant.TYPE_DOUBLE.equals(type)) {
            return Type.DOUBLE_TYPE;
        }
        else if (Constant.TYPE_LONG.equals(type)) {
            return Type.LONG_TYPE;
        }
        else if (Constant.TYPE_CHARACTER.equals(type)) {
            return Type.CHAR_TYPE;
        }
        else if (Constant.TYPE_BYTE.equals(type)) {
            return Type.BYTE_TYPE;
        }
        else if (Constant.TYPE_FLOAT.equals(type)) {
            return Type.FLOAT_TYPE;
        }
        else if (Constant.TYPE_SHORT.equals(type)) {
            return Type.SHORT_TYPE;
        }
        else {
            return type;
        }
    }

    public static boolean isArray(Type type) {
        return type.getSort() == Type.ARRAY;
    }

    public static Type getComponentType(Type type) {
        if (!isArray(type)) {
            throw new IllegalArgumentException("Type " + type + " is not an array");
        }
        return Type.getType(type.getDescriptor().substring(1));
    }

    public static boolean isPrimitive(Type type) {
        switch (type.getSort())
        {
            case Type.ARRAY :
            case Type.OBJECT :
                return false;
            default:
                return true;
        }
    }

    public static String emulateClassGetName(Type type) {
        if (isArray(type)) {
            return type.getDescriptor().replace('/', '.');
        }
        else {
            return getClassName(type);
        }
    }

    public static boolean isConstructor(MethodInfo method) {
        return method.getSignature().getName().equals(Constant.CONSTRUCTOR_NAME);
    }

    public static Type[] getTypes(Class<?>[] classes) {
        if (classes == null) {
            return null;
        }
        Type[] types = new Type[classes.length];
        for (int i = 0; i < classes.length; i++) {
            types[i] = Type.getType(classes[i]);
        }
        return types;
    }

    public static int ICONST(int value) {
        switch (value)
        {
            case -1 :
                return Constant.ICONST_M1;
            case 0 :
                return Constant.ICONST_0;
            case 1 :
                return Constant.ICONST_1;
            case 2 :
                return Constant.ICONST_2;
            case 3 :
                return Constant.ICONST_3;
            case 4 :
                return Constant.ICONST_4;
            case 5 :
                return Constant.ICONST_5;
        }
        return -1; // error
    }

    public static int LCONST(long value) {
        if (value == 0L) {
            return Constant.LCONST_0;
        }
        else if (value == 1L) {
            return Constant.LCONST_1;
        }
        else {
            return -1; // error
        }
    }

    public static int FCONST(float value) {
        if (value == 0f) {
            return Constant.FCONST_0;
        }
        else if (value == 1f) {
            return Constant.FCONST_1;
        }
        else if (value == 2f) {
            return Constant.FCONST_2;
        }
        else {
            return -1; // error
        }
    }

    public static int DCONST(double value) {
        if (value == 0d) {
            return Constant.DCONST_0;
        }
        else if (value == 1d) {
            return Constant.DCONST_1;
        }
        else {
            return -1; // error
        }
    }

    public static int NEWARRAY(Type type) {
        switch (type.getSort())
        {
            case Type.BYTE :
                return Constant.T_BYTE;
            case Type.CHAR :
                return Constant.T_CHAR;
            case Type.DOUBLE :
                return Constant.T_DOUBLE;
            case Type.FLOAT :
                return Constant.T_FLOAT;
            case Type.INT :
                return Constant.T_INT;
            case Type.LONG :
                return Constant.T_LONG;
            case Type.SHORT :
                return Constant.T_SHORT;
            case Type.BOOLEAN :
                return Constant.T_BOOLEAN;
            default:
                return -1; // error
        }
    }

    public static String escapeType(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            switch (c)
            {
                case '$' :
                    sb.append("$24");
                    break;
                case '.' :
                    sb.append("$2E");
                    break;
                case '[' :
                    sb.append("$5B");
                    break;
                case ';' :
                    sb.append("$3B");
                    break;
                case '(' :
                    sb.append("$28");
                    break;
                case ')' :
                    sb.append("$29");
                    break;
                case '/' :
                    sb.append("$2F");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

}
