package com.camo.ip_project.util

import timber.log.Timber

object ImageProcessing {
    private fun decodeYUV420SPtoRGBSum(
        yuv420sp: ByteArray?,
        width: Int,
        height: Int
    ): List<Int> {
        if (yuv420sp == null) return listOf(0, 0, 0)
        val frameSize = width * height
        var sumr = 0
        var sumg = 0
        var sumb = 0
        var j = 0
        var yp = 0
        while (j < height) {
            var uvp = frameSize + (j shr 1) * width
            var u = 0
            var v = 0
            var i = 0
            while (i < width) {
                var y = (0xff and yuv420sp[yp].toInt()) - 16
                if (y < 0) y = 0
                if (i and 1 == 0) {
                    v = (0xff and yuv420sp[uvp++].toInt()) - 128
                    u = (0xff and yuv420sp[uvp++].toInt()) - 128
                }
                val y1192 = 1192 * y
                var r = y1192 + 1634 * v
                var g = y1192 - 833 * v - 400 * u
                var b = y1192 + 2066 * u
                if (r < 0) r = 0 else if (r > 262143) r = 262143
                if (g < 0) g = 0 else if (g > 262143) g = 262143
                if (b < 0) b = 0 else if (b > 262143) b = 262143
                val pixel =
                    -0x1000000 or (r shl 6 and 0xff0000) or (g shr 2 and 0xff00) or (b shr 10 and 0xff)
                val red = pixel shr 16 and 0xff
                val green = pixel shr 8 and 0xff
                val blue = pixel and 0xff
                sumr += red
                sumg += green
                sumb += blue
                i++
                yp++
            }
            j++
        }
        return listOf(sumr, sumg, sumb)
    }

    /**
     * Given a byte array representing a yuv420sp image, determine the average
     * amount of red in the image. Note: returns 0 if the byte array is NULL.
     *
     * @param yuv420sp Byte array representing a yuv420sp image
     * @param width    Width of the image.
     * @param height   Height of the image.
     * @return int array of size 3 representing the average amount of (red, green, blue)
     * in the image.
     */
    fun decodeYUV420SPtoRGBAvg(
        yuv420sp: ByteArray?,
        width: Int,
        height: Int
    ): List<Double> {
        if (yuv420sp == null) return listOf(0.0, 0.0, 0.0)
        val frameSize = width * height * 1.0
        val sumArray = decodeYUV420SPtoRGBSum(yuv420sp, width, height)
        val ret = listOf(sumArray[0] / frameSize, sumArray[1] / frameSize, sumArray[2] / frameSize)
//        val hsv = FloatArray(3)
//        val currentColor = Color.rgb((sumArray[0]/ frameSize).toInt(), (sumArray[1] / frameSize).toInt(), (sumArray[2] / frameSize).toInt())
//        Color.colorToHSV(currentColor, hsv)
        Timber.d("width: $width,height: $height, size: ${yuv420sp.size} sums: $ret")
        return ret
    }
}
