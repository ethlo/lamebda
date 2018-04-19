# lamebda
Simple functional processing inspired by ServerLess and AWS Lambda, but can also running within you existing infrastructure as a gateway or integration layer.

### Usage from Spring Web Controller
```java
@Configuration
public class LamebdaCfg
{
    private static final String PATH = "/lamebda";
    private static final String PATH_PATTERN = PATH + "/**";
    
    @Bean
    public FunctionManager functionManager(ApplicationContext applicationContext)
    {
        String contextPath = "/servlet"
        String sourceDir = "/var/lib/lamebda/scripts"
        return new FunctionManager(new FileSystemClassResourceLoader(applicationContext, sourceDir));
    }
    
    @Controller
    public class LamebdaController
    {
        @Autowired
        private FunctionManager functionManager;
        
        @RequestMapping(PATH_PATTERN)
        public void handle(HttpServletRequest request, HttpServletResponse response)
        {
            final HttpRequest req = new ServletHttpRequest(PATH, request);
            final HttpResponse res = new ServletHttpResponse(response);
            functionManager.handle(req, res);
        }
    }
}

```

### Example script
```groovy
class MyFunction extends SimpleServerFunction {

    @Autowired
    private MyService service;

    def MyFunction() {
        super("/mine/**")
    }

    @Override
    void get(HttpRequest request, HttpResponse response) {
        response.json(200, [method: request.method, message:'Hello world'])
    }

    @Override
    void post(HttpRequest request, HttpResponse response) {
      def json = request.json()
      def prop = json.myprop
      def id = service.register(prop)
      response.json(200, [id: id])
    }
}
```
