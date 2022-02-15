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

package cn.taketoday.web.resource;

import cn.taketoday.core.io.Resource;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A contract for invoking a chain of {@link ResourceTransformer ResourceTransformers} where each resolver
 * is given a reference to the chain allowing it to delegate when necessary.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ResourceTransformerChain {

	/**
	 * Return the {@code ResourceResolverChain} that was used to resolve the
	 * {@code Resource} being transformed. This may be needed for resolving
	 * related resources, e.g. links to other resources.
	 */
	ResourceResolverChain getResolverChain();

	/**
	 * Transform the given resource.
	 * @param request the current request
	 * @param resource the candidate resource to transform
	 * @return the transformed or the same resource, never {@code null}
	 * @throws IOException if transformation fails
	 */
	Resource transform(HttpServletRequest request, Resource resource) throws IOException;

}
