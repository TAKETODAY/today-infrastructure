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

package cn.taketoday.cache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.redisson.api.RedissonClient;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.CachePut;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.RedisRunner.FailedToStartRedisException;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 11:19
 */
@Disabled
class RedissonCacheTests {

  public static class SampleObject implements Serializable {

    private String name;
    private String value;

    public SampleObject() {
    }

    public SampleObject(String name, String value) {
      super();
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

  }

  @Service
  public static class SampleBean {

    @CachePut(cacheNames = "testMap", key = "#key")
    public SampleObject store(String key, SampleObject object) {
      return object;
    }

    @CachePut(cacheNames = "testMap", key = "#key")
    public SampleObject storeNull(String key) {
      return null;
    }

    @CacheEvict(cacheNames = "testMap", key = "#key")
    public void remove(String key) {
    }

    @Cacheable(cacheNames = "testMap", key = "#key")
    public SampleObject read(String key) {
      throw new IllegalStateException();
    }

    @Cacheable(cacheNames = "testMap", key = "#key")
    public SampleObject readNull(String key) {
      return null;
    }

  }

  @Configuration
  @ComponentScan
  @EnableCaching
  public static class Application {

    @Bean(destroyMethod = "shutdown")
    RedissonClient redisson() {
      return BaseTest.createInstance();
    }

    @Bean
    CacheManager cacheManager(RedissonClient redissonClient) throws IOException {
      Map<String, CacheConfig> config = new HashMap<String, CacheConfig>();
      config.put("testMap", new CacheConfig(24 * 60 * 1000, 12 * 60 * 1000));
      return new RedissonCacheManager(redissonClient, config);
    }

  }

  @Configuration
  @ComponentScan
  @EnableCaching
  public static class JsonConfigApplication {

    @Bean(destroyMethod = "shutdown")
    RedissonClient redisson() {
      return BaseTest.createInstance();
    }

    @Bean
    CacheManager cacheManager(RedissonClient redissonClient) throws IOException {
      return new RedissonCacheManager(redissonClient, "classpath:/org/redisson/spring/cache/cache-config.json");
    }

  }

  private static Map<Class<?>, AnnotationConfigApplicationContext> contexts;

  public static List<Class<?>> data() {
    return Arrays.asList(Application.class, JsonConfigApplication.class);
  }

  @BeforeAll
  public static void before() throws FailedToStartRedisException, IOException, InterruptedException {
    RedisRunner.startDefaultRedisServerInstance();
    contexts = data().stream().collect(Collectors.toMap(e -> e, AnnotationConfigApplicationContext::new));
  }

  @AfterAll
  public static void after() throws InterruptedException {
    contexts.values().forEach(AbstractApplicationContext::close);
    RedisRunner.shutDownDefaultRedisServerInstance();
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testNull(Class<?> contextClass) {
    AnnotationConfigApplicationContext context = contexts.get(contextClass);
    SampleBean bean = context.getBean(SampleBean.class);
    bean.store("object1", null);
    assertThat(bean.readNull("object1")).isNull();
    bean.remove("object1");
    assertThat(bean.readNull("object1")).isNull();
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testRemove(Class<?> contextClass) {
    AnnotationConfigApplicationContext context = contexts.get(contextClass);
    SampleBean bean = context.getBean(SampleBean.class);
    bean.store("object1", new SampleObject("name1", "value1"));
    assertThat(bean.read("object1")).isNotNull();
    bean.remove("object1");
    assertThat(bean.readNull("object1")).isNull();
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testPutGet(Class<?> contextClass) {
    AnnotationConfigApplicationContext context = contexts.get(contextClass);
    SampleBean bean = context.getBean(SampleBean.class);
    bean.store("object1", new SampleObject("name1", "value1"));
    SampleObject s = bean.read("object1");
    assertThat(s.getName()).isEqualTo("name1");
    assertThat(s.getValue()).isEqualTo("value1");
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testGet(Class<?> contextClass) {
    Assertions.assertThrows(IllegalStateException.class, () -> {
      AnnotationConfigApplicationContext context = contexts.get(contextClass);
      SampleBean bean = context.getBean(SampleBean.class);
      bean.read("object2");
    });
  }
}
