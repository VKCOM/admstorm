package com.vk.admstorm.utils.extensions

/**
 * Returns the hexadecimal representation of an array of bytes.
 */
fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }
