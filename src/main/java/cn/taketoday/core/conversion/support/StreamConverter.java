/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.core.conversion.support;

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConditionalGenericConverter;
import cn.taketoday.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts a {@link Stream} to and from a collection or array, converting the
 * element type if necessary.
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
class StreamConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor STREAM_TYPE = TypeDescriptor.valueOf(Stream.class);

	private static final Set<ConvertiblePair> CONVERTIBLE_TYPES = createConvertibleTypes();

	private final ConversionService conversionService;


	public StreamConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return CONVERTIBLE_TYPES;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.isAssignableTo(STREAM_TYPE)) {
			return matchesFromStream(sourceType.getElementDescriptor(), targetType);
		}
		if (targetType.isAssignableTo(STREAM_TYPE)) {
			return matchesToStream(targetType.getElementDescriptor(), sourceType);
		}
		return false;
	}

	/**
	 * Validate that a {@link Collection} of the elements held within the stream can be
	 * converted to the specified {@code targetType}.
	 * @param elementType the type of the stream elements
	 * @param targetType the type to convert to
	 */
	public boolean matchesFromStream(@Nullable TypeDescriptor elementType, TypeDescriptor targetType) {
		TypeDescriptor collectionOfElement = TypeDescriptor.collection(Collection.class, elementType);
		return this.conversionService.canConvert(collectionOfElement, targetType);
	}

	/**
	 * Validate that the specified {@code sourceType} can be converted to a {@link Collection} of
	 * the type of the stream elements.
	 * @param elementType the type of the stream elements
	 * @param sourceType the type to convert from
	 */
	public boolean matchesToStream(@Nullable TypeDescriptor elementType, TypeDescriptor sourceType) {
		TypeDescriptor collectionOfElement = TypeDescriptor.collection(Collection.class, elementType);
		return this.conversionService.canConvert(sourceType, collectionOfElement);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.isAssignableTo(STREAM_TYPE)) {
			return convertFromStream((Stream<?>) source, sourceType, targetType);
		}
		if (targetType.isAssignableTo(STREAM_TYPE)) {
			return convertToStream(source, sourceType, targetType);
		}
		// Should not happen
		throw new IllegalStateException("Unexpected source/target types");
	}

	@Nullable
	private Object convertFromStream(@Nullable Stream<?> source, TypeDescriptor streamType, TypeDescriptor targetType) {
		List<Object> content = (source != null ? source.collect(Collectors.<Object>toList()) : Collections.emptyList());
		TypeDescriptor listType = TypeDescriptor.collection(List.class, streamType.getElementDescriptor());
		return this.conversionService.convert(content, listType, targetType);
	}

	private Object convertToStream(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor streamType) {
		TypeDescriptor targetCollection = TypeDescriptor.collection(List.class, streamType.getElementDescriptor());
		List<?> target = (List<?>) this.conversionService.convert(source, sourceType, targetCollection);
		if (target == null) {
			target = Collections.emptyList();
		}
		return target.stream();
	}


	private static Set<ConvertiblePair> createConvertibleTypes() {
		Set<ConvertiblePair> convertiblePairs = new HashSet<>();
		convertiblePairs.add(new ConvertiblePair(Stream.class, Collection.class));
		convertiblePairs.add(new ConvertiblePair(Stream.class, Object[].class));
		convertiblePairs.add(new ConvertiblePair(Collection.class, Stream.class));
		convertiblePairs.add(new ConvertiblePair(Object[].class, Stream.class));
		return convertiblePairs;
	}

}
