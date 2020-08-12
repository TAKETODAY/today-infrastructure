# TODAY framework
![Java8](https://img.shields.io/badge/JDK-8+-success.svg)
![GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)
[![Author](https://img.shields.io/badge/Author-TODAY-blue.svg)](https://github.com/TAKETODAY)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3fc111bcdf694f96bbf1a063058eea36)](https://app.codacy.com/app/TAKETODAY/today-framework?utm_source=github.com&utm_medium=referral&utm_content=TAKETODAY/today-framework&utm_campaign=Badge_Grade_Settings)


## 开始

```java

@Slf4j
@Configuration
@RequestMapping
@ContextListener
@EnableHotReload
@EnableDefaultMybatis
@EnableRedissonCaching
@Import({ TomcatServer.class })
@ComponentScan("cn.taketoday.blog")
@PropertiesSource("classpath:info.properties")
@MultipartConfig(maxFileSize = 10240000, fileSizeThreshold = 1000000000, maxRequestSize = 1024000000)
public class TestApplication implements WebMvcConfiguration, ApplicationListener<ContextStartedEvent> {

    public static void main(String[] args) {
        WebApplication.run(TestApplication.class, args);
    }

    @GET("index/{q}")
    public String index(String q) {
        return q;
    }
    
    @Singleton
    @Profile("prod")
    public ResourceHandlerRegistry prodResourceMappingRegistry() {

        final ResourceHandlerRegistry registry = new ResourceHandlerRegistry();

        registry.addResourceMapping(LoginInterceptor.class)//
                .setPathPatterns("/assets/admin/**")//
                .setOrder(Ordered.HIGHEST_PRECEDENCE)//
                .addLocations("/assets/admin/");

        return registry;
    }
    
	@Singleton
    @Profile("dev")
    public ResourceHandlerRegistry devRsourceMappingRegistry(@Env("site.uploadPath") String upload,
                                                             @Env("site.assetsPath") String assetsPath) //
    {
        final ResourceHandlerRegistry registry = new ResourceHandlerRegistry();

        registry.addResourceMapping("/assets/**")//
                .addLocations(assetsPath);

        registry.addResourceMapping("/upload/**")//
                .addLocations(upload);

        registry.addResourceMapping("/logo.png")//
                .addLocations("file:///D:/dev/www.yhj.com/webapps/assets/images/logo.png");

        registry.addResourceMapping("/favicon.ico")//
                .addLocations("classpath:/favicon.ico");

        return registry;
    }

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {
        log.info("----------------Application Started------------------");
    }
}
```

## 🙏 鸣谢
本项目的诞生离不开以下开源项目：
* [Slf4j](https://github.com/qos-ch/slf4j): Simple Logging Facade for Java
* [EL](https://github.com/TAKETODAY/today-expression): Java Unified Expression Language
* [Lombok](https://github.com/rzwitserloot/lombok): Very spicy additions to the Java programming language
* [FastJSON](https://github.com/alibaba/fastjson): A fast JSON parser/generator for Java
* [Freemarker](https://github.com/apache/freemarker): Apache Freemarker
* [Apache Commons FileUpload](https://github.com/apache/commons-fileupload): Apache Commons FileUpload
* [Netty](https://github.com/netty/netty): Netty project - an event-driven asynchronous network application framework
* [Jetty](https://github.com/eclipse/jetty.project): Eclipse Jetty® - Web Container 
* [Tomcat](https://github.com/apache/tomcat): Apache Tomcat
* [Undertow](https://github.com/undertow-io/undertow): High performance non-blocking webserver
* [Today Web](https://github.com/TAKETODAY/today-web): A Java library for building web applications
* [Today Context](https://github.com/TAKETODAY/today-context): A Java library for dependency injection and aspect oriented programing



## 📄 开源协议
请查看 [GNU GENERAL PUBLIC LICENSE](https://github.com/TAKETODAY/today-framework/blob/master/LICENSE)

