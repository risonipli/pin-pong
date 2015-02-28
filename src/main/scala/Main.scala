/*
Реализовать на Акке игру в пин-понг. Игра должна представлять собой 2 актора.
В начале игры первый актор посылает “пинг” каждые N ms.
Второй актор отвечает сообщением “понг” на каждые M посланных  “пингов”.
Известно, что актор, который принимает пинги часто падает с ошибкой.
Необходимо имитировать поломку каждые B ms > N и поднимать сломавшийся актор заново,
с тем значением пингов, с которым он упал. После K принятых “pong” акторы меняются местами
Игра в пин-понг заканчивается, после G перемен игроков.
Входные параметры
• N – задержка между пингами
• B – задержка между поломками
• M – количество “пингов”, необходимых для “понга”
• K – количесво “pong” после которого акторы меняются местами
• G -  количество “сетов” после которого игра заканчивается
 */

import ArgsConfig.Config
import akka.actor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}

case class StartGame(opponent: ActorRef, isContinue: Boolean = false)
case class PinMsg(pin: String = "pin")
case class PongMsg(pong: String = "pong")

class PinPong(config: Config, name: String) extends Actor {
  def blowUp(): Unit = {
      val deadline = config.failDelay millisecond fromNow
      Future(Await.ready(Promise().future, deadline.timeLeft)) onComplete { _ =>
        println("Bye, now.")
        1 / 0
        blowUp()
      }
  }

//  if (failer) blowUp()
  var pinCounter = 0
  var pongCounter = 0
  var switchCounter = 0
  var scheduler: Option[Cancellable] = None
  def receive = {
    case StartGame(opponent, isContinue) =>
      if (isContinue) switchCounter += 1

      scheduler = Some(this.context.system.scheduler.schedule(0.second, config.pinDelay.millisecond){
        opponent ! PinMsg()
        pinCounter += 1
      })

    case PinMsg(pin) =>
      println(s"$name received: $pin")
      pinCounter += 1

      if (pinCounter % config.pinNumber == 0) {
        sender ! PongMsg()
        pongCounter += 1
      }

    case PongMsg(pong) =>
      pongCounter += 1
      println(s"$name received: $pong\n")
      if (pongCounter != 0 && pongCounter % config.switch == 0) {
        switchCounter += 1
        if (switchCounter == config.set) this.context.system.shutdown()
        else {
          scheduler.map(s => s.cancel())
          sender ! StartGame(self, isContinue = true)
          println("=========SWITCH===========")
        }
      }
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    ArgsConfig.parser.parse(args, Config()) match {
      case Some(config) =>
        if (config.failDelay < config.pinDelay) {
          println("Опция --fail-delay должна быть больше чем опция --pin-delay")
          System.exit(1)
        }
        val system = ActorSystem("pinpong")
        val player1 = system.actorOf(Props(classOf[PinPong], config, "player1"))
        val player2 = system.actorOf(Props(classOf[PinPong], config, "player2"))
        player1 ! StartGame(player2)
      case None =>
    }
  }
}
