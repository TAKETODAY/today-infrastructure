# TODAY Web

🍎 A Java library for building web applications

![Java8](https://img.shields.io/badge/JDK-8+-success.svg)
[![GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)
[![Author](https://img.shields.io/badge/Author-TODAY-blue.svg)](https://github.com/TAKETODAY)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/811723d275dc409ba6a823c9e08a5b3b)](https://app.codacy.com/app/TAKETODAY/today-web?utm_source=github.com&utm_medium=referral&utm_content=TAKETODAY/today-web&utm_campaign=Badge_Grade_Dashboard)
[![Java CI](https://github.com/TAKETODAY/today-web/workflows/Java%20CI/badge.svg)](https://github.com/TAKETODAY/today-web/actions)


## 背景
<details>
  <summary>先不看</summary>
<blockquote>
	<p>
  本人从2016年大一开始学Java，准确的说是高三最后的几周开始的. 果然兴趣是最好的老师， 在大一下学期自己独自一人从前端到后台写了我的个人网站：<a href="https://taketoday.cn">TODAY BLOG</a> 。 从注册域名到备案再到网站成功上线，我遇到过的困难数不计其数。因为感兴趣所以我坚持了下来。第一个版本使用的纯Servlet写的。后来了解到Java有很多开源框架可以简化我的开发。于是又投入到新一轮的学习之中...... 学了Struts2后自己学着写了一个小框架：<a href="https://gitee.com/TAKETODAY/today_web/tree/v1.1.1/">TODAY WEB</a>，几百行搞定从解析xml定义的action到处理对应的请求。学了Spring MVC后，我写了此项目：<a href="https://gitee.com/TAKETODAY/today_web">TODAY WEB 2.0</a>。
	</p>
</blockquote>
 
</details>

## 安装

```xml
<dependency>
    <groupId>cn.taketoday</groupId>
    <artifactId>today-web</artifactId>
    <version>2.3.6.RELEASE</version>
</dependency>
```
- [Maven Central](https://search.maven.org/artifact/cn.taketoday/today-web/2.3.6.RELEASE/jar)

## 案例
- [DEMO](https://github.com/TAKETODAY/today-web-demo)

## 文档
- [Wiki](https://gitee.com/I-TAKE-TODAY/today_web/wikis)

## 使用说明

### 函数式路由

```java
@Component
@ResponseBody
public class FunctionController {

    public String function(RequestContext request) {
        return "body:" + request.method() + " requestURI -> " + request.requestURI();
    }

    public String test(RequestContext request) {
        return "body:" + request.method() + " test -> " + request.requestURI();
    }

    public void script(RequestContext request) throws IOException {

        ModelAndView modelAndView = new ModelAndView();
        request.modelAndView(modelAndView);

        modelAndView.setContentType("text/html;charset=UTF-8");
        modelAndView.setView(new StringBuilder("<script>alert('HELLO， 你好 script');</script>"));
    }
}

@Configuration
//@EnableDefaultMybatis
//@EnableRedissonCaching
public class WebMvcConfig implements WebMvcConfiguration {

    @Autowired
    private FunctionController functionController;

    @Override
    public void configureFunctionHandler(FunctionHandlerRegistry registry) {

        registry.get("/function", functionController::function);
        registry.get("/function/test", functionController::test);
        registry.get("/function/script", functionController::script);

        registry.get("/function/error/500", (context) -> {
            context.sendError(500);
        });
    }
}
```

### 注解路由

```java
//@Controller
@RestController
@RequestMapping("/users")
public class UserController {
    

    @GET("index")
    @POST("post")
    @PUT("articles/{id}")
    ......
    @RequestMapping("/users/{id}")
    @RequestMapping(value = "/**", method = {RequestMethod.GET})
    @RequestMapping(value = "/*.html", method = {RequestMethod.GET})
    @RequestMapping(value = {"/index.action", "/index.do", "/index"}, method = RequestMethod.GET)
    @Interceptor({LoginInterceptor.class, ...})
    public (String|List<?>|Set<?>|Map<?>|void|File|Image|...) \\w+ (request, request, session,servletContext, str, int, long , byte, short, boolean, @Session("loginUser"), @Header("User-Agent"), @Cookie("JSESSIONID"), @PathVariable("id"), @RequestBody("users"), @Multipart("uploadFiles") MultipartFile[]) {
        service...
        return </>;
    }
}

```

### ViewController

```java
@Configuration
public class WebMvcConfig implements WebMvcConfiguration {

    @Override
    public void configureViewController(ViewControllerHandlerRegistry registry) {
        registry.addViewController("/github", "redirect:https://github.com");
        registry.addRedirectViewController("/login.do", "/login");
        registry.addViewController("/login.action")
                .setAssetsPath("redirect:/login");
    }
}

```

### 静态资源

```java
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

@Override
public void configureResourceHandler(ResourceHandlerRegistry registry) {

    registry.addResourceMapping(LoginInterceptor.class)//
            .setPathPatterns("/assets/admin/**")//
            .setOrder(Ordered.HIGHEST_PRECEDENCE)//
            .addLocations("/assets/admin/");
}
```

### 自定义参数转换器

```java
@Component 
public class DateConverter implements Converter<String, Date> {
    @Override
    public Date convert(String source) throws ConversionException {
        ...
    }
}
```

### 也可以通过xml文件配置简单视图

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Web-Configuration PUBLIC 
		"-//TODAY BLOG//Web - Configuration DTD 2.0//CN"
			"https://taketoday.cn/framework/web/dtd/web-configuration-2.3.3.dtd">

<Web-Configuration>

    <controller prefix="/error/">
        <action resource="400" name="BadRequest" status="400" />
        <action resource="403" name="Forbidden" status="403" />
        <action resource="404" name="NotFound" status="404" />
        <action resource="500" name="ServerIsBusy" status="500" />
        <action resource="405" name="MethodNotAllowed" status="405" />
    </controller>

    <controller>
        <action resource="redirect:http://pipe.b3log.org/blogs/Today" name="today-blog-pipe" />
        <action resource="redirect:https://taketoday.cn" name="today" />
        <action resource="redirect:https://github.com" name="github" />
        <action resource="redirect:/login" name="login.do" />
    </controller>

    <controller class="cn.taketoday.web.demo.controller.XMLController" name="xmlController" prefix="/xml/">
        <action name="obj" method="obj" />
        <action name="test" resource="test" method="test"/>
    </controller>

</Web-Configuration>
```
### 登录实例

```java
@Controller
public class UserController {

/* 
    <controller prefix="/WEB-INF/view/" suffix=".ftl">
        <action resource="login" name="login" />
        <action resource="register" name="register" />
    </controller> */
    
    // @GET("login")
    @RequestMapping(value = "/login" , method = RequestMethod.GET)
    public String login() {
        return "/login/login";//支持jsp,FreeMarker,Thymeleaf,自定义视图
    }
    
    @Logger("登录")
    //@POST("/login")
    //@RequestMapping(value = "/login" , method = RequestMethod.POST)
    @ActionMapping(value = "/login", method = RequestMethod.POST)
    public String login(HttpSession session, RedirectModel redirectModel, @Valid User user, Errors error) {
    
        if (error.hasErrors()) {
            System.err.println(error.getAllErrors());
            redirectModel.attribute("msg", error.getAllErrors().toString());
            return "redirect:/login";
        }
    
        User login = userService.login(user);
        if (login == null) {
            redirectModel.attribute("userId", user.getUserId());
            redirectModel.attribute("msg", "登录失败");
            return "redirect:/login";
        }
        redirectModel.attribute("msg", "登录成功");
        session.setAttribute(USER_INFO, login);
        return "redirect:/user/info";
    }
    
}
```

### 文件下载，支持直接返回给浏览器图片

```java
@RequestMapping(value = {"/download"}, method = RequestMethod.GET)
public File download(String path) {
    return new File(path);
}
```

```java
@GET("/display")
public final BufferedImage display(HttpServletResponse response) throws IOException {
    response.setContentType("image/jpeg");
    return ImageIO.read(new File("D:/taketoday.cn/webapps/upload/logo.png"));
}

@GET("captcha")
public final BufferedImage captcha(HttpServletRequest request) throws IOException {
    BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics graphics = image.getGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
    Graphics2D graphics2d = (Graphics2D) graphics;
    drawRandomNum(graphics2d, request);
    return image;
}
```

### 文件上传，支持多文件

```java
@RequestMapping(value = { "/upload" }, method = RequestMethod.POST)
public final String upload(@Multipart MultipartFile uploadFile) throws IOException {

    String upload = "D:/www.yhj.com/webapps/upload/";
    String path = upload + uploadFile.getFileName();
    File file = new File(path);
    uploadFile.save(file);

    return "/upload/" + uploadFile.getFileName();
}

@POST({"/upload/multi"})
public final String multiUpload(HttpServletResponse response, @Multipart MultipartFile[] files) throws IOException {

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


## 🙏 鸣谢
本项目的诞生离不开以下开源项目：
* [Freemarker](https://github.com/apache/freemarker): Apache Freemarker
* [Slf4j](https://github.com/qos-ch/slf4j): Simple Logging Facade for Java
* [Spring](https://github.com/spring-projects/spring-framework): Spring Framework
* [EL](https://github.com/TAKETODAY/today-expression): Java Unified Expression Language
* [FastJSON](https://github.com/alibaba/fastjson): A fast JSON parser/generator for Java
* [Lombok](https://github.com/rzwitserloot/lombok): Very spicy additions to the Java programming language
* [Today Context](https://github.com/TAKETODAY/today-context): A Java library for dependency injection and aspect oriented programing
* [Hibernate Validator](https://github.com/hibernate/hibernate-validator): Hibernate Validator - Bean Validation 2.0 (JSR 380) Reference Implementation


## 📄 开源协议
请查看 [GNU GENERAL PUBLIC LICENSE](https://github.com/TAKETODAY/today-web/blob/master/LICENSE)

