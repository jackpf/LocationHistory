package com.jackpf.locationhistory.server.model

import com.jackpf.locationhistory.common.PushHandler as ProtoPushHandler

object PushHandler {
  def fromProto(proto: ProtoPushHandler): PushHandler = PushHandler(
    name = proto.name,
    url = proto.url
  )
}

case class PushHandler(name: String, url: String) {
  def toProto: ProtoPushHandler = ProtoPushHandler(
    name = name,
    url = url
  )
}
