/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.orm.mybatis.sample.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import cn.taketoday.orm.mybatis.SqlSessionFactoryBean;
import cn.taketoday.orm.mybatis.batch.MyBatisBatchItemWriter;
import cn.taketoday.orm.mybatis.batch.MyBatisCursorItemReader;
import cn.taketoday.orm.mybatis.batch.builder.MyBatisBatchItemWriterBuilder;
import cn.taketoday.orm.mybatis.batch.builder.MyBatisCursorItemReaderBuilder;
import cn.taketoday.orm.mybatis.sample.batch.UserToPersonItemProcessor;
import cn.taketoday.orm.mybatis.sample.domain.Person;
import cn.taketoday.orm.mybatis.sample.domain.User;
import cn.taketoday.batch.core.Job;
import cn.taketoday.batch.core.Step;
import cn.taketoday.batch.core.configuration.annotation.EnableBatchProcessing;
import cn.taketoday.batch.core.configuration.annotation.JobBuilderFactory;
import cn.taketoday.batch.core.configuration.annotation.StepBuilderFactory;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.convert.converter.Converter;
import cn.taketoday.core.io.support.PathMatchingResourcePatternResolver;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;
import cn.taketoday.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class SampleJobConfig {

  @Autowired
  private JobBuilderFactory jobBuilderFactory;

  @Autowired
  private StepBuilderFactory stepBuilderFactory;

  @Bean
  public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
        .addScript("org/mybatis/spring/sample/db/database-schema.sql")
        .addScript("org/springframework/batch/core/schema-drop-hsqldb.sql")
        .addScript("org/springframework/batch/core/schema-hsqldb.sql")
        .addScript("org/mybatis/spring/sample/db/database-test-data.sql").build();
  }

  @Bean
  public PlatformTransactionManager transactionalManager() {
    return new DataSourceTransactionManager(dataSource());
  }

  @Bean
  public SqlSessionFactory sqlSessionFactory() throws Exception {
    PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    SqlSessionFactoryBean ss = new SqlSessionFactoryBean();
    ss.setDataSource(dataSource());
    ss.setMapperLocations(resourcePatternResolver.getResources("org/mybatis/spring/sample/mapper/*.xml"));
    org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
    configuration.setDefaultExecutorType(ExecutorType.BATCH);
    ss.setConfiguration(configuration);
    return ss.getObject();
  }

  @Bean
  public MyBatisCursorItemReader<User> reader() throws Exception {
    // @formatter:off
    return new MyBatisCursorItemReaderBuilder<User>()
        .sqlSessionFactory(sqlSessionFactory())
        .queryId("cn.taketoday.orm.mybatis.sample.mapper.UserMapper.getUsers")
        .build();
    // @formatter:on
  }

  @Bean
  public UserToPersonItemProcessor processor() {
    return new UserToPersonItemProcessor();
  }

  @Bean
  public MyBatisBatchItemWriter<Person> writer() throws Exception {
    // @formatter:off
    return new MyBatisBatchItemWriterBuilder<Person>()
        .sqlSessionFactory(sqlSessionFactory())
        .statementId("cn.taketoday.orm.mybatis.sample.mapper.PersonMapper.createPerson")
        .itemToParameterConverter(createItemToParameterMapConverter("batch_java_config_user", LocalDateTime.now()))
        .build();
    // @formatter:on
  }

  public static <T> Converter<T, Map<String, Object>> createItemToParameterMapConverter(String operationBy,
      LocalDateTime operationAt) {
    return item -> {
      Map<String, Object> parameter = new HashMap<>();
      parameter.put("item", item);
      parameter.put("operationBy", operationBy);
      parameter.put("operationAt", operationAt);
      return parameter;
    };
  }

  @Bean
  public Job importUserJob() throws Exception {
    // @formatter:off
    return jobBuilderFactory.get("importUserJob")
        .flow(step1())
        .end()
        .build();
    // @formatter:on
  }

  @Bean
  public Step step1() throws Exception {
    // @formatter:off
    return stepBuilderFactory.get("step1")
        .transactionManager(transactionalManager())
        .<User, Person>chunk(10)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .build();
    // @formatter:on
  }

}
