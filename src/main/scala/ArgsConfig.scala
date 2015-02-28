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
      c.copy(pinDelay = x) } text "pin-delay - задержка между пингами в миллисекундах" validate { x =>
      if (x > 0) success else failure("Опция --pin-delay должна быть больше 0")
    }
    opt[Int]('b', "fail-delay") action { (x, c) =>
      c.copy(failDelay = x) } text "fail-delay - задержка между поломками в миллисекундах" validate { x =>
      if (x > 0) success else failure("Опция --fail-delay должна быть больше 0")
    }
    opt[Int]('m', "pin-number") action { (x, c) =>
      c.copy(pinNumber = x) } text "pin-number - количество “пингов”, необходимых для “понга”" validate { x =>
      if (x > 0) success else failure("Опция --pin-number должна быть больше 0")
    }
    opt[Int]('k', "switch") action { (x, c) =>
      c.copy(switch = x) } text "switch - количесво “pong” после которого акторы меняются местами" validate { x =>
      if (x > 0) success else failure("Опция --switch должна быть больше 0")
    }
    opt[Int]('g', "set") action { (x, c) =>
      c.copy(set = x) } text "set - количество “сетов” после которого игра заканчивается" validate { x =>
      if (x > 0) success else failure("Опция --set должна быть больше 0")
    }
  }
}
