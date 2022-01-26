package zio.aws.codegen.generator

import zio.aws.codegen.loader.ModelId

trait ArtifactListGenerator {
  this: HasConfig with GeneratorBase =>

  def generateArtifactList(ids: Set[ModelId], version: String): String = {
    val prefix = s"""---
                    |id: overview_artifacts
                    |title: Artifacts
                    |---
                    |
                    |# Published artifacts
                    |
                    |### HTTP client modules:
                    |```scala
                    |"dev.zio" %% "zio-aws-akka-http" % "$version"
                    |"dev.zio" %% "zio-aws-http4s" % "$version"
                    |"dev.zio" %% "zio-aws-netty" % "$version"
                    |```
                    |
                    |### List of all the generated libraries:
                    |
                    |```scala
                    |""".stripMargin

    val clients = ids.toList
      .sortBy(_.moduleName)
      .map { id =>
        s""""dev.zio" %% "zio-aws-${id.moduleName}" % "$version""""
      }
      .mkString("\n")

    val postfix = "\n```\n"

    prefix + clients + postfix
  }

}
