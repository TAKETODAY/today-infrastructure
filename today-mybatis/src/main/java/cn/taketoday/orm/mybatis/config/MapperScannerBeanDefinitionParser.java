/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.orm.mybatis.config;

import org.w3c.dom.Element;

import java.lang.annotation.Annotation;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.xml.AbstractBeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.beans.factory.xml.XmlReaderContext;
import cn.taketoday.orm.mybatis.mapper.ClassPathMapperScanner;
import cn.taketoday.orm.mybatis.mapper.MapperFactoryBean;
import cn.taketoday.orm.mybatis.mapper.MapperScannerConfigurer;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * A {#code BeanDefinitionParser} that handles the element scan of the MyBatis. namespace
 *
 * @author Lishu Luo
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 * @see MapperScannerConfigurer
 * @since 4.0 2022/3/17 14:52
 */
public class MapperScannerBeanDefinitionParser extends AbstractBeanDefinitionParser {

  private static final String ATTRIBUTE_BASE_PACKAGE = "base-package";
  private static final String ATTRIBUTE_ANNOTATION = "annotation";
  private static final String ATTRIBUTE_MARKER_INTERFACE = "marker-interface";
  private static final String ATTRIBUTE_NAME_GENERATOR = "name-generator";
  private static final String ATTRIBUTE_TEMPLATE_REF = "template-ref";
  private static final String ATTRIBUTE_FACTORY_REF = "factory-ref";
  private static final String ATTRIBUTE_MAPPER_FACTORY_BEAN_CLASS = "mapper-factory-bean-class";
  private static final String ATTRIBUTE_LAZY_INITIALIZATION = "lazy-initialization";
  private static final String ATTRIBUTE_DEFAULT_SCOPE = "default-scope";

  /**
   * {@inheritDoc}
   */
  @Override
  protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);

    builder.addPropertyValue("processPropertyPlaceHolders", true);

    try {
      String annotationClassName = element.getAttribute(ATTRIBUTE_ANNOTATION);
      if (StringUtils.hasText(annotationClassName)) {
        Class<? extends Annotation> annotationClass = ClassUtils.forName(annotationClassName);
        builder.addPropertyValue("annotationClass", annotationClass);
      }

      String markerInterfaceClassName = element.getAttribute(ATTRIBUTE_MARKER_INTERFACE);
      if (StringUtils.hasText(markerInterfaceClassName)) {
        Class<?> markerInterface = ClassUtils.forName(markerInterfaceClassName);
        builder.addPropertyValue("markerInterface", markerInterface);
      }

      String nameGeneratorClassName = element.getAttribute(ATTRIBUTE_NAME_GENERATOR);
      if (StringUtils.hasText(nameGeneratorClassName)) {
        Class<BeanNameGenerator> nameGeneratorClass = ClassUtils.forName(nameGeneratorClassName);
        BeanNameGenerator nameGenerator = BeanUtils.newInstance(nameGeneratorClass);
        builder.addPropertyValue("nameGenerator", nameGenerator);
      }

      String mapperFactoryBeanClassName = element.getAttribute(ATTRIBUTE_MAPPER_FACTORY_BEAN_CLASS);
      if (StringUtils.hasText(mapperFactoryBeanClassName)) {
        Class<? extends MapperFactoryBean> mapperFactoryBeanClass = ClassUtils.forName(mapperFactoryBeanClassName);
        builder.addPropertyValue("mapperFactoryBeanClass", mapperFactoryBeanClass);
      }
    }
    catch (Exception ex) {
      XmlReaderContext readerContext = parserContext.getReaderContext();
      readerContext.error(ex.getMessage(), readerContext.extractSource(element), ex.getCause());
    }

    builder.addPropertyValue("basePackage", element.getAttribute(ATTRIBUTE_BASE_PACKAGE));
    builder.addPropertyValue("defaultScope", element.getAttribute(ATTRIBUTE_DEFAULT_SCOPE));
    builder.addPropertyValue("sqlSessionFactoryBeanName", element.getAttribute(ATTRIBUTE_FACTORY_REF));
    builder.addPropertyValue("lazyInitialization", element.getAttribute(ATTRIBUTE_LAZY_INITIALIZATION));
    builder.addPropertyValue("sqlSessionTemplateBeanName", element.getAttribute(ATTRIBUTE_TEMPLATE_REF));

    builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

    return builder.getBeanDefinition();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean shouldGenerateIdAsFallback() {
    return true;
  }

}
