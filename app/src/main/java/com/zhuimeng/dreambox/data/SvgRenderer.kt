package com.zhuimeng.dreambox.data

import android.graphics.Bitmap
import android.graphics.Picture
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException

/**
 * SVG 渲染器
 *
 * 将 SVG 字节数组渲染为 Android Bitmap，
 * 用于在 RemoteViews 的 ImageView 中显示。
 */
object SvgRenderer {

    /**
     * 将 SVG 字节渲染为 Bitmap
     *
     * @param svgBytes SVG 文件的原始字节
     * @param targetWidth 目标宽度（px），0 表示使用 SVG 原始宽度
     * @return 渲染后的 Bitmap，失败时返回 null
     */
    fun renderToBitmap(
        svgBytes: ByteArray,
        targetWidth: Int = 0
    ): Bitmap? {
        return try {
            val svg = SVG.getFromString(String(svgBytes, Charsets.UTF_8))
            renderSvg(svg, targetWidth)
        } catch (e: SVGParseException) {
            // SVG 解析失败，记录日志
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun renderSvg(svg: SVG, targetWidth: Int): Bitmap {
        val picture: Picture = svg.renderToPicture()

        val svgWidth = picture.width
        val svgHeight = picture.height

        if (svgWidth <= 0 || svgHeight <= 0) {
            throw IllegalStateException("SVG 尺寸无效: ${svgWidth}x${svgHeight}")
        }

        val width = if (targetWidth > 0) targetWidth else svgWidth
        val height = if (targetWidth > 0) {
            (svgHeight.toFloat() / svgWidth * width).toInt()
        } else {
            svgHeight
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawPicture(picture, android.graphics.Rect(0, 0, width, height))
        return bitmap
    }

    /**
     * 根据 Widget 宽度估算合适的 Bitmap 宽度
     */
    fun estimateTargetWidth(widgetWidthDp: Float, density: Float): Int {
        val widgetWidthPx = (widgetWidthDp * density).toInt()
        // SVG 通常比 Widget 宽，我们需要缩放到合适大小
        return (widgetWidthPx * 1.2f).toInt()
    }
}
