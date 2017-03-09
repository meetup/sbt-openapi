package com.meetup.sbtopenapi

import java.io.File
import java.net.URI
import java.util.ServiceLoader

import sbt.{Classpaths, Compile, Configuration, FileFilter, FileFunction, FilesInfo, Logger, Setting, SettingKey, TaskKey, config, globFilter, inConfig, richFile, singleFileFinder}
import sbt.Keys._
import io.swagger.codegen.{ClientOptInput, CodegenConfig, DefaultGenerator}
import io.swagger.codegen.config.CodegenConfigurator

import scala.annotation.tailrec

/**
 * sbt plugin for generating scala sources from an OpenAPI specification.
 */
object Plugin extends sbt.Plugin {
  val openapiConfig: Configuration = config("openapi")

  val basePackage: SettingKey[String] = SettingKey[String]("base-package", "The base package for the generated code.")
  val generate: TaskKey[Seq[File]] = TaskKey[Seq[File]]("generate", "Generate the Scala sources from the OpenAPI specification.")
  val listGenerators: TaskKey[Unit] = TaskKey[Unit]("list-generators", "List all available generators.")

  lazy val openapiSettings: Seq[Setting[_]] = inConfig(openapiConfig)(Seq[Setting[_]](
    basePackage := "org.openapis",
    sourceDirectory := (sourceDirectory in Compile).value / "openapi",
    scalaSource := (sourceManaged in Compile).value / "compiled_openapi",
    managedClasspath := Classpaths.managedJars(openapiConfig, classpathTypes.value, update.value),
    generate := {
      val srcDir = (sourceDirectory in openapiConfig).value
      val targetDir = (scalaSource in openapiConfig).value
      val basePkg = (basePackage in openapiConfig).value
      val taskStreams = streams.value
      val cache = taskStreams.cacheDirectory
      val cachedCompile = FileFunction.cached(
        cache / "openapi",
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists
      )(compile(srcDir, targetDir, taskStreams.log, basePkg))
      cachedCompile((srcDir ** fileFilter).get.toSet).toSeq
    },
    listGenerators := {
      val it = getCodegenServiceLoader.iterator()
      val sb = new StringBuilder("")
      while (it.hasNext) {
        if (sb.nonEmpty) sb.append(", ")
        sb.append(it.next().getName)
      }
      println(s"Available generators: ${sb.result()}")
    }
  )) ++ Seq[Setting[_]](
    sourceGenerators in Compile += (generate in openapiConfig).taskValue,
    managedSourceDirectories in Compile += (scalaSource in openapiConfig).value,
    cleanFiles += (scalaSource in openapiConfig).value,
    ivyConfigurations += openapiConfig
  )

  private def getCodegenServiceLoader: ServiceLoader[CodegenConfig] = {
    val cls = classOf[CodegenConfig]
    ServiceLoader.load(cls, cls.getClassLoader)
  }

  /**
   * Resolves a swagger-codegen config instance by its generator name. We need
   * this because the underlying loading mechanism relies on
   * `ServiceLoader.load(java.lang.Class)`, which assumes the service
   * definition file can be loaded from the current thread's context
   * class loader. For SBT plugins, this isn't the case, and so we explicitly
   * get the loader of the `CodegenConfig` class and then attempt to load the
   * service loader directly. Assuming we can load it, we then search all
   * instances for a config by the given name.
   */
  private def resolveConfigFromName(name: String): Option[CodegenConfig] = {
    val it = getCodegenServiceLoader.iterator()

      @tailrec def loop(): Option[CodegenConfig] = {
        if (it.hasNext) {
          val config = it.next()
          if (config.getName == name) Some(config)
          else loop()
        } else None
      }

    loop()
  }

  val fileFilter: FileFilter = "*.yaml" || "*.json"

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

  private def compile(srcDir: File, target: File, log: Logger, basePackage: String)(in: Set[File]) = {
    for (openapiFile <- in) {
      log.info(s"Generating source files from OpenAPI spec: $openapiFile")
      val parentFileName = openapiFile.getParentFile.getName // TODO too simplistic?
      val generatorName = resolveConfigFromName(parentFileName).getOrElse(sys.error(s"Failed to locate a generator by name $parentFileName!"))
      runCodegen(openapiFile.toURI, target, generatorName.getClass.getName, basePackage)
    }
    (target ** "*.scala").get.toSet // TODO can't assume scala
  }
}
