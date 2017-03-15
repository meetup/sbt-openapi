# sbt-openapi

## Overview

`sbt-openapi` is an sbt plugin that will automatically generate source code from your OpenAPI specifications.
The generated code is managed, which makes it available to your project but does not need to be checked in to your
code repository.

## Usage

### Install the plugin

Add the following code to your `project/plugins.sbt` file:

```
addSbtPlugin("com.meetup" % "sbt-swagger" % "{pluginVersion}" ) // TODO
```

Add the following code to your `build.sbt` file:

```
com.meetup.sbtopenapi.Plugin.openapiSettings
```

### Use the plugin

The current OpenAPI specification is identical to the Swagger 2.0 specification and, as such, the sbt-openapi plugin
is merely a simplified interface to the [swagger-codegen](https://github.com/swagger-api/swagger-codegen) tool.
This plugin treats specification files as providers of [managed source code](http://www.scala-sbt.org/0.13/docs/Classpaths.html#Unmanaged+vs+managed).
Thus, at compile time, the plugin invokes a code generator using a specification file as input, and emits the resulting
code as managed source code. If you merely want to use a code generator to create a standalone library, you are better
off using [swagger-codegen](https://github.com/swagger-api/swagger-codegen) directly.

The plugin uses the on-disk organization of specification files to drive code generation, with specification files
rooted under `src/main/openapi/[generator]/[spec].yaml`. For example, consider this project structure:

```
<project_root>
   |
   +- src
       |
       +- main
           |
           +- openapi
               |
               +- [generator0]
               |   |
               |   +- spec0.yaml
               |   |
               |   +- spec1.yaml
               |
               +- [generator1]
                   |
                   +- spec2.yaml
```

Compilation of this project would use result in the plugin generating sources using `generator0` for the specification
files `spec0.yaml` and `spec1.yaml`, and `generator1` for the file `spec2.yaml`.