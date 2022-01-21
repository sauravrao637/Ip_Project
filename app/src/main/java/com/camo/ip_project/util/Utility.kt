package com.camo.ip_project.util

import java.nio.ByteBuffer
import kotlin.math.abs

object Utility {
    fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }
}