val mainScala     = "2.13.15"
val allScala      = Seq("3.3.4", "2.13.15", "2.12.20")
val zioVersion    = "2.1.11"
val zioAwsVersion = "7.28.26.1"

inThisBuild(
  List(
    organization := "dev.zio",
    homepage := Some(url("https://zio.dev/zio-sqs")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := mainScala,
    crossScalaVersions := allScala,
    Test / parallelExecution := false,
    Test / fork := true,
    run / fork := true,
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
    scmInfo := Some(
      ScmInfo(url("https://github.com/zio/zio-sqs/"), "scm:git:git@github.com:zio/zio-sqs.git")
    ),
    developers := List(
      Developer(
        "ghostdogpr",
        "Pierre Ricadat",
        "ghostdogpr@gmail.com",
        url("https://github.com/ghostdogpr")
      )
    ),
    githubWorkflowTargetTags ++= Seq("v*"),
    githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
    githubWorkflowJavaVersions := List(
      JavaSpec.temurin("11"),
      JavaSpec.temurin("17"),
      JavaSpec.temurin("21")
    ),
    githubWorkflowPublish := Seq(
      WorkflowStep.Sbt(
        commands = List("ci-release"),
        name = Some("Publish project"),
        env = Map(
          "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
          "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
          "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
          "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
        )
      )
    )
  )
)

publishTo := sonatypePublishToBundle.value

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("validate", "check" + allScala.map(v => s"++${v}! test").mkString(";", ";", ""))

lazy val root = project
  .in(file("."))
  .settings(
    publish / skip := true
  )
  .aggregate(
    sqs,
    docs
  )

lazy val sqs =
  project
    .in(file("zio-sqs"))
    .settings(
      name := "zio-sqs",
      scalafmtOnCompile := true,
      libraryDependencies ++= Seq(
        "dev.zio"                %% "zio"                     % zioVersion,
        "dev.zio"                %% "zio-streams"             % zioVersion,
        "dev.zio"                %% "zio-aws-sqs"             % zioAwsVersion,
        "dev.zio"                %% "zio-aws-netty"           % zioAwsVersion,
        "org.scala-lang.modules" %% "scala-collection-compat" % "2.12.0",
        "dev.zio"                %% "zio-test"                % zioVersion % "test",
        "dev.zio"                %% "zio-test-sbt"            % zioVersion % "test",
        "org.elasticmq"          %% "elasticmq-rest-sqs"      % "1.6.9"    % "test",
        "org.elasticmq"          %% "elasticmq-core"          % "1.6.9"    % "test"
      ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12 | 13)) =>
          Seq("org.typelevel" %% "kind-projector" % "0.13.3" cross CrossVersion.full)
        case _                  =>
          Nil
      }),
      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding",
        "UTF-8",
        "-explaintypes",
        "-feature",
        "-language:higherKinds",
        "-language:existentials",
        "-unchecked"
      ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) =>
          Seq(
            "-Xfuture",
            "-Xsource:2.13",
            "-Xlint:_,-type-parameter-shadow",
            "-Yno-adapted-args",
            "-Ypartial-unification",
            "-Ywarn-extra-implicit",
            "-Ywarn-inaccessible",
            "-Ywarn-infer-any",
            "-Ywarn-nullary-override",
            "-Ywarn-nullary-unit",
            "-Yrangepos",
            "-Ywarn-numeric-widen",
            "-Ywarn-unused",
            "-Ywarn-value-discard",
            "-opt-inline-from:<source>",
            "-opt-warnings",
            "-opt:l:inline"
          )
        case Some((2, 13)) =>
          Seq(
            "-Xlint:_,-type-parameter-shadow",
            "-Werror",
            "-Yrangepos",
            "-Ywarn-numeric-widen",
            "-Ywarn-unused",
            "-Ywarn-value-discard"
          )
        case _             =>
          Nil
      }),
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
    )

lazy val docs = project
  .in(file("zio-sqs-docs"))
  .settings(
    moduleName := "zio-sqs-docs",
    scalacOptions -= "-Yno-imports",
    scalacOptions -= "-Xfatal-warnings",
    projectName := "ZIO SQS",
    mainModuleName := (sqs / moduleName).value,
    projectStage := ProjectStage.ProductionReady,
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(sqs),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion
    )
  )
  .dependsOn(sqs)
  .enablePlugins(WebsitePlugin)
