package com.example.capstone.utils

import android.content.Context
import android.graphics.*
import androidx.core.content.ContextCompat
import com.example.capstone.R

class MannequinCompositor(private val context: Context) {

    /**
     * Create mannequin with user's face composited on top
     */
    fun createMannequinWithFace(
        userFaceBitmap: Bitmap?,
        mannequinWidth: Int = 400,
        mannequinHeight: Int = 1000
    ): Bitmap {
        val output = Bitmap.createBitmap(mannequinWidth, mannequinHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw mannequin body silhouette
        drawMannequinBody(canvas, mannequinWidth, mannequinHeight)

        // Composite user's face on mannequin head if available
        userFaceBitmap?.let {
            compositeFaceOnMannequin(canvas, it, mannequinWidth, mannequinHeight)
        }

        return output
    }

    /**
     * Draw standardized mannequin body
     */
    private fun drawMannequinBody(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        // Body color
        paint.color = Color.parseColor("#E5E7EB")

        val headRadius = width * 0.12f
        val headCenterX = width / 2f
        val headCenterY = height * 0.08f

        // Head (circle placeholder - will be replaced by user face)
        canvas.drawCircle(headCenterX, headCenterY, headRadius, paint)

        // Neck
        val neckTop = headCenterY + headRadius
        val neckBottom = neckTop + height * 0.06f
        val neckWidth = width * 0.15f
        val neckLeft = headCenterX - neckWidth / 2
        val neckRight = headCenterX + neckWidth / 2
        canvas.drawRect(neckLeft, neckTop, neckRight, neckBottom, paint)

        // Shoulders & Torso
        val shoulderWidth = width * 0.7f
        val torsoTop = neckBottom
        val torsoBottom = height * 0.6f
        val shoulderLeft = headCenterX - shoulderWidth / 2
        val shoulderRight = headCenterX + shoulderWidth / 2

        val path = Path().apply {
            moveTo(shoulderLeft, torsoTop)
            lineTo(shoulderRight, torsoTop)
            lineTo(headCenterX + width * 0.25f, torsoBottom)
            lineTo(headCenterX - width * 0.25f, torsoBottom)
            close()
        }
        canvas.drawPath(path, paint)

        // Arms
        paint.color = Color.parseColor("#D1D5DB")
        val armWidth = width * 0.08f

        // Left arm
        canvas.drawRect(
            shoulderLeft - armWidth,
            torsoTop + height * 0.05f,
            shoulderLeft,
            torsoBottom - height * 0.1f,
            paint
        )

        // Right arm
        canvas.drawRect(
            shoulderRight,
            torsoTop + height * 0.05f,
            shoulderRight + armWidth,
            torsoBottom - height * 0.1f,
            paint
        )

        // Hips & Legs
        paint.color = Color.parseColor("#E5E7EB")
        val hipWidth = width * 0.5f
        val legTop = torsoBottom
        val legBottom = height * 0.95f
        val legGap = width * 0.08f

        // Left leg
        canvas.drawRect(
            headCenterX - hipWidth / 2,
            legTop,
            headCenterX - legGap / 2,
            legBottom,
            paint
        )

        // Right leg
        canvas.drawRect(
            headCenterX + legGap / 2,
            legTop,
            headCenterX + hipWidth / 2,
            legBottom,
            paint
        )
    }

    /**
     * Composite user's face onto mannequin head
     */
    private fun compositeFaceOnMannequin(
        canvas: Canvas,
        faceBitmap: Bitmap,
        mannequinWidth: Int,
        mannequinHeight: Int
    ) {
        val headRadius = mannequinWidth * 0.12f * 2 // Diameter
        val headCenterX = mannequinWidth / 2f
        val headCenterY = mannequinHeight * 0.08f

        val faceSize = headRadius.toInt()
        val scaledFace = Bitmap.createScaledBitmap(faceBitmap, faceSize, faceSize, true)

        val faceLeft = headCenterX - faceSize / 2
        val faceTop = headCenterY - faceSize / 2

        canvas.drawBitmap(scaledFace, faceLeft, faceTop, null)
        scaledFace.recycle()
    }

    /**
     * Overlay clothing item on mannequin
     */
    fun overlayClothing(
        mannequinBitmap: Bitmap,
        clothingBitmap: Bitmap,
        category: String
    ): Bitmap {
        val result = mannequinBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val width = mannequinBitmap.width
        val height = mannequinBitmap.height

        when (category.lowercase()) {
            "tops" -> {
                val topWidth = (width * 0.75f).toInt()
                val topHeight = (height * 0.4f).toInt()
                val topLeft = (width - topWidth) / 2
                val topTop = (height * 0.15f).toInt()

                val scaledTop = Bitmap.createScaledBitmap(clothingBitmap, topWidth, topHeight, true)
                canvas.drawBitmap(scaledTop, topLeft.toFloat(), topTop.toFloat(), null)
                scaledTop.recycle()
            }

            "bottoms" -> {
                val bottomWidth = (width * 0.55f).toInt()
                val bottomHeight = (height * 0.45f).toInt()
                val bottomLeft = (width - bottomWidth) / 2
                val bottomTop = (height * 0.52f).toInt()

                val scaledBottom = Bitmap.createScaledBitmap(clothingBitmap, bottomWidth, bottomHeight, true)
                canvas.drawBitmap(scaledBottom, bottomLeft.toFloat(), bottomTop.toFloat(), null)
                scaledBottom.recycle()
            }

            "dresses" -> {
                val dressWidth = (width * 0.7f).toInt()
                val dressHeight = (height * 0.7f).toInt()
                val dressLeft = (width - dressWidth) / 2
                val dressTop = (height * 0.15f).toInt()

                val scaledDress = Bitmap.createScaledBitmap(clothingBitmap, dressWidth, dressHeight, true)
                canvas.drawBitmap(scaledDress, dressLeft.toFloat(), dressTop.toFloat(), null)
                scaledDress.recycle()
            }

            "shoes" -> {
                val shoeWidth = (width * 0.5f).toInt()
                val shoeHeight = (height * 0.15f).toInt()
                val shoeLeft = (width - shoeWidth) / 2
                val shoeTop = (height * 0.82f).toInt()

                val scaledShoes = Bitmap.createScaledBitmap(clothingBitmap, shoeWidth, shoeHeight, true)
                canvas.drawBitmap(scaledShoes, shoeLeft.toFloat(), shoeTop.toFloat(), null)
                scaledShoes.recycle()
            }

            "outerwear" -> {
                val outerWidth = (width * 0.8f).toInt()
                val outerHeight = (height * 0.5f).toInt()
                val outerLeft = (width - outerWidth) / 2
                val outerTop = (height * 0.14f).toInt()

                val scaledOuter = Bitmap.createScaledBitmap(clothingBitmap, outerWidth, outerHeight, true)
                canvas.drawBitmap(scaledOuter, outerLeft.toFloat(), outerTop.toFloat(), null)
                scaledOuter.recycle()
            }
        }

        return result
    }
}