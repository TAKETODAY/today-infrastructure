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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.web.servlet.resource.GzipSupport.GzippedFiles;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for
 * {@link cn.taketoday.web.servlet.resource.CachingResourceResolver}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(GzipSupport.class)
public class CachingResourceResolverTests {

	private Cache cache;

	private ResourceResolvingChain chain;

	private List<Resource> locations;


	@BeforeEach
	public void setup() {

		this.cache = new ConcurrentMapCache("resourceCache");

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(new CachingResourceResolver(this.cache));
		resolvers.add(new PathResourceResolver());
		this.chain = new DefaultResourceResolvingChain(resolvers);

		this.locations = new ArrayList<>();
		this.locations.add(new ClassPathResource("test/", getClass()));
	}


	@Test
	public void resolveResourceInternal() {
		Resource expected = new ClassPathResource("test/bar.css", getClass());
		Resource actual = this.chain.resolveResource(null, "bar.css", this.locations);

		assertThat(actual).isNotSameAs(expected);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void resolveResourceInternalFromCache() {
		Resource expected = Mockito.mock(Resource.class);
		this.cache.put(resourceKey("bar.css"), expected);
		Resource actual = this.chain.resolveResource(null, "bar.css", this.locations);

		assertThat(actual).isSameAs(expected);
	}

	@Test
	public void resolveResourceInternalNoMatch() {
		assertThat(this.chain.resolveResource(null, "invalid.css", this.locations)).isNull();
	}

	@Test
	public void resolverUrlPath() {
		String expected = "/foo.css";
		String actual = this.chain.resolveUrlPath(expected, this.locations);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void resolverUrlPathFromCache() {
		String expected = "cached-imaginary.css";
		this.cache.put(CachingResourceResolver.RESOLVED_URL_PATH_CACHE_KEY_PREFIX + "imaginary.css", expected);
		String actual = this.chain.resolveUrlPath("imaginary.css", this.locations);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void resolverUrlPathNoMatch() {
		assertThat(this.chain.resolveUrlPath("invalid.css", this.locations)).isNull();
	}

	@Test
	public void resolveResourceAcceptEncodingInCacheKey(GzippedFiles gzippedFiles) throws IOException {

		String file = "bar.css";
		gzippedFiles.create(file);

		// 1. Resolve plain resource

		MockHttpServletRequest request = new MockHttpServletRequest("GET", file);
		Resource expected = this.chain.resolveResource(request, file, this.locations);

		String cacheKey = resourceKey(file);
		assertThat(this.cache.get(cacheKey).get()).isSameAs(expected);

		// 2. Resolve with Accept-Encoding

		request = new MockHttpServletRequest("GET", file);
		request.addHeader("Accept-Encoding",  "gzip ; a=b  , deflate ,  br  ; c=d ");
		expected = this.chain.resolveResource(request, file, this.locations);

		cacheKey = resourceKey(file + "+encoding=br,gzip");
		assertThat(this.cache.get(cacheKey).get()).isSameAs(expected);

		// 3. Resolve with Accept-Encoding but no matching codings

		request = new MockHttpServletRequest("GET", file);
		request.addHeader("Accept-Encoding", "deflate");
		expected = this.chain.resolveResource(request, file, this.locations);

		cacheKey = resourceKey(file);
		assertThat(this.cache.get(cacheKey).get()).isSameAs(expected);
	}

	@Test
	public void resolveResourceNoAcceptEncoding() {
		String file = "bar.css";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", file);
		Resource expected = this.chain.resolveResource(request, file, this.locations);

		String cacheKey = resourceKey(file);
		Object actual = this.cache.get(cacheKey).get();

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void resolveResourceMatchingEncoding() {
		Resource resource = Mockito.mock(Resource.class);
		Resource gzipped = Mockito.mock(Resource.class);
		this.cache.put(resourceKey("bar.css"), resource);
		this.cache.put(resourceKey("bar.css+encoding=gzip"), gzipped);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "bar.css");
		assertThat(this.chain.resolveResource(request, "bar.css", this.locations)).isSameAs(resource);

		request = new MockHttpServletRequest("GET", "bar.css");
		request.addHeader("Accept-Encoding", "gzip");
		assertThat(this.chain.resolveResource(request, "bar.css", this.locations)).isSameAs(gzipped);
	}

	private static String resourceKey(String key) {
		return CachingResourceResolver.RESOLVED_RESOURCE_CACHE_KEY_PREFIX + key;
	}

}
