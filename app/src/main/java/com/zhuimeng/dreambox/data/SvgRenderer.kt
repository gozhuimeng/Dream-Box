package com.zhuimeng.dreambox.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.Rect
import android.util.Log
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException

object SvgRenderer {

    private const val TAG = "SvgRenderer"

    /**
     * 渲染 SVG 并裁剪放大，填满 widget 可用空间
     *
     * - 裁剪左侧星期标签 (Mon/Wed/Fri)，只保留热力图网格
     * - 按 widget 尺寸放大，纵向填满
     * - 超出宽度的部分取右侧（最新贡献数据）
     */
    fun renderToWidgetBitmap(
        svgBytes: ByteArray,
        widgetWidthPx: Int,
        widgetHeightPx: Int
    ): Bitmap? {
        return try {
            val svgString = String(svgBytes, Charsets.UTF_8)
            val svg = SVG.getFromString(svgString)
            val picture = svg.renderToPicture()
            renderCroppedToFit(picture, widgetWidthPx, widgetHeightPx)
        } catch (e: SVGParseException) {
            Log.e(TAG, "SVG 解析失败", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "渲染异常", e)
            null
        }
    }

    /**
     * 裁剪左侧标签 + 放大填满 widget
     */
    private fun renderCroppedToFit(
        picture: Picture,
        widgetW: Int,
        widgetH: Int
    ): Bitmap {
        val svgW = picture.width
        val svgH = picture.height
        Log.d(TAG, "SVG 原始尺寸: ${svgW}x${svgH}, widget: ${widgetW}x${widgetH}")

        if (svgW <= 0 || svgH <= 0) {
            throw IllegalStateException("SVG 尺寸无效: ${svgW}x${svgH}")
        }

        // 1. 先把完整 SVG 渲染到 Bitmap
        val fullBitmap = Bitmap.createBitmap(svgW, svgH, Bitmap.Config.ARGB_8888)
        Canvas(fullBitmap).drawPicture(picture)

        // 2. 裁剪左侧 ~4% 的星期标签区域（ghchart 中约 27px）
        val cropLeft = (svgW * 0.04f).toInt().coerceIn(15, 50)
        val gridW = svgW - cropLeft
        val gridH = svgH
        Log.d(TAG, "裁剪左侧 $cropLeft px, 网格区域: ${gridW}x${gridH}")

        // 3. 按高度缩放，使网格纵向填满 widget
        val scale = widgetH.toFloat() / gridH
        val scaledGridW = (gridW * scale).toInt()
        Log.d(TAG, "缩放因子: $scale, 缩放后网格宽: $scaledGridW")

        val result = Bitmap.createBitmap(widgetW, widgetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        // 白色背景
        canvas.drawColor(android.graphics.Color.WHITE)

        if (scaledGridW <= widgetW) {
            // 网格比 widget 窄 → 居中显示（通常不会发生）
            val srcRect = Rect(cropLeft, 0, svgW, svgH)
            val dstX = (widgetW - scaledGridW) / 2
            val dstRect = Rect(dstX, 0, dstX + scaledGridW, widgetH)
            canvas.drawBitmap(fullBitmap, srcRect, dstRect, null)
            Log.d(TAG, "网格较窄, 居中显示")
        } else {
            // 网格比 widget 宽 → 取右侧（最新贡献），纵向填满
            val srcLeft = (svgW - widgetW / scale).toInt().coerceAtLeast(cropLeft)
            val srcRect = Rect(srcLeft, 0, svgW, svgH)
            val dstRect = Rect(0, 0, widgetW, widgetH)
            canvas.drawBitmap(fullBitmap, srcRect, dstRect, null)
            Log.d(TAG, "显示右侧最新贡献, 源 x=$srcLeft..$svgW")
        }

        fullBitmap.recycle()
        Log.d(TAG, "渲染完成: ${widgetW}x${widgetH}")
        return result
    }

    // --- 保留原始渲染方法（兼容旧调用） ---

    fun renderToBitmap(
        svgBytes: ByteArray,
        targetWidth: Int = 0
    ): Bitmap? {
        return try {
            val svgString = String(svgBytes, Charsets.UTF_8)
            Log.d(TAG, "SVG 原始数据: ${svgString.take(200)}...")
            val svg = SVG.getFromString(svgString)
            renderSvg(svg, targetWidth)
        } catch (e: SVGParseException) {
            Log.e(TAG, "SVG 解析失败", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "渲染异常", e)
            null
        }
    }

    private fun renderSvg(svg: SVG, targetWidth: Int): Bitmap {
        val picture: Picture = svg.renderToPicture()
        val svgWidth = picture.width
        val svgHeight = picture.height
        Log.d(TAG, "SVG 原始尺寸: ${svgWidth}x${svgHeight}")

        if (svgWidth <= 0 || svgHeight <= 0) {
            throw IllegalStateException("SVG 尺寸无效: ${svgWidth}x${svgHeight}")
        }

        val width = if (targetWidth > 0) targetWidth else svgWidth
        val height = if (targetWidth > 0) {
            (svgHeight.toFloat() / svgWidth * width).toInt()
        } else {
            svgHeight
        }
        Log.d(TAG, "渲染目标尺寸: ${width}x${height}")

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawPicture(picture, Rect(0, 0, width, height))
        Log.d(TAG, "Bitmap 创建成功")
        return bitmap
    }

    fun estimateTargetWidth(widgetWidthDp: Float, density: Float): Int {
        val widgetWidthPx = (widgetWidthDp * density).toInt()
        return (widgetWidthPx * 1.2f).toInt()
    }
}
