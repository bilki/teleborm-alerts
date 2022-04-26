package com.lambdarat.teleborm.domain.ports

import com.lambdarat.teleborm.domain.model.UserState

trait UserStateRepository[F[_]] {

  def getUserState(userId: Long): F[Option[UserState]]

  def saveUserState(userState: UserState): F[Boolean]

}
