organization := "com.meetup"
name := "sbt-openapi"

description := "Plugin for generating managed code from OpenAPI specifications"

sbtPlugin := true

scalaVersion := appConfiguration.value.provider.scalaProvider.version

libraryDependencies += "io.swagger" % "swagger-codegen" % "2.4.19"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))


// Add the default sonatype repository setting
publishTo := sonatypePublishTo.value
