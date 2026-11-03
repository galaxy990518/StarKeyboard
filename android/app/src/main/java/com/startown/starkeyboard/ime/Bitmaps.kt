package com.startown.starkeyboard.ime

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** 讀成差不多 maxSide 大小的圖，不先把整張大照片解開（省記憶體、不閃退） */
fun decodeSmall(path: String, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}
