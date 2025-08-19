
<img alt="Lamebda logo" src="./logo.svg" width="230" style="background:white; padding-right:10px; padding-left:20px; margin-right: 20px; float:left; fill:white">

[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.lamebda/lamebda.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.ethlo.lamebda%22)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](LICENSE)

A simple, powerful, plugin system for adding new API endpoints or custom functionality to you Spring project. 
Oftentimes there are conflicting requirements when utilizing a common main application. The required custom functionality needed is often easy to do with a few lines of code, but is not acceptable to add to your main code-base. With Lamebda you can load libraries with custom functionality.

> It Really Whips The Lambda's Ass!

## Example use-cases

* Ad-hoc API services and integration projects - Changes in the integration code can evolve freely from the core service
* Transactional support across multiple API calls
* Batch multiple API calls to avoid chatty data exchange
* API prototyping
* A powerful support tool for extracting data or changing state.

## Getting started

### Integrating with your project


#### Usage with Spring Boot and Spring MVC

Add dependency to your `pom.xml`
```xml
<dependency>
    <groupId>com.ethlo.lamebda</groupId>
    <artifactId>lamebda-spring-web-starter</artifactId>
    <version>3.0.0</version>
</dependency>
```

Add the following properties to your applications `application.properties|yaml`:
```properties
lamebda.enabled=true
lamebda.root-directory=/var/lib/lamebda
lamebda.request-path=/gateway
lamebda.required-projects= # Optional list of projects that have to be present for the project to start
```

### Project configuration
`project.properties`
* `project.name` - Human-readable name of the project. Optional.
* `project.base-packages` - The base packages that Spring IOC container is scanning for services and controllers.
* `project.root-request-path-enabled` - Default is true. If you set this to false, the URLs in this project will not have the prepended `gateway` path.
* `project.url-prefix-enabled` - Default is true. If you set this to false the URLs in this project will not have the prepended project alias.

To create a project for deploying into Lamebda, please see https://github.com/ethlo/lamebda-samples.

### Monitoring
The loaded project(s) can be monitored using a custom [Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html) endpoint, under `/actuator/lamebda`.