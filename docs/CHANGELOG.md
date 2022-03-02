# TODAY Web CHANGE LOG

:apple: today-web is a servlet based high-performance lightweight web framework

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/811723d275dc409ba6a823c9e08a5b3b)](https://app.codacy.com/app/TAKETODAY/today-web?utm_source=github.com&utm_medium=referral&utm_content=TAKETODAY/today-web&utm_campaign=Badge_Grade_Dashboard)

## 背景
<details>
  <summary>先不看</summary>
<blockquote>
    <p>
  本人大学生一枚，专业是电子信息工程，大三在读。大一开始学Java，准确的说是高三最后的几周开始的. 果然兴趣是最好的老师， 在大一下学期自己独自一人从前端到后台写了我的个人网站：<a href="https://taketoday.cn">TODAY BLOG</a> 。 从注册域名到备案再到网站成功上线，我遇到过的困难数不计其数。因为感兴趣所以我坚持了下来。第一个版本使用的纯Servlet写的。后来了解到Java有很多开源框架可以简化我的开发。于是又投入到新一轮的学习之中...... 学了Struts2后自己学着写了一个小框架：<a href="https://gitee.com/TAKETODAY/today_web/tree/v1.1.1/">TODAY WEB</a>，几百行搞定从解析xml定义的action到处理对应的请求。学了Spring MVC后，我写了此项目：<a href="https://gitee.com/TAKETODAY/today_web">TODAY WEB 2.0</a>。
    </p>
</blockquote>
 
</details>

## 安装

```xml
<dependency>
    <groupId>cn.taketoday</groupId>
    <artifactId>today-web</artifactId>
    <version>3.0.0.RELEASE</version>
</dependency>
```
- [Maven Central](https://search.maven.org/artifact/cn.taketoday/today-web/2.3.6.RELEASE/jar)

## v3.0.3
- fix request body and data-binder

## v3.0.2
- :zap: 使用 WebMvcAutoConfiguration 自动配置 以前在 beans 文件的组件
- :zap: 优化异常处理器 applyResponseStatus
- :bug: fix #15 JacksonConfiguration 重复实例
- :bug: fix #14 JacksonConfiguration ObjectMapper fails on empty beans
- :zap: 访问控制
- :zap: 优化 servlet 参数注册部分
- :arrow_up: Bump hibernate-validator from 6.1.0.Final to 6.1.5.Final dependabot[bot]* 2021/6/5, 05:54

## v3.0.0(v2.3.7)
- :sparkles: feat: allow rebuild Controllers
- :sparkles: :zap: add WebUtils api
- :sparkles: feat: status feature
- :sparkles: feat: `@Interceptor` exclude feature
- :bug: fix: applicationContext NullPointerException
- refactor: refactor DispatcherServlet
- unify date format
- :sparkles: feat: `WebResource` feature
- :sparkles: feat: use `WebMvcConfiguration` to config web mvc
- :sparkles: feat: use Spring path matcher to match static resource
- :sparkles: feat: use `ResourceServlet` to handle static resource request
- :sparkles: feat: static Resource supports Interceptor
- refactor: extract comment code

- :bug: fix: #5 Request Body Read Error
- :bug: fix: #6 `ResourceServlet` can't resolve Chinese Url

- :sparkles: feat: config `ViewResolver`
- :sparkles: feat: add `ControllerAdvice`,`ExceptionHandler`
- :bug: fix: #7 filter can't be null
- :sparkles: feat: use `ControllerAdviceExceptionResolver` as default `ExceptionResolver`
- refactor: refactor `HandlerInterceptor`
- :hammer: :zap: refactoring `ParameterResolver` to improve performance
- :hammer: add `ResultResolver` to resolve handler result
- :sparkles: feat: add validation feature
- :sparkles: feat: add `SharedVariable`
- :sparkles: feat: supports prototype controller
- :sparkles: feat: 添加TemplateLoader，可配置多个模板加载位置
- :hammer: 重构HandlerMapping
- :hammer: 重构应用启动逻辑
- :hammer: 重构拦截器
- :hammer: 重构处理器映射机制
- :hammer: 重大重构
	1. :fire: 舍弃ResultUtils
	2. :art: 更正设计错误（将ResultResolver改成ViewResolver）
	3. :art: 更正设计错误（将ViewResolver改成TemplateViewResolver）
	4. :zap: 优化默认模板处理器DefaultTemplateViewResolver（原名DefaultViewResolver）
	5. :zap: 优化WebApplicationLoader
	6. :art: 更正了类名称和包名
- :hammer: 重大重构
	1. :fire: 舍弃WebMapping
	2. :sparkles: 添加WebServletApplicationContextSupport
	3. :fire: 舍弃ViewDispatcher修改为ViewController
	4. :sparkles: 添加NotFoundRequestAdapter处理handler找不到的情况
	5. :zap: 优化ModelAndView
	6. :sparkles: MimeType
	7. :sparkles: MediaType
	8. :sparkles: 使用HandlerRegistry映射handler
	9. :sparkles: 使用HandlerAdapter适配handler
	10. :zap: 优化HandlerInterceptor
	11. :fire: 删除HandlerInterceptorRegistry
	12. :zap: 优化HandlerMethod
	13. :sparkles: 添加ResultHandler
	14. :sparkles: 使用PatternMapping映射
- :zap: 取消自动使用DispatcherServletInitializer注册DispatcherServlet
- :hammer: 重构ExceptionResolver 修改ExceptionResolver为HandlerExceptionHandler更正设计错误

- :sparkles: 加入 WebSocket

## v2.3.6
- add new today-context version
- :sparkles: feat: auto register `Servlet`,`Filter`,`Listener`


## v2.3.5
- add new today-context version
- adjust: adjust `DispatcherServlet`.`destroy()`
- :sparkles: feat: add `Reader`, `Writer`,request `Locale`, `OutputStream`, `InputStream`, `java.security.Principal`. parameter types


## v2.3.4
- Sync to maven Central
- Waiting for Jetty 10.0.0

## v2.3.3
- fix path variable不能匹配中文字符
- view add content type 
- fix #3 upload file not found exception
- add some test code

## v2.3.2
- fix #1
- fix #2 JSONObject could be null
- use `HandlerMappingRegistry` instead of `HandlerMappingPool`

## v2.3.1
![LOGO](https://taketoday.cn/display.action?userId=666)
- 修复@Application 空指针
- [重构 `ViewDispatcher`](/src/main/java/cn/taketoday/web/servlet/ViewDispatcher.java)
- [重构 `DispatcherServlet`](/cn/taketoday/web/servlet/DispatcherServlet.java)
- [优化 path variable 参数注入](/cn/taketoday/web/bind/resolver/DefaultParameterResolver.java#L337)
- [修复exception resolver InvocationTargetException](/cn/taketoday/web/bind/resolver/DefaultExceptionResolver.java#L49)
- [优化requestBody注解参数注入](/cn/taketoday/web/bind/resolver/DefaultParameterResolver.java#L304)
- [优化MultipartResolver](/src/main/java/cn/taketoday/web/multipart/CommonsMultipartResolver.java#L80)
- [update web-configuration-2.3.0.dtd](/src/main/resources/web-configuration-2.3.0.dtd)
- [增加WebMvcConfigLocation，自定义web-mvc配置文件路径，加快启动速度](/cn/taketoday/web/Constant.java#L51)
- 去掉Optional类型参数

## v2.3.0

- 修复 path variable 参数不匹配
- 增加 @Application注解，可注入ServletContxet Attribute
- 重构参数转换器


## v2.2.4
- requestMapping.setAction(clazz.getSimpleName());


## v2.2.3
> ### 修复ServletContext 注入过晚
> ### JSON.toJSON(invoke) -> JSON.toJSONString(invoke, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullListAsEmpty)


## v2.2.2
> ### `HandlerMapping` private Class<?> actionProcessor 改为 private String action
> ### 更改 `Freemarker` 版本 解决 `AllHttpScopesHashModel` 构造函数不可视问题, 去掉 `AllScopesModel` 类
> ### 添加单元测试
> ### 优化拦截器
> ### 添加 web-configuration-2.2.0.dtd
> ### 改进freemarker view
> ### 修改@RequestParam 逻辑
> ### 启用defaultValue
> ### 优化参数转换器
> ### @ActionProcessor @RestProcessor 改名 @Controller @RestController @RequestMapping
> ### 更改包名cn.taketoday.web.core -> cn.taketoday.web

## v2.2.1 加入`@PathVariable` `Optional<T>`
    
```java
@RestProcessor
public final class OptionalAction {

    public OptionalAction() {
    
    }

    @GET("/optional")
    public String optional(Optional<String> opt) {
        
        opt.ifPresent(opts -> {
            System.out.println(opts);
        });
            
        return "Optional";
    }
}
@RestProcessor
public final class PathVariableAction {

    @ActionMapping(value = {"/path/{id}"}, method = RequestMethod.GET)
    public String pathVariable(@PathVariable Integer id) {
        
    return "id -> " + id;
    }
    
    @ActionMapping(value = {"/p/**/yhj.html"}, method = RequestMethod.GET)
    public String path() {
        
    return "/path/**";
    }
    
    @ActionMapping(value = {"/pa/{i}"}, method = RequestMethod.GET)
    public String path(@PathVariable Integer i) {
        
    return "/path/"+ i;
    }
    
    @ActionMapping(value = {"/paths/{name}"}, method = RequestMethod.GET)
    public String path(@PathVariable String name) {
        return name;
    }

    @ActionMapping(value = {"/path/{name}/{id}.html"}, method = RequestMethod.GET)
    public String path_(@PathVariable String name,@PathVariable Integer id) {
        return "name -> " + name + "/id -> " + id;
    }
    
    @ActionMapping(value = {"/path/{name}/{id}-{today}.html"}, method = RequestMethod.GET)
    public String path_(@PathVariable String name,@PathVariable Integer id, @PathVariable Integer today) {
        return "name -> " + name + "/id -> " + id + "/today->" + today;
    }
    
}

```



## v2.0 `2018-06-26`
> ### 优化架构，简化配置几乎可以做到零配置
> ### 请求参数支持基本数据类型数组
> ### 请求参数支持简单POJO参数注入
> ### 支持List参数，Set参数，Map参数注入
> ### 配置自定义参数转换器
> 添加 `@ParameterConverter` 注解并实现 `Converter` 的 `doConverter` 方法

### 配置控制器
```java
@GET("index")
@POST("post")
......
@ActionMapping("/users/{id}")
@ActionMapping(value = "/users/**", method = {RequestMethod.GET})
@ActionMapping(value = "/users/*.html", method = {RequestMethod.GET})
@ActionMapping(value = {"/index.action", "/index.do", "/index"})
@Interceptor({LoginInterceptor.class, ...})
public (String|List<?>|Set<?>|Map<?>|void|File|Image|...) \\w+ (request, response, session,servletContext, str, int, long , byte, short, boolean, @Session("loginUser"), @Header("User-Agent"), @Cookie("JSESSIONID"), @PathVariable("id"), @RequestBody("users"), @Multipart("uploadFiles") MultipartFile[]) {
        
    return </>;
}
```



### 加入文件上传，支持多文件
```java
@ResponseBody
@ActionMapping(value = { "/upload" }, method = RequestMethod.POST)
public String upload(HttpServletRequest request, HttpSession session, @Multipart MultipartFile uploadFile)
        throws IOException {

    String upload = "D:/www.yhj.com/webapps/upload/";
    String path = upload + uploadFile.getFileName();
    File file = new File(path);
    uploadFile.save(file);

    return "/upload/" + uploadFile.getFileName();
}

@ResponseBody
@ActionMapping(value = { "/upload/multi" }, method = RequestMethod.POST)
public String multiUpload(HttpServletRequest request, HttpSession session, HttpServletResponse response,
        @Multipart("files") MultipartFile[] files) throws IOException {

    String upload = "D:/www.yhj.com/webapps/upload/";

    for (MultipartFile multipartFile : files) {
        String path = upload + multipartFile.getFileName();
        File file = new File(path);
        System.out.println(path);
        if (!multipartFile.save(file)) {
            return "<script>alert('upload error !')</script>";
            //response.getWriter().print("<script>alert('upload error !')</script>");
        }
    }
    //response.getWriter().print("<script>alert('upload success !')</script>");
    return "<script>alert('upload success !')</script>";
}
```

### 支持文件下载 ， 支持直接返回给浏览器图片

```java
@ActionMapping(value = {"/download"}, method = RequestMethod.GET)
public final File download(String path) {
    return new File(path);
}
```
```java
@GET("/display")
public final BufferedImage display(HttpServletResponse response) throws IOException {
    response.setContentType("image/jpeg");
    return ImageIO.read(new File("D:/www.yhj.com/webapps/upload/logo.png"));
}

@GET("captcha")
public final BufferedImage captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
    BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics graphics = image.getGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
    Graphics2D graphics2d = (Graphics2D) graphics;
    this.drawRandomNum(graphics2d, request);
    return image;
}
```
### 精确配置静态资源, 文件上传, 视图

```xml
<Web-Configuration>
    <!-- Servlet mapping -->
    <static-resources mapping="/assets/*" />
    
    <!-- <multipart class="cn.taketoday.web.multipart.DefaultMultipartResolver"> 或者自定义-->
    <multipart class="cn.taketoday.web.multipart.CommonsMultipartResolver">
        <upload-encoding>UTF-8</upload-encoding>
        <!-- <upload-location>D:/upload</upload-location> -->
        <upload-maxFileSize>10240000</upload-maxFileSize>
        <upload-maxRequestSize>1024000000</upload-maxRequestSize>
        <upload-fileSizeThreshold>1000000000</upload-fileSizeThreshold>
    </multipart>

    <!-- 默认-> <view-resolver class="cn.taketoday.web.view.JstlViewResolver"> 可以自定义-->
    <view-resolver class="cn.taketoday.web.view.FreeMarkerViewResolver">
        <view-suffix>.ftl</view-suffix>
        <view-encoding>UTF-8</view-encoding>
        <view-prefix>/WEB-INF/view</view-prefix>
    </view-resolver>


    <common prefix="/WEB-INF/error/" suffix=".jsp">
        <view res="400" name="BadRequest" />
        <view res="403" name="Forbidden" />
        <view res="404" name="NotFound" />
        <view res="500" name="ServerIsBusy" />
        <view res="405" name="MethodNotAllowed" />
    </common>

</Web-Configuration>
```

## v2.2.0 加入 IOC 取消 ConfigurationFactory
> IOC容器：[today-context](https://gitee.com/TAKETODAY/today_context)


## v1.2.0
> - 取消tags
> - 取消后缀
> - Java1.8 通过反射得到参数名称


## v1.0.1
> 除去字符编码只用UTF-8
    

