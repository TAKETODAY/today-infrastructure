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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Convert an Object to {@code java.util.Optional<T>} if necessary using the
 * {@code ConversionService} to convert the source Object to the generic type
 * of Optional when known.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 */
final class ObjectToOptionalConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;


	public ObjectToOptionalConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		Set<ConvertiblePair> convertibleTypes = new LinkedHashSet<>(4);
		convertibleTypes.add(new ConvertiblePair(Collection.class, Optional.class));
		convertibleTypes.add(new ConvertiblePair(Object[].class, Optional.class));
		convertibleTypes.add(new ConvertiblePair(Object.class, Optional.class));
		return convertibleTypes;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType.getResolvableType().hasGenerics()) {
			return this.conversionService.canConvert(sourceType, new GenericTypeDescriptor(targetType));
		}
		else {
			return true;
		}
	}

	@Override
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return Optional.empty();
		}
		else if (source instanceof Optional) {
			return source;
		}
		else if (targetType.getResolvableType().hasGenerics()) {
			Object target = this.conversionService.convert(source, sourceType, new GenericTypeDescriptor(targetType));
			if (target == null || (target.getClass().isArray() && Array.getLength(target) == 0) ||
						(target instanceof Collection && ((Collection<?>) target).isEmpty())) {
				return Optional.empty();
			}
			return Optional.of(target);
		}
		else {
			return Optional.of(source);
		}
	}


	@SuppressWarnings("serial")
	private static class GenericTypeDescriptor extends TypeDescriptor {

		public GenericTypeDescriptor(TypeDescriptor typeDescriptor) {
			super(typeDescriptor.getResolvableType().getGeneric(), null, typeDescriptor.getAnnotations());
		}
	}

}
