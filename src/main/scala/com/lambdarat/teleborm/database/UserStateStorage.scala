package com.lambdarat.teleborm.database

import java.sql.SQLException

import ConversationState._
import cats.effect.kernel.Async
import cats.syntax.all._
import doobie._
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._

class UserStateStorage[F[_]: Async: Logger](xa: Transactor[F]) {

  def getUserState(userId: Long): F[Option[UserState]] =
    sql"select user_id, message_id, conv_state, created from user_state where user_id = $userId"
      .query[UserState]
      .option
      .transact(xa)

  def saveUserState(userState: UserState): F[Int] = {
    val insert = sql"""
      insert into user_state (user_id, message_id, conv_state, created)
      values (${userState.userId}, ${userState.messageId}, ${userState.convState}, ${userState.created})
    """.update

    val update = sql"""
      update user_state
      set message_id = ${userState.messageId}, conv_state = ${userState.convState}, created = ${userState.created}
      where user_id = ${userState.userId}
    """.update

    // Crude upsert
    insert.run
      .transact(xa)
      .recoverWith { case ex: SQLException =>
        for {
          _      <- debug"Tried insert and failed with ${ex.getMessage}"
          result <- update.run.transact(xa)
        } yield result
      }
  }
}
