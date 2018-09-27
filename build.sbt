enablePlugins(CommonSettingsPlugin)

name := "sbt-openapi"

organization := "com.meetup"

description := "Plugin for generating managed code from OpenAPI specifications"

sbtPlugin := true

scalaVersion := appConfiguration.value.provider.scalaProvider.version

libraryDependencies += "io.swagger" % "swagger-codegen" % "2.3.1"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayOrganization in ThisBuild := Some("meetup")
