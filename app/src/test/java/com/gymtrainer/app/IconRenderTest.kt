package com.gymtrainer.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the launcher icon the way launchers mask it (circle, rounded square, themed) for review. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class IconRenderTest {

    private val size = 432

    private fun layer(drawable: android.graphics.drawable.Drawable?, tint: Int? = null): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable?.mutate()?.apply {
            if (tint != null) setTint(tint)
            setBounds(0, 0, size, size); draw(Canvas(bmp))
        }
        return bmp
    }

    private fun masked(layers: List<Bitmap>, mask: Path, bg: Int? = null): Bitmap {
        // Stack the layers normally, then keep only what's inside the launcher's mask.
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        if (bg != null) c.drawColor(bg)
        layers.forEach { c.drawBitmap(it, 0f, 0f, null) }
        val maskBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(maskBmp).drawPath(mask, Paint(Paint.ANTI_ALIAS_FLAG))
        c.drawBitmap(maskBmp, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })
        return out
    }

    @Test
    fun renderLauncherIcon() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val icon = ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher) as AdaptiveIconDrawable
        assertNotNull(icon.monochrome)
        val bg = layer(icon.background); val fg = layer(icon.foreground)
        val circle = Path().apply { addCircle(size / 2f, size / 2f, size / 2f * 0.66f / 0.72f * 0.72f, Path.Direction.CW) }
        // Launchers show the middle 72/108 of the layers; scale the mask accordingly.
        val visible = size * 72f / 108f
        val inset = (size - visible) / 2
        val circleMask = Path().apply { addCircle(size / 2f, size / 2f, visible / 2, Path.Direction.CW) }
        val squircle = Path().apply { addRoundRect(RectF(inset, inset, inset + visible, inset + visible), visible * 0.24f, visible * 0.24f, Path.Direction.CW) }
        val themed = layer(icon.monochrome, Color.parseColor("#C9D7FF"))

        val sheet = Bitmap.createBitmap(size * 3, size, Bitmap.Config.ARGB_8888)
        Canvas(sheet).apply {
            drawColor(Color.parseColor("#F2F4F8"))
            drawBitmap(masked(listOf(bg, fg), circleMask), 0f, 0f, null)
            drawBitmap(masked(listOf(bg, fg), squircle), size.toFloat(), 0f, null)
            drawBitmap(masked(listOf(themed), circleMask, Color.parseColor("#2B3A67")), size * 2f, 0f, null)
        }
        val dir = File("build/screens").apply { mkdirs() }
        File(dir, "00_launcher_icon.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(circle.isEmpty.not())
    }
}
