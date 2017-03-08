package com.meetup.sbtopenapi

import java.io.File
import java.net.URI

import sbt.{
  Compile,
  Classpaths,
  Configuration,
  FileFunction,
  FilesInfo,
  Logger,
  Setting,
  SettingKey,
  TaskKey,
  config,
  globFilter,
  inConfig,
  richFile,
  singleFileFinder
}
import sbt.Keys.{
  classpathTypes,
  cleanFiles,
  ivyConfigurations,
  scalaSource,
  managedClasspath,
  managedSourceDirectories,
  sourceDirectory,
  sourceGenerators,
  sourceManaged,
  streams,
  update
}
import io.swagger.codegen.ClientOptInput
import io.swagger.codegen.DefaultGenerator
import io.swagger.codegen.config.CodegenConfigurator

/**
 * sbt plugin for generating scala sources from an OpenAPI specification.
 */
object Plugin extends sbt.Plugin {
  val openapiConfig: Configuration = config("openapi")

  val generate: TaskKey[Seq[File]] = TaskKey[Seq[File]]("generate", "Generate the Scala sources from the OpenAPI specification.")

  val codeGenClass: SettingKey[String] = SettingKey[String]("codegen-class", "CodeGen class to use to generate sources.")

  lazy val openapiSettings: Seq[Setting[_]] = inConfig(openapiConfig)(Seq[Setting[_]](
    sourceDirectory := (sourceDirectory in Compile).value / "openapi",
    scalaSource := (sourceManaged in Compile).value / "compiled_openapi",
    managedClasspath := Classpaths.managedJars(openapiConfig, classpathTypes.value, update.value),
    generate := {
      val srcDir = (sourceDirectory in openapiConfig).value
      val targetDir = (scalaSource in openapiConfig).value
      val taskStreams = streams.value
      val cache = taskStreams.cacheDirectory
      val cachedCompile = FileFunction.cached(
        cache / "openapi",
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists
      ) { (in: Set[File]) =>
          compile(srcDir, targetDir, taskStreams.log)
        }
      cachedCompile((srcDir ** "*.yaml").get.toSet).toSeq
    }
  )) ++ Seq[Setting[_]](
    sourceGenerators in Compile += (generate in openapiConfig).taskValue,
    managedSourceDirectories in Compile += (scalaSource in openapiConfig).value,
    cleanFiles += (scalaSource in openapiConfig).value,
    ivyConfigurations += openapiConfig
  )

  private def runCodegen(openapiSpec: URI, target: File, generatorName: String, basePackage: String) = {
    val configurator = new CodegenConfigurator()
    configurator.setVerbose(false)

    val specLocation =
      openapiSpec.toString match {
        case file if file.toLowerCase.startsWith("file:") => "file:///" + file.substring(6) // TODO explain
        case x => x
      }

    configurator.setLang(generatorName)
    configurator.setInputSpec(specLocation)
    configurator.setOutputDir(target.toString)

    // determine the scala package of the generated code by the
    // filename of the OpenAPI specification
    val filename = openapiSpec.toString.split("/").last
    val basename = filename.substring(0, filename.length - 5) // TODO assumes .yaml|.json

    val invokerPackage = basePackage + "." + basename
    configurator.setInvokerPackage(basePackage + "." + basename)
    configurator.setModelPackage(s"$invokerPackage.model")
    configurator.setApiPackage(s"$invokerPackage.api")

    val input: ClientOptInput = configurator.toClientOptInput

    // configurator.toClientOptInput() attempts to read the file and parse an
    // OpenAPI specification. If it fails for any reason, that reason is
    // swallowed and null is returned. For now, this is our best bet at
    // providing a slightly less hostile error message, but we should look
    // manually constructing ClientOptInput, so we can control how parsing
    // errors are reported.
    if (input.getSwagger == null) {
      sys.error(s"Failed to load OpenAPI specification from $openapiSpec! Is it valid?")
    } else {
      new DefaultGenerator().opts(input).generate()
    }
  }

  private def compile(srcDir: File, target: File, log: Logger, basePackage: String) = {
    for (openapiFile <- (srcDir ** "*.yaml").get) {
      log.info(s"Generate source files for OpenAPI yaml: $openapiFile")
      val generatorName = openapiFile.getParentFile.getName // TODO too simplistic?
      runCodegen(openapiFile.toURI, target, generatorName, basePackage)
    }

    (target ** "*.scala").get.toSet // TODO can't assume scala
  }
}
