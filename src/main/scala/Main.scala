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
import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class StartGame(opponent: ActorRef, state: PlayerState)
case class PinMsg(pin: String = "pin")
case class PongMsg(pong: String = "pong")
case class PlayerState(pins: Int = 0, pongs: Int = 0, switchs: Int = 0)
case object BlowUp

class PinPongSuperVisorStrategy extends SupervisorStrategyConfigurator {
  override def create(): SupervisorStrategy = OneForOneStrategy() {
    case _: Exception => Restart
  }
}

object PinPong {
  private val stateStorage = collection.mutable.Map[String, PlayerState]()

  def saveState(path: String, state: PlayerState) =
    if (!stateStorage.contains(path)) {
      stateStorage(path) = state
    } else throw new Exception("This state already stored")

  def getState(path: String) = {
    val state = stateStorage.getOrElse(path, PlayerState())
    stateStorage.remove(path)
    state
  }
}

class PinPong(config: Config, name: String) extends Actor {
  var pinCounter = 0
  var pongCounter = 0
  var switchCounter = 0
  var pinSenderSch: Option[Cancellable] = None
  var blowUpSch: Option[Cancellable] = None

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    cancelBlowUp()
    PinPong.saveState(self.path.toString, PlayerState(pinCounter))
  }
  override def postRestart(reason: Throwable): Unit = {
    val state = PinPong.getState(self.path.toString)
    pinCounter = state.pins
  }

  def startPinSending(opponent: ActorRef) = {
    if (pinSenderSch.isEmpty)
      pinSenderSch = Some(this.context.system.scheduler.schedule(0.second, config.pinDelay.millisecond){
        opponent ! PinMsg()
      })
  }

  def cancelPinSending() = {
    pinSenderSch.map(_.cancel())
    pinSenderSch = None
  }

  def startBlowUp() = {
    if (blowUpSch.isEmpty)
      blowUpSch = Some(context.system.scheduler.schedule(config.failDelay.millisecond, config.failDelay.millisecond) {
        self ! BlowUp
      })
  }

  def cancelBlowUp() = {
    blowUpSch.map(_.cancel())
    blowUpSch = None
  }

  def receive = {
    case StartGame(opponent, state) =>
      cancelBlowUp()
      pongCounter = state.pongs
      switchCounter = state.switchs
      startPinSending(opponent)

    case PinMsg(pin) =>
      println(s"$name received: $pin")
      pinCounter += 1
      startBlowUp()

      if (pinCounter % config.pinNumber == 0) {
        sender ! PongMsg()
      }

    case PongMsg(pong) =>
      pongCounter += 1
      println(s"$name received: $pong\n")
      if (pongCounter != 0 && pongCounter % config.switch == 0) {
        switchCounter += 1
        cancelPinSending()
        if (switchCounter == config.set) this.context.system.shutdown()
        else {
          cancelPinSending()
          sender ! StartGame(self, PlayerState(
            pongs = pongCounter, switchs = switchCounter))
          println(s"=========SWITCH #$switchCounter===========")
        }
      }

    case BlowUp =>
        throw new Exception(s"Player $name blowed up with $pinCounter pins")
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
        val akkaConfig = ConfigFactory.load()
        val system = ActorSystem("pinpong", akkaConfig)
        val player1 = system.actorOf(Props(classOf[PinPong], config, "player1"))
        val player2 = system.actorOf(Props(classOf[PinPong], config, "player2"))

        player1 ! StartGame(player2, PlayerState())
      case None =>
    }
  }
}
