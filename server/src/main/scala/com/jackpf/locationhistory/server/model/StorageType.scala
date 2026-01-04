package com.jackpf.locationhistory.server.model

import scopt.Read

enum StorageType {
  case IN_MEMORY, SQLITE
}

object StorageType {
  given Read[StorageType] = Read.reads { str =>
    StorageType.values
      .find(_.toString.equalsIgnoreCase(str))
      .getOrElse(
        throw new IllegalArgumentException(
          s"'$str' is not a valid RepoType. Allowed: ${StorageType.values.mkString(", ")}"
        )
      )
  }
}
