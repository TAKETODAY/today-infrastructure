/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.http.converter.xml;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.xml.XmlFactory;
import tools.jackson.dataformat.xml.XmlMapper;

import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.http.converter.AbstractJacksonHttpMessageConverter;
import infra.http.converter.json.ProblemDetailJacksonXmlMixin;
import infra.util.xml.StaxUtils;

/**
 * Implementation of {@link infra.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write XML using <a href="https://github.com/FasterXML/jackson-dataformat-xml">
 * Jackson 3.x extension component for reading and writing XML encoded data</a>.
 *
 * <p>By default, this converter supports {@code application/xml}, {@code text/xml}, and
 * {@code application/*+xml} with {@code UTF-8} character set. This can be overridden by
 * setting the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * <p>The following hint entries are supported:
 * <ul>
 *     <li>A JSON view with a <code>com.fasterxml.jackson.annotation.JsonView</code>
 *         key and the class name of the JSON view as value.</li>
 *     <li>A filter provider with a <code>tools.jackson.databind.ser.FilterProvider</code>
 *         key and the filter provider class name as value.</li>
 * </ul>
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class JacksonXmlHttpMessageConverter extends AbstractJacksonHttpMessageConverter<XmlMapper> {

	private static final List<MediaType> problemDetailMediaTypes =
			Collections.singletonList(MediaType.APPLICATION_PROBLEM_XML);

	private static final MediaType[] DEFAULT_XML_MIME_TYPES = new MediaType[] {
			new MediaType("application", "xml", StandardCharsets.UTF_8),
			new MediaType("text", "xml", StandardCharsets.UTF_8),
			new MediaType("application", "*+xml", StandardCharsets.UTF_8)
	};

	/**
	 * Construct a new instance with an {@link XmlMapper} created from
	 * {@link #defensiveXmlFactory} and customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)} and
	 * {@link ProblemDetailJacksonXmlMixin}.
	 */
	public JacksonXmlHttpMessageConverter() {
		this(XmlMapper.builder(defensiveXmlFactory()));
	}

	/**
	 * Construct a new instance with the provided {@link XmlMapper.Builder builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)} and
	 * {@link ProblemDetailJacksonXmlMixin}.
	 * @see XmlMapper#builder()
	 */
	public JacksonXmlHttpMessageConverter(XmlMapper.Builder builder) {
		super(builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonXmlMixin.class), DEFAULT_XML_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link XmlMapper}.
	 * @see XmlMapper#builder()
	 */
	public JacksonXmlHttpMessageConverter(XmlMapper xmlMapper) {
		super(xmlMapper, DEFAULT_XML_MIME_TYPES);
	}

	/**
	 * Return an {@link XmlFactory} created from {@link StaxUtils#createDefensiveInputFactory}
	 * with Spring's defensive setup, i.e. no support for the resolution of DTDs and external
	 * entities.
	 */
	public static XmlFactory defensiveXmlFactory() {
		return new XmlFactory(StaxUtils.createDefensiveInputFactory());
	}

	@Override
	protected List<MediaType> getMediaTypesForProblemDetail() {
		return problemDetailMediaTypes;
	}

}
