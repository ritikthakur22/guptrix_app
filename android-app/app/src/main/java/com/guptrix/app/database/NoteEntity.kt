package com.guptrix.app.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "noteId")
    val noteId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String = "",

    @ColumnInfo(name = "canvasData")
    val canvasData: String? = null,

    @ColumnInfo(name = "color")
    val color: String = "default",

    @ColumnInfo(name = "isPinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "isDeleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "deletedAt")
    val deletedAt: Long? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "isDraft")
    val isDraft: Boolean = false
)
