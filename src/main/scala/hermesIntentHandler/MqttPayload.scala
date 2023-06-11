package hermesIntentHandler

trait MqttPayload {
  val topic: String
  val payload: Array[Byte]
  val qos: Int = 2
  val retained = false
}
