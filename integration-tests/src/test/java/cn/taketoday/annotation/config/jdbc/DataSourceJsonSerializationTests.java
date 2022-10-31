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

package cn.taketoday.annotation.config.jdbc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.SerializerFactory;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.Test;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that a {@link DataSource} can be exposed as JSON for actuator endpoints.
 *
 * @author Dave Syer
 */
class DataSourceJsonSerializationTests {

  @Test
  void serializerFactory() throws Exception {
    DataSource dataSource = new DataSource();
    SerializerFactory factory = BeanSerializerFactory.instance
            .withSerializerModifier(new GenericSerializerModifier());
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializerFactory(factory);
    String value = mapper.writeValueAsString(dataSource);
    assertThat(value.contains("\"url\":")).isTrue();
  }

  @Test
  void serializerWithMixin() throws Exception {
    DataSource dataSource = new DataSource();
    ObjectMapper mapper = new ObjectMapper();
    mapper.addMixIn(DataSource.class, DataSourceJson.class);
    String value = mapper.writeValueAsString(dataSource);
    assertThat(value.contains("\"url\":")).isTrue();
    assertThat(StringUtils.countOccurrencesOf(value, "\"url\"")).isEqualTo(1);
  }

  @JsonSerialize(using = TomcatDataSourceSerializer.class)
  interface DataSourceJson {

  }

  static class TomcatDataSourceSerializer extends JsonSerializer<DataSource> {

    private ConversionService conversionService = new DefaultConversionService();

    @Override
    public void serialize(DataSource value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      jgen.writeStartObject();
      for (PropertyDescriptor property : BeanUtils.getPropertyDescriptors(DataSource.class)) {
        Method reader = property.getReadMethod();
        if (reader != null && property.getWriteMethod() != null
                && this.conversionService.canConvert(String.class, property.getPropertyType())) {
          jgen.writeObjectField(property.getName(), ReflectionUtils.invokeMethod(reader, value));
        }
      }
      jgen.writeEndObject();
    }

  }

  static class GenericSerializerModifier extends BeanSerializerModifier {

    private ConversionService conversionService = new DefaultConversionService();

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties) {
      List<BeanPropertyWriter> result = new ArrayList<>();
      for (BeanPropertyWriter writer : beanProperties) {
        AnnotatedMethod setter = beanDesc.findMethod("set" + StringUtils.capitalize(writer.getName()),
                new Class<?>[] { writer.getType().getRawClass() });
        if (setter != null && this.conversionService.canConvert(String.class, writer.getType().getRawClass())) {
          result.add(writer);
        }
      }
      return result;
    }

  }

}
