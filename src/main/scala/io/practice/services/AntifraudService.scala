package io.practice.services

import akka.actor.Scheduler
import io.practice
import io.practice.data.Mongo
import io.practice.executionContext
import io.practice.models.Report

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object AntifraudService {
  private val pollInterval = 5 seconds
  private val scheduler: Scheduler = practice.scheduler

  private val stopList = Set("хакер", "взлом", "ddos", "доход", "paypal", "фрибет")

  private val whiteList = Set("notSoBad")

  def start(): Unit = {
    scheduler.scheduleWithFixedDelay(pollInterval, pollInterval) { () =>
      scanPages()
    }
  }


  private def scanPages(): Unit = {
    Mongo.getAllPages.map { pagesSeq =>
      pagesSeq.foreach { page =>
        if (!whiteList.contains(page.projectId)) {
          val foundStopWords = stopList.filter(word => page.content.toLowerCase().contains(word))
          if (foundStopWords.nonEmpty) {
            val report = Report(
              id = java.util.UUID.randomUUID().toString,
              stopWords = foundStopWords.toList,
              projectId = page.projectId,
              pageId = page.id
            )
            Mongo.saveReport(report)
          }
        }
      }
    }
  }

