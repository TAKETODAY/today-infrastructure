
## v1.0.1
> 除去字符编码只用UTF-8
    
## v1.2.0
> - 取消tags
> - 取消后缀
> - Java1.8 通过反射得到参数名称

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

## v2.2.3
> ### 修复ServletContext 注入过晚
> ### JSON.toJSON(invoke) -> JSON.toJSONString(invoke, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullListAsEmpty)

## v2.2.4

- requestMapping.setAction(clazz.getSimpleName());

## v2.3.0

- 修复 path variable 参数不匹配
- 增加 @Application注解，可注入ServletContxet Attribute
- 重构参数转换器

## v2.3.1
- 修复@Application 空指针
- 重构 `ViewDispatcher`
- 重构 `DispatcherServlet`
- 优化 path variable 参数注入


<br>
<br>
<br>
<br>
