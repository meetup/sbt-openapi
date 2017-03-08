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
seq( com.meetup.sbtopenapi.Plugin.openapiSettings : _*)
```

### Use the plugin

TODO