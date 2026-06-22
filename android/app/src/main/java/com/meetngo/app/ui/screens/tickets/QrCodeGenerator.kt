package com.meetngo.app.ui.screens.tickets

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Generates a square black/white QR code bitmap for [content]. ZXing's core
 * artifact only produces a BitMatrix — there's no BarcodeEncoder helper in
 * core, so we convert the matrix to a Bitmap manually by iterating pixels.
 */
fun generateQrCodeBitmap(content: String, size: Int = 512): Bitmap {
    val bitMatrix: BitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(
                x,
                y,
                if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE,
            )
        }
    }
    return bitmap
}
