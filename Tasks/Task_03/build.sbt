scalaVersion := "2.13.8"

name := "Task_03"

// Want to use a published library in your project?
// You can define other libraries as dependencies in your build like this:

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor-typed
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.18"

// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.0-alpha6"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.0-alpha6"

// https://mvnrepository.com/artifact/com.h2database/h2
libraryDependencies += "com.h2database" % "h2" % "1.4.200"
