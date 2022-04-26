package com.lambdarat.teleborm.domain.ports

import com.lambdarat.teleborm.bot.LegacyTelebormBot

import cats.effect.kernel.Async

class TelebormService[F[_]: Async](
    bot: LegacyTelebormBot[F]
) {}
