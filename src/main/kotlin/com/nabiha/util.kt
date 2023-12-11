package com.nabiha

import java.text.SimpleDateFormat
import java.util.*

fun generateRandomString(length: Int): String {
    val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    return (1..length)
        .map { charset.random() }
        .joinToString("")
}

fun dateNow(): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date())
}