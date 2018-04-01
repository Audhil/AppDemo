package com.audhil.medium.demoapp.camera2

import android.graphics.ImageFormat
import android.media.Image
import com.audhil.medium.demoapp.util.showVLog
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR
import android.graphics.ImageFormat.YUV_420_888
import android.graphics.ImageFormat.getBitsPerPixel
import java.nio.ByteBuffer


/*
 * Created by mohammed-2284 on 31/03/18.
 */

class ImageManipulation(private val acquireLatestImage: Image) : Runnable {

    override fun run() {
        showVLog("---acquireLatestImage.height : " + acquireLatestImage.height)
        showVLog("---acquireLatestImage.width : " + acquireLatestImage.width)
        showVLog("---acquireLatestImage.format : " + acquireLatestImage.format)
        try {
//            val buf = Mat(acquireLatestImage.height, acquireLatestImage.width, CvType.CV_8UC1)
//            val buffer = acquireLatestImage.planes[0].buffer
//            val bytes = ByteArray(buffer.remaining())
//            buffer.get(bytes)
//            buf.put(0, 0, bytes)
//            val mat = Imgcodecs.imdecode(buf, IMREAD_COLOR)
//            showVLog("acquireLatestImage : Resultant Mat is : mat : $mat")

            val mat = imageToMat(acquireLatestImage)
            showVLog("acquireLatestImage : Resultant Mat is : mat : $mat")
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } finally {
            acquireLatestImage.close()
        }
    }

    //  image to mat converter
    private fun imageToMat(image: Image): Mat {
        var buffer: ByteBuffer
        var rowStride: Int
        var pixelStride: Int
        val width = image.width
        val height = image.height
        var offset = 0

        val planes = image.planes
        val data = ByteArray(image.width * image.height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)
        val rowData = ByteArray(planes[0].rowStride)

        for (i in planes.indices) {
            buffer = planes[i].buffer
            rowStride = planes[i].rowStride
            pixelStride = planes[i].pixelStride
            val w = if (i == 0) width else width / 2
            val h = if (i == 0) height else height / 2
            for (row in 0 until h) {
                val bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8
                if (pixelStride == bytesPerPixel) {
                    val length = w * bytesPerPixel
                    buffer.get(data, offset, length)

                    // Advance buffer the remainder of the row stride, unless on the last row.
                    // Otherwise, this will throw an IllegalArgumentException because the buffer
                    // doesn't include the last padding.
                    if (h - row != 1) {
                        buffer.position(buffer.position() + rowStride - length)
                    }
                    offset += length
                } else {

                    // On the last row only read the width of the image minus the pixel stride
                    // plus one. Otherwise, this will throw a BufferUnderflowException because the
                    // buffer doesn't include the last padding.
                    if (h - row == 1) {
                        buffer.get(rowData, 0, width - pixelStride + 1)
                    } else {
                        buffer.get(rowData, 0, rowStride)
                    }

                    for (col in 0 until w) {
                        data[offset++] = rowData[col * pixelStride]
                    }
                }
            }
        }

        // Finally, create the Mat.
        val mat = Mat(height + height / 2, width, CvType.CV_8UC1)
        mat.put(0, 0, data)

        return mat
    }
}