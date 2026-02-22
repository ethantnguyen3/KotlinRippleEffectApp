package com.example.rippleeffect.data

import java.util.*

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val description: String,
    val status: String,
    val priority: String,
    val assignedTo: String,
    val dueDate: Date?,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val tags: List<String> = emptyList(),
    val comments: List<Comment> = emptyList()
)

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorCode: Long,
    val icon: String,
    val itemCount: Int = 0
)

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val author: String,
    val createdAt: Date = Date()
)

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val role: String = "Member"
)
