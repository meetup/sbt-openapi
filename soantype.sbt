import xerial.sbt.Sonatype._

publishMavenStyle := true

sonatypeProfileName := "com.meetup"
sonatypeProjectHosting := Some(GitHubHosting(user="meetup", repository="sbt-openapi", email="engineering@meetup.com"))
developers := List(
  Developer(id = "meetup", name = "Meetup Developer", email = "engineering@meetup.com", url = url("https://www.meetup.com"))
)
licenses := Seq("MIT" -> url("https://mit-license.org/"))

publishTo := sonatypePublishTo.value
