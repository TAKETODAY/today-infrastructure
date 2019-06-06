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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.conversion.StringTypeConverter;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.io.Resource;

/**
 * 
 * @author TODAY <br>
 *         2018-07-12 20:43:53
 */
// @Slf4j
public abstract class ConvertUtils {

    private static TypeConverter[] converters;

    /**
     * Convert source to target type
     * 
     * @param value
     *            value
     * @param targetClass
     *            targetClass
     * @return converted object
     * @throws IOException
     */
    public static Object convert(Object source, Class<?> targetClass) {

        if (targetClass.isInstance(source)) {
            return targetClass.cast(source);
        }

        for (TypeConverter converter : getConverters()) {
            if (converter.supports(targetClass, source)) {
                return converter.convert(targetClass, source);
            }
        }
        return null;
    }

    public static TypeConverter[] getConverters() {
        return converters;
    }

    public static void setConverters(TypeConverter[] converters) {
        ConvertUtils.converters = converters;
    }

    /**
     * Add {@link TypeConverter} to {@link #converters}
     * 
     * @param converters
     *            {@link TypeConverter} object
     * @since 2.1.6
     */
    public static void addConverter(TypeConverter... converters) {

        final List<TypeConverter> typeConverters = new ArrayList<>();

        if (getConverters() != null) {
            Collections.addAll(typeConverters, getConverters());
        }

        Collections.addAll(typeConverters, converters);

        OrderUtils.reversedSort(typeConverters);

        setConverters(typeConverters.toArray(new TypeConverter[0]));
    }

    static {
        addConverter(//
                new StringEnumConverter(), //
                new StringArrayConverter(), //
                new StringNumberConverter(), //
                new StringResourceConverter(), //
                new StringConstructorConverter(), //
                new DelegatingStringTypeConverter<>(Class.class, source -> {
                    try {
                        return Class.forName(source);
                    }
                    catch (ClassNotFoundException e) {
                        throw new ConversionException(e);
                    }
                }), //
                new DelegatingStringTypeConverter<>(DataSize.class, DataSize::parse), //
                new DelegatingStringTypeConverter<>(Charset.class, Charset::forName), //
                new DelegatingStringTypeConverter<>(Duration.class, ConvertUtils::parseDuration)//
        );
    }

    /**
     * @author TODAY <br>
     *         2019-06-06 15:51
     * @since 2.1.6
     */
    public static class StringNumberConverter extends StringTypeConverter {

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public boolean supports(Class<?> targetClass) {
            return NumberUtils.isNumber(targetClass);
        }

        @Override
        protected Object convertInternal(Class<?> targetClass, String source) {
            return NumberUtils.parseDigit(source, targetClass);
        }
    }

    /**
     * @author TODAY <br>
     *         2019-06-06 15:50
     * @since 2.1.6
     */
    public static class StringResourceConverter extends StringTypeConverter {

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public boolean supports(Class<?> targetClass) {
            return Resource.class == targetClass //
                    || targetClass == URI.class//
                    || targetClass == URL.class//
                    || targetClass == File.class;
        }

        @Override
        protected Object convertInternal(Class<?> targetClass, String source) {

            try {
                final Resource resource = ResourceUtils.getResource(source);
                if (targetClass == File.class) {
                    return resource.getFile();
                }
                if (targetClass == URL.class) {
                    return resource.getLocation();
                }
                if (targetClass == URI.class) {
                    return resource.getLocation().toURI();
                }
                return resource;
            }
            catch (Throwable e) {
                throw new ConversionException(e);
            }
        }
    }

    /**
     * @author TODAY <br>
     *         2019-06-06 15:50
     * @since 2.1.6
     */
    public static class StringEnumConverter extends StringTypeConverter {

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public boolean supports(Class<?> targetClass) {
            return targetClass.isEnum();
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected Object convertInternal(Class<?> targetClass, String source) {
            return Enum.valueOf((Class<Enum>) targetClass, source);
        }
    }

    /**
     * @author TODAY <br>
     *         2019-06-06 16:06
     * @since 2.1.6
     */
    public static class DelegatingStringTypeConverter<T> extends StringTypeConverter {

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        private final Class<?> targetClass;
        private final Converter<String, T> converter;

        @Override
        public boolean supports(Class<?> targetClass) {
            return targetClass == this.targetClass;
        }

        public DelegatingStringTypeConverter(Class<?> targetClass, Converter<String, T> converter) {
            this.converter = converter;
            this.targetClass = targetClass;
        }

        @Override
        protected Object convertInternal(Class<?> targetClass, String source) throws ConversionException {
            return converter.convert(source);
        }

    }

    /**
     * @author TODAY <br>
     *         2019-06-06 15:50
     * @since 2.1.6
     */
    public static class StringArrayConverter extends StringTypeConverter {

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public boolean supports(Class<?> targetClass) {
            return targetClass.isArray();
        }

        @Override
        protected Object convertInternal(Class<?> targetClass, String source) {
            final Class<?> componentType = targetClass.getComponentType();

            final String[] split = StringUtils.split(source);

            final Object arrayValue = Array.newInstance(componentType, split.length);
            for (int i = 0; i < split.length; i++) {
                Array.set(arrayValue, i, ConvertUtils.convert(split[i], componentType));
            }
            return arrayValue;
        }
    }

    /**
     * @author TODAY <br>
     *         2019-06-06 16:12
     */
    public static class StringConstructorConverter extends StringTypeConverter {

        @Override
        public boolean supports(Class<?> targetClass) {
            try {
                targetClass.getConstructor(String.class);
                return true;
            }
            catch (NoSuchMethodException e) {
                return false;
            }
        }

        @Override
        protected Object convertInternal(Class<?> targetClass, String source) {
            try {
                return ClassUtils.makeAccessible(targetClass.getConstructor(String.class)).newInstance(source);
            }
            catch (Throwable e) {
                throw new ConversionException(e);
            }
        }
    }

    /**
     * Convert a string to {@link Duration}
     * 
     * @param value
     * @return
     */
    public static Duration parseDuration(String value) {

        if (value.endsWith("ns")) {
            return Duration.ofNanos(Long.parseLong(value.substring(0, value.length() - 2)));
        }
        if (value.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
        }
        if (value.endsWith("min")) {
            return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 3)));
        }

        if (value.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        if (value.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        if (value.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(value.substring(0, value.length() - 1)));
        }

        return Duration.parse(value);
    }
}
