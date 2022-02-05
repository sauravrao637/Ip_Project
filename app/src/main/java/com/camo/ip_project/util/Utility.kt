package com.camo.ip_project.util

import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.sqrt

object Utility {
    fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    fun getVarianceDouble(arrayList: ArrayList<Double>): Double {
        if (arrayList.isEmpty()) return 0.0
        val mean = arrayList.sum() / arrayList.size
        var sd = 0.0
        for (num in arrayList) {
            sd += (num - mean).pow(2.0)
        }
        return sqrt(sd / arrayList.size)
    }

    fun getVarianceLong(
        arrayList: ArrayList<Long>,
        offsetStart: Int = 0,
        offsetEnd: Int = 0
    ): Double {
        for (i in 0 until offsetStart) arrayList.removeFirstOrNull()
        for (i in 0 until offsetEnd) arrayList.removeLastOrNull()
        if (arrayList.isEmpty()) return 0.0
        val mean = arrayList.sum() / (1.0 * arrayList.size)
        var sd = 0.0
        for (num in arrayList) {
            sd += (num - mean).pow(2.0)
        }
        return sqrt(sd / arrayList.size)
    }

    fun List<Long>.toDoubleArray(): DoubleArray? {
        val ret = arrayListOf<Double>()
        for (num in this) {
            ret.add(num * 1.0)
        }
        return ret.toDoubleArray()
    }

    fun ArrayList<Long>.toDoubleArray(): DoubleArray? {
        val array = arrayListOf<Double>()
        for (i in 0 until size) {
            array.add(get(i) * 1.0)
        }
        return array.toDoubleArray()
    }
}