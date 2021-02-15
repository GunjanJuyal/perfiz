package org.znsio.perfiz

import java.io.{File, FileInputStream}

import com.intuit.karate.gatling.PreDef._
import io.gatling.core.Predef._
import io.gatling.core.structure.PopulationBuilder
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps

class PerfizSimulation extends Simulation {

  private val configuration: PerfizConfiguration = new Yaml(new Constructor(classOf[PerfizConfiguration])).load(
    new FileInputStream(new File(System.getProperty("PERFIZ")))
  )

  private val builders: List[PopulationBuilder] = configuration.getKarateFeatures.asScala.toList.map(karateFeatureConfig => {
    val injections = karateFeatureConfig.getLoadPattern.asScala.toList.map(loadPattern => {
      val injectionStep = loadPattern.getPatternType match {
        case "nothingFor" => nothingFor(Duration(loadPattern.getDuration).asInstanceOf[FiniteDuration])
        case "atOnceUsers" => atOnceUsers(loadPattern.getUserCount.toInt)
        case "rampUsers" => rampUsers(loadPattern.getUserCount.toInt) during Duration(loadPattern.getDuration).asInstanceOf[FiniteDuration]
        case "constantUsersPerSec" =>constantUsersPerSec(loadPattern.getUserCount.toInt) during Duration(loadPattern.getDuration).asInstanceOf[FiniteDuration]
        case "rampUsersPerSec" => rampUsersPerSec(loadPattern.getUserCount.toInt) to(loadPattern.targetUserCount.toInt) during Duration(loadPattern.getDuration).asInstanceOf[FiniteDuration]
        case "heavisideUsers" => heavisideUsers(loadPattern.getUserCount.toInt) during Duration(loadPattern.getDuration).asInstanceOf[FiniteDuration]
      }
      injectionStep
    })
    val protocol = karateProtocol(
      karateFeatureConfig.uriPatterns.asScala.map { uriPattern => uriPattern -> Nil }: _*
    )
    scenario(karateFeatureConfig.getGatlingSimulationName).
      exec(karateFeature("classpath:" + karateFeatureConfig.getKarateFile)).
      inject(injections).
      protocols(protocol)
  })

  setUp(
    builders
  )

}
