package com.speekez.data

import android.content.Context
import com.speekez.data.dao.DailyStatsDao
import com.speekez.data.dao.PresetDao
import com.speekez.data.dao.TranscriptionDao

fun Context.speekEZDatabase(): SpeekEZDatabase = SpeekEZDatabase.getDatabase(this)

fun Context.presetDao(): PresetDao = speekEZDatabase().presetDao()

fun Context.transcriptionDao(): TranscriptionDao = speekEZDatabase().transcriptionDao()

fun Context.dailyStatsDao(): DailyStatsDao = speekEZDatabase().dailyStatsDao()
