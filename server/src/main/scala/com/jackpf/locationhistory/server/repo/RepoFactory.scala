package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.repo.RepoFactory.RepoType

object RepoFactory {
  enum RepoType {
    case IN_MEMORY, SQLITE
  }
}

class RepoFactory {
  def deviceRepo(repoType: RepoType): DeviceRepo = repoType match {
    case RepoType.IN_MEMORY => new InMemoryDeviceRepo
    case RepoType.SQLITE    => new SQLiteDeviceRepo
  }

  def locationRepo(repoType: RepoType): LocationRepo = repoType match {
    case RepoType.IN_MEMORY => new InMemoryLocationRepo
    case RepoType.SQLITE    => new SQLiteLocationRepo
  }
}
