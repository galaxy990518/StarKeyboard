package com.startown.starkeyboard.ime

import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF

// 由 tools/key_shapes.txt 產生（node tools/build_shapes.js），不要直接改這個檔案

/** 用線條畫的按鍵造型（動物、像素、拼圖、書本、蝴蝶結…）。座標在 0～100 的方格裡，畫的時候拉伸成按鍵大小。 */
class KeyShapeArt(
    val title: String,
    /** 字放在這個方框（0～100）的中間：x, y, 寬, 高 */
    private val body: FloatArray,
    private val path: FloatArray,
    private val detail: FloatArray,
    /** 小裝飾的顏色；null = 用主題的強調色 */
    private val detailLight: Int?,
    private val detailDark: Int?,
    val detailAlpha: Float,
) {
    fun path(r: RectF): Path = build(path, r)
    fun detailPath(r: RectF): Path = build(detail, r)
    fun bodyRect(r: RectF) = RectF(r.left + r.width() * body[0] / 100, r.top + r.height() * body[1] / 100,
        r.left + r.width() * (body[0] + body[2]) / 100, r.top + r.height() * (body[1] + body[3]) / 100)
    fun detailColor(theme: KeyboardTheme): Int {
        val light = detailLight ?: return theme.accent
        val c = if (theme.dark) detailDark ?: light else light
        return Color.rgb(c shr 16 and 0xFF, c shr 8 and 0xFF, c and 0xFF)
    }

    companion object {
        val order = listOf("catFace", "bear", "paw", "pixel", "pixelHeart", "puzzle", "book", "bow", "butterfly", "fish", "strawberry", "flower", "moon", "sun", "star", "heart")

        val shapes: Map<String, KeyShapeArt> = mapOf(
            "catFace" to KeyShapeArt(
                "貓咪", floatArrayOf(12f, 34f, 76f, 56f),
                floatArrayOf(0f, 14f, 34f, 1f, 10f, 6f, 3f, 10f, 2f, 14f, 4f, 1f, 36f, 20f, 3f, 50f, 16f, 64f, 20f, 1f, 86f, 4f, 3f, 90f, 2f, 90f, 6f, 1f, 86f, 34f, 3f, 98f, 48f, 96f, 66f, 2f, 94f, 90f, 76f, 98f, 50f, 98f, 2f, 24f, 98f, 6f, 90f, 4f, 66f, 3f, 2f, 48f, 14f, 34f, 4f),
                floatArrayOf(0f, 17f, 28f, 1f, 15f, 12f, 1f, 30f, 22f, 4f, 0f, 70f, 22f, 1f, 85f, 12f, 1f, 83f, 28f, 4f, 0f, 3f, 62f, 1f, 17f, 64f, 1f, 17f, 66f, 1f, 3f, 65f, 4f, 0f, 3f, 73f, 1f, 17f, 70f, 1f, 17f, 72f, 1f, 3f, 76f, 4f, 0f, 97f, 65f, 1f, 83f, 66f, 1f, 83f, 64f, 1f, 97f, 62f, 4f, 0f, 97f, 76f, 1f, 83f, 72f, 1f, 83f, 70f, 1f, 97f, 73f, 4f),
                0xF2A0BC, 0xF2A0BC, 0.85f),
            "bear" to KeyShapeArt(
                "熊熊", floatArrayOf(14f, 30f, 72f, 52f),
                floatArrayOf(0f, 22f, 30f, 2f, 10f, 30f, 4f, 22f, 6f, 14f, 2f, 8f, 4f, 20f, 2f, 28f, 10f, 3f, 50f, 0f, 72f, 10f, 2f, 80f, 2f, 92f, 4f, 94f, 14f, 2f, 96f, 22f, 90f, 30f, 78f, 30f, 3f, 98f, 44f, 96f, 64f, 2f, 94f, 90f, 76f, 98f, 50f, 98f, 2f, 24f, 98f, 6f, 90f, 4f, 64f, 3f, 2f, 44f, 22f, 30f, 4f),
                floatArrayOf(0f, 39f, 84f, 1f, 48f, 87f, 1f, 39f, 91f, 4f, 0f, 61f, 91f, 1f, 52f, 87f, 1f, 61f, 84f, 4f, 0f, 53.5f, 87f, 2f, 53.5f, 88.93f, 51.93f, 90.5f, 50f, 90.5f, 2f, 48.07f, 90.5f, 46.5f, 88.93f, 46.5f, 87f, 2f, 46.5f, 85.07f, 48.07f, 83.5f, 50f, 83.5f, 2f, 51.93f, 83.5f, 53.5f, 85.07f, 53.5f, 87f, 4f),
                0x8A5A44, 0xD9A07A, 0.9f),
            "paw" to KeyShapeArt(
                "肉球", floatArrayOf(26f, 46f, 48f, 40f),
                floatArrayOf(0f, 18f, 64f, 2f, 18f, 46f, 34f, 38f, 50f, 38f, 2f, 66f, 38f, 82f, 46f, 82f, 64f, 2f, 82f, 86f, 68f, 98f, 50f, 98f, 2f, 32f, 98f, 18f, 86f, 18f, 64f, 4f, 0f, 24.63f, 33.15f, 2f, 26.63f, 40.62f, 23.49f, 47.95f, 17.62f, 49.52f, 2f, 11.76f, 51.1f, 5.38f, 46.32f, 3.37f, 38.85f, 2f, 1.37f, 31.38f, 4.51f, 24.05f, 10.38f, 22.48f, 2f, 16.24f, 20.9f, 22.62f, 25.68f, 24.63f, 33.15f, 4f, 0f, 47.96f, 17.04f, 2f, 48.63f, 24.74f, 44.27f, 31.42f, 38.22f, 31.95f, 2f, 32.17f, 32.48f, 26.72f, 26.66f, 26.04f, 18.96f, 2f, 25.37f, 11.26f, 29.73f, 4.58f, 35.78f, 4.05f, 2f, 41.83f, 3.52f, 47.28f, 9.34f, 47.96f, 17.04f, 4f, 0f, 73.96f, 18.96f, 2f, 73.28f, 26.66f, 67.83f, 32.48f, 61.78f, 31.95f, 2f, 55.73f, 31.42f, 51.37f, 24.74f, 52.04f, 17.04f, 2f, 52.72f, 9.34f, 58.17f, 3.52f, 64.22f, 4.05f, 2f, 70.27f, 4.58f, 74.63f, 11.26f, 73.96f, 18.96f, 4f, 0f, 96.63f, 38.85f, 2f, 94.62f, 46.32f, 88.24f, 51.1f, 82.38f, 49.52f, 2f, 76.51f, 47.95f, 73.37f, 40.62f, 75.37f, 33.15f, 2f, 77.38f, 25.68f, 83.76f, 20.9f, 89.62f, 22.48f, 2f, 95.49f, 24.05f, 98.63f, 31.38f, 96.63f, 38.85f, 4f),
                floatArrayOf(0f, 19.8f, 35.45f, 2f, 20.94f, 39.71f, 19.27f, 43.87f, 16.07f, 44.73f, 2f, 12.87f, 45.59f, 9.35f, 42.82f, 8.2f, 38.55f, 2f, 7.06f, 34.29f, 8.73f, 30.13f, 11.93f, 29.27f, 2f, 15.13f, 28.41f, 18.65f, 31.18f, 19.8f, 35.45f, 4f, 0f, 42.98f, 18.48f, 2f, 43.36f, 22.88f, 41f, 26.68f, 37.7f, 26.97f, 2f, 34.4f, 27.26f, 31.41f, 23.92f, 31.02f, 19.52f, 2f, 30.64f, 15.12f, 33f, 11.32f, 36.3f, 11.03f, 2f, 39.6f, 10.74f, 42.59f, 14.08f, 42.98f, 18.48f, 4f, 0f, 68.98f, 19.52f, 2f, 68.59f, 23.92f, 65.6f, 27.26f, 62.3f, 26.97f, 2f, 59f, 26.68f, 56.64f, 22.88f, 57.02f, 18.48f, 2f, 57.41f, 14.08f, 60.4f, 10.74f, 63.7f, 11.03f, 2f, 67f, 11.32f, 69.36f, 15.12f, 68.98f, 19.52f, 4f, 0f, 91.8f, 38.55f, 2f, 90.65f, 42.82f, 87.13f, 45.59f, 83.93f, 44.73f, 2f, 80.73f, 43.87f, 79.06f, 39.71f, 80.2f, 35.45f, 2f, 81.35f, 31.18f, 84.87f, 28.41f, 88.07f, 29.27f, 2f, 91.27f, 30.13f, 92.94f, 34.29f, 91.8f, 38.55f, 4f),
                0xF4A3BC, 0xF4A3BC, 0.95f),
            "pixel" to KeyShapeArt(
                "像素", floatArrayOf(6f, 6f, 88f, 88f),
                floatArrayOf(0f, 12f, 0f, 1f, 88f, 0f, 1f, 88f, 6f, 1f, 94f, 6f, 1f, 94f, 12f, 1f, 100f, 12f, 1f, 100f, 88f, 1f, 94f, 88f, 1f, 94f, 94f, 1f, 88f, 94f, 1f, 88f, 100f, 1f, 12f, 100f, 1f, 12f, 94f, 1f, 6f, 94f, 1f, 6f, 88f, 1f, 0f, 88f, 1f, 0f, 12f, 1f, 6f, 12f, 1f, 6f, 6f, 1f, 12f, 6f, 4f),
                floatArrayOf(0f, 12f, 10f, 1f, 28f, 10f, 1f, 28f, 16f, 1f, 18f, 16f, 1f, 18f, 28f, 1f, 12f, 28f, 4f),
                0xFFFFFF, 0xFFFFFF, 0.75f),
            "pixelHeart" to KeyShapeArt(
                "像素心", floatArrayOf(14f, 12f, 72f, 52f),
                floatArrayOf(0f, 10f, 0f, 1f, 40f, 0f, 1f, 40f, 10f, 1f, 60f, 10f, 1f, 60f, 0f, 1f, 90f, 0f, 1f, 90f, 10f, 1f, 100f, 10f, 1f, 100f, 50f, 1f, 90f, 50f, 1f, 90f, 60f, 1f, 80f, 60f, 1f, 80f, 70f, 1f, 70f, 70f, 1f, 70f, 80f, 1f, 60f, 80f, 1f, 60f, 100f, 1f, 40f, 100f, 1f, 40f, 80f, 1f, 30f, 80f, 1f, 30f, 70f, 1f, 20f, 70f, 1f, 20f, 60f, 1f, 10f, 60f, 1f, 10f, 50f, 1f, 0f, 50f, 1f, 0f, 10f, 1f, 10f, 10f, 4f),
                floatArrayOf(0f, 10f, 10f, 1f, 22f, 10f, 1f, 22f, 18f, 1f, 16f, 18f, 1f, 16f, 26f, 1f, 10f, 26f, 4f),
                0xFFFFFF, 0xFFFFFF, 0.75f),
            "puzzle" to KeyShapeArt(
                "拼圖", floatArrayOf(6f, 26f, 72f, 68f),
                floatArrayOf(0f, 0f, 20f, 1f, 38f, 20f, 2f, 34f, 12f, 38f, 2f, 50f, 2f, 2f, 62f, 2f, 66f, 12f, 62f, 20f, 1f, 100f, 20f, 1f, 100f, 44f, 2f, 92f, 40f, 84f, 44f, 84f, 56f, 2f, 84f, 68f, 92f, 72f, 100f, 68f, 1f, 100f, 100f, 1f, 0f, 100f, 4f),
                floatArrayOf(),
                null, null, 0f),
            "book" to KeyShapeArt(
                "書本", floatArrayOf(8f, 20f, 84f, 62f),
                floatArrayOf(0f, 0f, 14f, 3f, 24f, 4f, 50f, 14f, 3f, 76f, 4f, 100f, 14f, 1f, 100f, 92f, 3f, 76f, 82f, 50f, 92f, 3f, 24f, 82f, 0f, 92f, 4f),
                floatArrayOf(0f, 49f, 16f, 1f, 51f, 16f, 1f, 51f, 90f, 1f, 49f, 90f, 4f, 0f, 0f, 92f, 3f, 24f, 82f, 50f, 92f, 3f, 76f, 82f, 100f, 92f, 1f, 100f, 98f, 3f, 76f, 88f, 50f, 98f, 3f, 24f, 88f, 0f, 98f, 4f),
                0xB98A6A, 0xD7A983, 0.85f),
            "bow" to KeyShapeArt(
                "蝴蝶結", floatArrayOf(18f, 28f, 64f, 36f),
                floatArrayOf(0f, 50f, 40f, 2f, 60f, 20f, 76f, 6f, 90f, 12f, 2f, 100f, 18f, 100f, 40f, 94f, 60f, 2f, 90f, 76f, 70f, 76f, 56f, 62f, 1f, 62f, 94f, 1f, 52f, 88f, 1f, 50f, 98f, 1f, 48f, 88f, 1f, 38f, 94f, 1f, 44f, 62f, 2f, 30f, 76f, 10f, 76f, 6f, 60f, 2f, 0f, 40f, 0f, 18f, 10f, 12f, 2f, 24f, 6f, 40f, 20f, 50f, 40f, 4f),
                floatArrayOf(0f, 58f, 48f, 2f, 58f, 53.52f, 54.42f, 58f, 50f, 58f, 2f, 45.58f, 58f, 42f, 53.52f, 42f, 48f, 2f, 42f, 42.48f, 45.58f, 38f, 50f, 38f, 2f, 54.42f, 38f, 58f, 42.48f, 58f, 48f, 4f),
                0xF29AB8, 0xF7B5CC, 0.9f),
            "butterfly" to KeyShapeArt(
                "蝴蝶", floatArrayOf(14f, 22f, 72f, 52f),
                floatArrayOf(0f, 50f, 30f, 2f, 56f, 10f, 76f, 0f, 90f, 6f, 2f, 100f, 12f, 98f, 34f, 86f, 46f, 2f, 96f, 54f, 96f, 74f, 84f, 84f, 2f, 72f, 94f, 58f, 84f, 52f, 70f, 1f, 50f, 66f, 1f, 48f, 70f, 2f, 42f, 84f, 28f, 94f, 16f, 84f, 2f, 4f, 74f, 4f, 54f, 14f, 46f, 2f, 2f, 34f, 0f, 12f, 10f, 6f, 2f, 24f, 0f, 44f, 10f, 50f, 30f, 4f),
                floatArrayOf(0f, 53.5f, 50f, 2f, 53.5f, 62.15f, 51.93f, 72f, 50f, 72f, 2f, 48.07f, 72f, 46.5f, 62.15f, 46.5f, 50f, 2f, 46.5f, 37.85f, 48.07f, 28f, 50f, 28f, 2f, 51.93f, 28f, 53.5f, 37.85f, 53.5f, 50f, 4f),
                0xB79CF0, 0xCDB8FF, 0.9f),
            "fish" to KeyShapeArt(
                "小魚", floatArrayOf(12f, 32f, 60f, 36f),
                floatArrayOf(0f, 4f, 50f, 2f, 16f, 22f, 50f, 14f, 72f, 32f, 1f, 92f, 16f, 3f, 98f, 14f, 96f, 22f, 1f, 88f, 50f, 1f, 96f, 78f, 3f, 98f, 86f, 92f, 84f, 1f, 72f, 68f, 2f, 50f, 86f, 16f, 78f, 4f, 50f, 4f),
                floatArrayOf(0f, 23.5f, 44f, 2f, 23.5f, 45.93f, 21.93f, 47.5f, 20f, 47.5f, 2f, 18.07f, 47.5f, 16.5f, 45.93f, 16.5f, 44f, 2f, 16.5f, 42.07f, 18.07f, 40.5f, 20f, 40.5f, 2f, 21.93f, 40.5f, 23.5f, 42.07f, 23.5f, 44f, 4f),
                0x4A5A8A, 0xDDE6FF, 0.9f),
            "strawberry" to KeyShapeArt(
                "草莓", floatArrayOf(16f, 34f, 68f, 48f),
                floatArrayOf(0f, 50f, 22f, 2f, 70f, 18f, 92f, 22f, 94f, 40f, 2f, 96f, 64f, 72f, 92f, 50f, 98f, 2f, 28f, 92f, 4f, 64f, 6f, 40f, 2f, 8f, 22f, 30f, 18f, 50f, 22f, 4f),
                floatArrayOf(0f, 50f, 26f, 1f, 38f, 8f, 1f, 47f, 15f, 1f, 50f, 2f, 1f, 53f, 15f, 1f, 62f, 8f, 4f, 0f, 28f, 22f, 3f, 40f, 12f, 50f, 24f, 3f, 60f, 12f, 72f, 22f, 3f, 60f, 32f, 50f, 27f, 3f, 40f, 32f, 28f, 22f, 4f),
                0x74B86E, 0x8FD08A, 0.95f),
            "flower" to KeyShapeArt(
                "花花", floatArrayOf(26f, 32f, 48f, 44f),
                floatArrayOf(0f, 68f, 22f, 2f, 68f, 34.15f, 59.94f, 44f, 50f, 44f, 2f, 40.06f, 44f, 32f, 34.15f, 32f, 22f, 2f, 32f, 9.85f, 40.06f, 0f, 50f, 0f, 2f, 59.94f, 0f, 68f, 9.85f, 68f, 22f, 4f, 0f, 86.56f, 61.12f, 2f, 75.01f, 64.87f, 63.15f, 60.25f, 60.08f, 50.8f, 2f, 57f, 41.34f, 63.88f, 30.64f, 75.44f, 26.88f, 2f, 86.99f, 23.13f, 98.85f, 27.75f, 101.92f, 37.2f, 2f, 105f, 46.66f, 98.12f, 57.36f, 86.56f, 61.12f, 4f, 0f, 54.44f, 90.58f, 2f, 47.3f, 80.75f, 48.03f, 68.04f, 56.07f, 62.2f, 2f, 64.11f, 56.36f, 76.42f, 59.59f, 83.56f, 69.42f, 2f, 90.7f, 79.25f, 89.97f, 91.96f, 81.93f, 97.8f, 2f, 73.89f, 103.64f, 61.58f, 100.41f, 54.44f, 90.58f, 4f, 0f, 16.44f, 69.42f, 2f, 23.58f, 59.59f, 35.89f, 56.36f, 43.93f, 62.2f, 2f, 51.97f, 68.04f, 52.7f, 80.75f, 45.56f, 90.58f, 2f, 38.42f, 100.41f, 26.11f, 103.64f, 18.07f, 97.8f, 2f, 10.03f, 91.96f, 9.3f, 79.25f, 16.44f, 69.42f, 4f, 0f, 24.56f, 26.88f, 2f, 36.12f, 30.64f, 43f, 41.34f, 39.92f, 50.8f, 2f, 36.85f, 60.25f, 24.99f, 64.87f, 13.44f, 61.12f, 2f, 1.88f, 57.36f, -5f, 46.66f, -1.92f, 37.2f, 2f, 1.15f, 27.75f, 13.01f, 23.13f, 24.56f, 26.88f, 4f, 0f, 80f, 54f, 2f, 80f, 70.57f, 66.57f, 84f, 50f, 84f, 2f, 33.43f, 84f, 20f, 70.57f, 20f, 54f, 2f, 20f, 37.43f, 33.43f, 24f, 50f, 24f, 2f, 66.57f, 24f, 80f, 37.43f, 80f, 54f, 4f),
                floatArrayOf(0f, 57f, 54f, 2f, 57f, 57.87f, 53.87f, 61f, 50f, 61f, 2f, 46.13f, 61f, 43f, 57.87f, 43f, 54f, 2f, 43f, 50.13f, 46.13f, 47f, 50f, 47f, 2f, 53.87f, 47f, 57f, 50.13f, 57f, 54f, 4f),
                0xF6CD5B, 0xF6D877, 0.95f),
            "moon" to KeyShapeArt(
                "月亮", floatArrayOf(10f, 32f, 44f, 40f),
                floatArrayOf(0f, 62f, 4f, 2f, 46f, 10f, 36f, 20f, 34f, 34f, 2f, 30f, 56f, 44f, 74f, 68f, 78f, 2f, 78f, 80f, 88f, 78f, 98f, 72f, 2f, 92f, 88f, 76f, 98f, 56f, 98f, 2f, 28f, 98f, 6f, 80f, 6f, 54f, 2f, 6f, 28f, 30f, 6f, 62f, 4f, 4f),
                floatArrayOf(),
                null, null, 0f),
            "sun" to KeyShapeArt(
                "太陽", floatArrayOf(22f, 22f, 56f, 56f),
                floatArrayOf(0f, 100f, 50f, 1f, 84.77f, 59.32f, 1f, 93.3f, 75f, 1f, 75.46f, 75.46f, 1f, 75f, 93.3f, 1f, 59.32f, 84.77f, 1f, 50f, 100f, 1f, 40.68f, 84.77f, 1f, 25f, 93.3f, 1f, 24.54f, 75.46f, 1f, 6.7f, 75f, 1f, 15.23f, 59.32f, 1f, 0f, 50f, 1f, 15.23f, 40.68f, 1f, 6.7f, 25f, 1f, 24.54f, 24.54f, 1f, 25f, 6.7f, 1f, 40.68f, 15.23f, 1f, 50f, 0f, 1f, 59.32f, 15.23f, 1f, 75f, 6.7f, 1f, 75.46f, 24.54f, 1f, 93.3f, 25f, 1f, 84.77f, 40.68f, 4f, 0f, 86f, 50f, 2f, 86f, 69.88f, 69.88f, 86f, 50f, 86f, 2f, 30.12f, 86f, 14f, 69.88f, 14f, 50f, 2f, 14f, 30.12f, 30.12f, 14f, 50f, 14f, 2f, 69.88f, 14f, 86f, 30.12f, 86f, 50f, 4f),
                floatArrayOf(),
                null, null, 0f),
            "star" to KeyShapeArt(
                "星星", floatArrayOf(28f, 40f, 44f, 34f),
                floatArrayOf(0f, 50f, 2f, 1f, 65.28f, 32.97f, 1f, 99.45f, 37.93f, 1f, 74.73f, 62.03f, 1f, 80.56f, 96.07f, 1f, 50f, 80f, 1f, 19.44f, 96.07f, 1f, 25.27f, 62.03f, 1f, 0.55f, 37.93f, 1f, 34.72f, 32.97f, 4f),
                floatArrayOf(),
                null, null, 0f),
            "heart" to KeyShapeArt(
                "愛心", floatArrayOf(16f, 20f, 68f, 46f),
                floatArrayOf(0f, 50f, 96f, 2f, 30f, 80f, 2f, 62f, 2f, 34f, 2f, 2f, 14f, 18f, 4f, 32f, 4f, 2f, 42f, 4f, 48f, 10f, 50f, 18f, 2f, 52f, 10f, 58f, 4f, 68f, 4f, 2f, 82f, 4f, 98f, 14f, 98f, 34f, 2f, 98f, 62f, 70f, 80f, 50f, 96f, 4f),
                floatArrayOf(),
                null, null, 0f),
        )

        /** 0 = 移到、1 = 直線、2 = 三次曲線、3 = 二次曲線、4 = 收尾 */
        fun build(ops: FloatArray, r: RectF): Path {
            val p = Path()
            fun x(k: Int) = r.left + r.width() * ops[k] / 100
            fun y(k: Int) = r.top + r.height() * ops[k] / 100
            var i = 0
            while (i < ops.size) {
                when (ops[i].toInt()) {
                    0 -> { p.moveTo(x(i + 1), y(i + 2)); i += 3 }
                    1 -> { p.lineTo(x(i + 1), y(i + 2)); i += 3 }
                    2 -> { p.cubicTo(x(i + 1), y(i + 2), x(i + 3), y(i + 4), x(i + 5), y(i + 6)); i += 7 }
                    3 -> { p.quadTo(x(i + 1), y(i + 2), x(i + 3), y(i + 4)); i += 5 }
                    else -> { p.close(); i += 1 }
                }
            }
            return p
        }
    }
}
