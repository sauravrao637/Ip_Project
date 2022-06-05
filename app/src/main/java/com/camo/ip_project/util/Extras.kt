/*****************************************************************************************
 * Copyright <2022> <Saurav Rao> <sauravrao637@gmail.com>                                *
 * Copyright <2022> <Piyush Sharma> <piyushlp@gmail.com>                                 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this  *
 * software and associated documentation files (the "Software"), to deal in the Software *
 * without restriction, including without limitation the rights to use, copy, modify,    *
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to    *
 * permit persons to whom the Software is furnished to do so, subject to the following   *
 * conditions:                                                                           *
 *                                                                                       *
 * The above copyright notice and this permission notice shall be included in all copies *
 * or substantial portions of the Software.                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,   *
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A         *
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT    *
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF  *
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE  *
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                                         *
 *****************************************************************************************/

package com.camo.ip_project.util

import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.sqrt

object Extras {
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

    fun doubleExponentialForecast(
        data: List<Double>,
        alpha: Double,
        gamma: Double,
        initializationMethod: Int,
        numForecasts: Int
    ): DoubleArray? {
        val y = DoubleArray(data.size + numForecasts)
        val s = DoubleArray(data.size)
        val b = DoubleArray(data.size)
        y[0] = data[0]
        s[0] = y[0]
        if (initializationMethod == 0) {
            b[0] = data[1] - data[0]
        } else if (initializationMethod == 1 && data.size > 4) {
            b[0] = (data[3] - data[0]) / 3
        } else if (initializationMethod == 2) {
            b[0] = (data[data.size - 1] - data[0]) / (data.size - 1)
        }
        var i = 1
        y[1] = s[0] + b[0]
        i = 1
        while (i < data.size) {
            s[i] = alpha * data[i] + (1 - alpha) * (s[i - 1] + b[i - 1])
            b[i] = gamma * (s[i] - s[i - 1]) + (1 - gamma) * b[i - 1]
            y[i + 1] = s[i] + b[i]
            i++
        }
        var j = 0
        while (j < numForecasts) {
            y[i] = s[data.size - 1] + (j + 1) * b[data.size - 1]
            j++
            i++
        }
        return y
    }
}