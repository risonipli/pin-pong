/*
Входные параметры
• N – задержка между пингами
• B – задержка между поломками
• M – количество “пингов”, необходимых для “понга”
• K – количесво “pong” после которого акторы меняются местами
• G - количество “сетов” после которого игра заканчивается

 */
object ArgsConfig {

  case class Config(
    pinDelay: Int = 500,
    failDelay: Int = 5000,
    pinNumber: Int = 3,
    switch: Int = 2,
    set: Int = 2)

  val parser = new scopt.OptionParser[Config]("pinpong") {
    head("pinpong", "0.0.1")
    opt[Int]('n', "pin-delay") action { (x, c) =>
      c.copy(pinDelay = x) } text "pin-delay - задержка между пингами в милисекундах"
    opt[Int]('b', "fail-delay") action { (x, c) =>
      c.copy(failDelay = x) } text "fail-delay - задержка между поломками"
    opt[Int]('m', "pin-number") action { (x, c) =>
      c.copy(pinNumber = x) } text "pin-number - количество “пингов”, необходимых для “понга”"
    opt[Int]('k', "switch") action { (x, c) =>
      c.copy(switch = x) } text "switch - количесво “pong” после которого акторы меняются местами"
    opt[Int]('g', "set") action { (x, c) =>
      c.copy(set = x) } text "set - количество “сетов” после которого игра заканчивается"
  }
}
