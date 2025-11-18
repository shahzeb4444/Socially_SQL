package com.teamsx.i230610_i230040

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.BitmapShader
import android.graphics.Shader
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Resize bitmap to a maximum dimension while maintaining aspect ratio
     */
    fun resizeBitmap(source: Bitmap, maxSize: Int): Bitmap {
        if (source.width <= maxSize && source.height <= maxSize) {
            return source
        }

        val ratio = if (source.width > source.height) {
            maxSize.toFloat() / source.width
        } else {
            maxSize.toFloat() / source.height
        }

        val width = (source.width * ratio).toInt()
        val height = (source.height * ratio).toInt()

        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    /**
     * Convert bitmap to Base64 string with specified quality
     * @param quality 0-100, where 100 is best quality
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 70): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Convert Base64 string to Bitmap
     */
    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create circular bitmap from source bitmap
     */
    fun getCircularBitmap(source: Bitmap, size: Int = 300): Bitmap {
        val scaled = Bitmap.createScaledBitmap(source, size, size, true)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        return output
    }

    /**
     * Load and decode Base64 image with memory optimization
     */
    fun loadBase64ImageOptimized(base64: String?, targetSize: Int = 512): Bitmap? {
        if (base64.isNullOrEmpty()) return null

        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)

            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calculate sample size for efficient bitmap loading
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}