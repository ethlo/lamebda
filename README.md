# Lamebda
[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.lamebda/lamebda.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.ethlo.lamebda%22)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](LICENSE)
[![Build Status](https://travis-ci.org/ethlo/lamebda.svg?branch=master)](https://travis-ci.org/ethlo/lamebda)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/598913bc1fe9405c82be73d9a4f105c8)](https://www.codacy.com/app/ethlo/lamebda?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ethlo/lamebda&amp;utm_campaign=Badge_Grade)

![Lamebda ](logo.svg)
<img src="logo.svg">
Simple HTTP handler supporting dynamic loading of HTTP handler functions. Intended for running within your existing infrastructure as a gateway or integration layer embedded with your current framework like, but not limited to, Spring MVC or Spring Flux.

> It Really Whips The Lambda's Ass!

## Example use-cases

* Ad-hoc API services and integration projects - Changes in the integration code can evolve freely from the core service
* Transactional support across multiple API calls
* Batch multiple API calls to avoid chatty data exchange
* API prototyping
* A powerful support tool for extracting data or changing state (think JMX on steroids)

## Getting started

### Integrating with your project

NOTE: If you want to try the bleeding edge, please add this snapshot repository
```xml
<repository>
  <id>sonatype-snapshots</id>     
  <snapshots><enabled>true</enabled></snapshots>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
</repository>
```

#### Usage with Spring Boot and Spring MVC

Add dependency to your `pom.xml`
```xml
<dependency>
    <groupId>com.ethlo.lamebda</groupId>
    <artifactId>lamebda-spring-web-starter</artifactId>
    <version>0.6.0</version>
</dependency>
```

Add the following properties to `application.properties`:
```properties
lamebda.enabled=true
lamebda.source.directory=/var/lib/lamebda
lamebda.request-path=/gateway
```

#### Invocation/delegation from a standard HttpServlet

```xml
<dependency>
    <groupId>com.ethlo.lamebda</groupId>
    <artifactId>lamebda-servlet</artifactId>
    <version>0.6.0</version>
</dependency>
```

```java
public class MyLamebdaServlet implements HttpServlet
{
    String contextPath = "/servlet";
    String sourceDir = "/var/lib/lamebda";
    final ClassResourceLoader classResourceLoader = new FileSystemClassResourceLoader(f->f, sourceDir);

    @Override
    public void service(HttpServletrequest req, HttpServletResponse res)
    {
        final HttpRequest request = new ServletHttpRequest(contextPath, request);
        final HttpResponse ressponse = new ServletHttpResponse(response);
        functionManager.handle(request, response);
    }
}
```

### Directory structure
* A root folder, example: `/var/lib/lamebda`
* Project directory, example: `var/lib/lamebda/test`
* A Project configuration file: `/var/lib/lamebda/project.properties`

### Project configuration
* `project.name` - Human-readable name of the project
* `project.version` - The project version  
* `mapping.project-context-path` - Project context path. Default is the project directory name.
* `mapping.use-project-context-path` - Whether to prepend the request path with the project context name, i.e `/servlet/gateway/<project>/my-function`
* `function.static.enabled` - Turn static content serving on/off. Default `true`
* `function.static.prefix` - The path that static content is served under, default `static`
* `function.static.path` - The folder that static content is served from, default is `<project-dir>/static`

### Extra preparations if you have an Open API Specification (OAS) file

1. Create a home directory for Lamebda. We will use `/var/lib/lamebda` as an example.

2. Put your API specification file and put it in location `/var/lib/lamebda/specification/oas.yaml`

3. Create directory `.generator` in the root folder, so we have `/var/lib/lamebda/.generator`. 

4. Download [`openapi-generator-cli`](http://central.maven.org/maven2/org/openapitools/openapi-generator-cli/3.3.4/openapi-generator-cli-3.3.4.jar) and [`groovy-models`](https://repo1.maven.org/maven2/com/ethlo/openapi-tools/groovy-models/0.1/groovy-models-0.1.jar) and put it into the `.generator` folder.

### Setup a project folder
Create a project folder. This folder is a logical grouping for your API functions. We will go with the create `test` for now. We now have the folder `/var/lib/lamebda/test`.


### Add your first script
1. Create a folder for the functions in the test project: `/var/lib/lamebda/test/scripts`

2. Add a simple script in the scripts folder:

```groovy
class MyFunction extends SimpleServerFunction {
    @Override
    void get(HttpRequest request, HttpResponse response) {
        response.json(HttpStatus.OK, [requestmethod: request.method, message:'Hello world'])
    }
}
```

### Access your first function

Your function should be available under `/gateway/test/my-function`

### Built in functions

* /servet/gateway/test/status/ - Simple status page (by default) requiring authentication specified with `admin.credentials.username` and `admin.credentials.password` in the `project.properties` file.

## Using pre-compilation

Using pre-compilation the notion of scripts dissapear. Lamebda is then using classpath scanning for implementations of `ServerFunction`.

### Building
If the project is part of a build system it is easy to pre-compile the scripts and run OpenAPI model and human redable documentation generation. I recommend using Gradle. Below is a sample script showing how you can compile and package your project.
```groovy
plugins {
    id 'java'
    id 'groovy'
    id 'idea'
    id "com.gorylenko.gradle-git-properties" version "2.0.0"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots'
    }
}

configurations{
    genDeps
}

dependencies{
    genDeps 'org.openapitools:openapi-generator-cli:3.3.4', 'com.ethlo.openapi-tools:groovy-models:0.1'
}

task executeModelGen(type:JavaExec, group: "Build") {
    def generateModelsCmd = ['generate', "-ispecification/oas.yaml", '-gcom.ethlo.openapi.GroovyModelGenerator', "-otarget/generated-sources/models", '-Dmodels', '-DdateLibrary=java8', '--model-package=spec', '-DuseSwaggerAnnotations=false']
    main 'org.openapitools.codegen.OpenAPIGenerator'
    args generateModelsCmd
    classpath = configurations.genDeps

    doLast {
        new File("$projectDir/.models.gen").text = generateModelsCmd.join(' ')
    }
}

task executeApiDocGen(type:JavaExec, group: "Build") {
    def generateApiDocCmd = ['generate', "-ispecification/oas.yaml", '-ghtml', "-otarget/api-doc"]
    main 'org.openapitools.codegen.OpenAPIGenerator'
    args generateApiDocCmd
    classpath = configurations.genDeps

    doLast {
        new File("$projectDir/.apidoc.gen").text = generateApiDocCmd.join(' ')
    }
}

task copyGeneratorJars(type: Copy) {
    from configurations.genDeps
    into "$projectDir/.generator"
}

tasks.findByName('executeModelGen').dependsOn tasks.findByName('copyGeneratorJars')
tasks.findByName('compileGroovy').dependsOn tasks.findByName('executeApiDocGen')
tasks.findByName('compileGroovy').dependsOn tasks.findByName('executeModelGen')

sourceSets {
    main {
        groovy {

            srcDirs = [
                    "$projectDir/scripts",
                    "$projectDir/shared",
                    "$projectDir/target/generated-sources/models"
            ]
        }
        resources {
            srcDirs= ["$projectDir/resources"]
        }
    }
    test {
        java {
            srcDirs = ["$projectDir/tests"]
        }
        resources {
            srcDirs = ["$projectDir/tests-resources"]
        }
    }

    custom{}
}

task tJar(type: Jar, dependsOn: compileJava) {
    from sourceSets.main.output
    archivesBaseName = 'compiled'
}

dependencies {
    compile 'org.codehaus.groovy:groovy-all:2.5.4'

    testCompile 'junit:junit:4.12'
    testCompile 'org.mockito:mockito-core:2.22.0'
    testCompile 'org.assertj:assertj-core:3.11.1'

    compile 'com.ethlo.lamebda:lamebda-springmvc:0.6.3-SNAPSHOT'
    compile 'org.openapitools:openapi-generator:3.3.4'
}

gitProperties {
    extProperty = 'gitProps'
    dateFormat = "yyyyMMdd'T'HHmmss'Z'"
    dateFormatTimeZone = "Z"
}
// make sure the generateGitProperties task always executes (even when git.properties is not changed)
generateGitProperties.outputs.upToDateWhen { false }

task distZip( type: Zip) {
    from ("$projectDir/resources") {
        into ('resources/')
    }
    from ("$projectDir/target/api-doc") {
        into ('target/api-doc')
    }
    from ("$projectDir/specification") {
        into ('specification/')
    }
    from ("$projectDir/lib") {
        into ('lib/')
    }
    from ("$projectDir/build/libs") {
        into ('lib/')
    }
    from ("$projectDir/config.properties") {
        into ('')
    }
    from ("$projectDir/project.properties") {
        into ('')
    }
}

distZip {
    doLast {
        file("$destinationDir/$archiveName").renameTo("$destinationDir/test-" + project.ext.gitProps['git.branch'] + '-' + project.ext.gitProps['git.commit.time'] + '-' + project.ext.gitProps['git.commit.id.abbrev'] + ".jar")
    }
}
```

### Deploying
The jar file must be named the same as the project folder, i.e. `/var/lib/lamebda/test/test.jar`
