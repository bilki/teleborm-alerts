package com.lambdarat.teleborm.domain.model

import com.bot4s.telegram.models.InlineKeyboardMarkup

final case class SearchCommandResult(
    searchResult: SearchResult,
    pagination: InlineKeyboardMarkup
)
