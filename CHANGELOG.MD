
1.0.1
	除去字符编码只用UTF-8
	
1.2.0
	取消tags
	取消后缀
	
	
Java1.8
	通过反射得到参数名称


###	配置请求

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

###	请求参数支持基本数据类型数组
###	请求参数支持简单POJO参数注入

### 加入自定义参数转换器
    添加 @ParameterConverter 注解并实现 Converter 的 doConverter 方法



## 2018 06 26 web v2.0
	优化架构，简化配置几乎可以做到零配置

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
