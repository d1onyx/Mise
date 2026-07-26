package com.d1onix.dishlab.data.session

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun graphDatabaseBuilder(context: Context): RoomDatabase.Builder<GraphDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder(
        context = appContext,
        name = appContext.getDatabasePath("dishlab_graph.db").absolutePath,
    )
}
