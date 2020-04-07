package fr.neamar.kiss.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.Shader.TileMode;
import android.preference.PreferenceManager;
import android.util.TypedValue;

import androidx.annotation.NonNull;

public class DrawableUtils {

    // https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
    static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Drawable handleAdaptiveIcons(Context ctx, Drawable icon) {
        Bitmap outputBitmap;
        Canvas outputCanvas;
        Paint outputPaint;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String iconsPackName = prefs.getString("icons-pack", "default");

        if(icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) icon;
            Drawable bgDrawable = adaptiveIcon.getBackground();
            Drawable fgDrawable = adaptiveIcon.getForeground();

            int layerSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108f, ctx.getResources().getDisplayMetrics()));
            int iconSize = Math.round(layerSize / (1 + 2 * adaptiveIcon.getExtraInsetFraction()));
            int layerOffset = (layerSize - iconSize) / 2;

            // Create a bitmap of the icon to use it as the shader of the outputBitmap
            Bitmap iconBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            Canvas iconCanvas = new Canvas(iconBitmap);

            // Stretch adaptive layers because they are 108dp and the icon size is 48dp
            bgDrawable.setBounds(-layerOffset, -layerOffset, iconSize+layerOffset, iconSize+layerOffset);
            bgDrawable.draw(iconCanvas);

            fgDrawable.setBounds(-layerOffset, -layerOffset, iconSize+layerOffset, iconSize+layerOffset);
            fgDrawable.draw(iconCanvas);

            outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = new Canvas(outputBitmap);
            outputPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outputPaint.setShader(new BitmapShader(iconBitmap, TileMode.CLAMP, TileMode.CLAMP));

            setIconShape(outputCanvas, outputPaint, iconsPackName);
        }
        // If icon is not adaptive, put it in a white canvas to make it have a unified shape
        else {
            int iconSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, ctx.getResources().getDisplayMetrics()));

            outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = new Canvas(outputBitmap);
            outputPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outputPaint.setARGB(255,255,255,255);

            // Shrink icon to 70% of its size so that it fits the shape
            int topLeftCorner = Math.round(0.15f*iconSize);
            int bottomRightCorner = Math.round(0.85f*iconSize);
            icon.setBounds(topLeftCorner, topLeftCorner, bottomRightCorner, bottomRightCorner);

            setIconShape(outputCanvas, outputPaint, iconsPackName);
            icon.draw(outputCanvas);
        }
        return new BitmapDrawable(ctx.getResources(), outputBitmap);
    }

    // Set the icon shape for adaptive icons
    private static void setIconShape(Canvas canvas, Paint paint, String iconsPackName) {
        int iconSize = canvas.getHeight();

        if(iconsPackName.equalsIgnoreCase("squircle")) {
            // |x|^n + |y|^n = |r|^n with n = 3
            int radius = iconSize / 2;
            double radiusToPow = radius * radius * radius;

            Path path = new Path();
            path.moveTo(-radius, 0);
            for (int x = -radius ; x <= radius ; x++)
                path.lineTo(x, ((float) Math.cbrt(radiusToPow - Math.abs(x * x * x))));
            for (int x = radius ; x >= -radius ; x--)
                path.lineTo(x, ((float) -Math.cbrt(radiusToPow - Math.abs(x * x * x))));
            path.close();

            Matrix matrix = new Matrix();
            matrix.postTranslate(radius, radius);
            path.transform(matrix);

            canvas.drawPath(path, paint);
        } else if(iconsPackName.equalsIgnoreCase("square")) {
            canvas.drawRoundRect(0f, 0f, iconSize, iconSize, iconSize/8, iconSize/12, paint);
        } else if(iconsPackName.equalsIgnoreCase("circle")) {
            int radius = iconSize / 2;
            canvas.drawCircle(radius, radius, radius, paint);
        }
    }
}
