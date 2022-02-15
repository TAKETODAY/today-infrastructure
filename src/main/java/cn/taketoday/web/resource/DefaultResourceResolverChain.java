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
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Default immutable implementation of {@link ResourceResolverChain}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class DefaultResourceResolverChain implements ResourceResolverChain {

	@Nullable
	private final ResourceResolver resolver;

	@Nullable
	private final ResourceResolverChain nextChain;


	public DefaultResourceResolverChain(@Nullable List<? extends ResourceResolver> resolvers) {
		resolvers = (resolvers != null ? resolvers : Collections.emptyList());
		DefaultResourceResolverChain chain = initChain(new ArrayList<>(resolvers));
		this.resolver = chain.resolver;
		this.nextChain = chain.nextChain;
	}

	private static DefaultResourceResolverChain initChain(ArrayList<? extends ResourceResolver> resolvers) {
		DefaultResourceResolverChain chain = new DefaultResourceResolverChain(null, null);
		ListIterator<? extends ResourceResolver> it = resolvers.listIterator(resolvers.size());
		while (it.hasPrevious()) {
			chain = new DefaultResourceResolverChain(it.previous(), chain);
		}
		return chain;
	}

	private DefaultResourceResolverChain(@Nullable ResourceResolver resolver, @Nullable ResourceResolverChain chain) {
		Assert.isTrue((resolver == null && chain == null) || (resolver != null && chain != null),
				"Both resolver and resolver chain must be null, or neither is");
		this.resolver = resolver;
		this.nextChain = chain;
	}


	@Override
	@Nullable
	public Resource resolveResource(
			@Nullable HttpServletRequest request, String requestPath, List<? extends Resource> locations) {

		return (this.resolver != null && this.nextChain != null ?
				this.resolver.resolveResource(request, requestPath, locations, this.nextChain) : null);
	}

	@Override
	@Nullable
	public String resolveUrlPath(String resourcePath, List<? extends Resource> locations) {
		return (this.resolver != null && this.nextChain != null ?
				this.resolver.resolveUrlPath(resourcePath, locations, this.nextChain) : null);
	}

}
