package de.stas.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.Button;
import de.stas.R;

public class ColorButton extends Button {
	private int color;
	private int percent;
	private Bitmap copy;
	private boolean highlighted;
	
	public ColorButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.colorButton);
		color = ta.getColor(R.styleable.colorButton_color, 0);
		percent = ta.getInt(R.styleable.colorButton_percent, 0);
		ta.recycle();
	}
	
	public boolean isHighlighted() {
		return highlighted;
	}

	@Override
	public void onDraw(Canvas c) {
		super.onDraw(c);
		buildDrawingCache();
		copy = getDrawingCache();
		if (highlighted) {
			Paint borderPaint = new Paint();
			borderPaint.setAntiAlias(true);
			borderPaint.setStyle(Style.STROKE);
			borderPaint.setColor(Color.GRAY);
			borderPaint.setStrokeWidth(3);
			borderPaint.setPathEffect(new DashPathEffect(new float[] {4, 4}, 2));
			RectF borderRect = new RectF();
			borderRect.left = 0;
			borderRect.top = 0;
			borderRect.right = getMeasuredWidth();
			borderRect.bottom = getMeasuredHeight();
			c.drawRoundRect(borderRect, 10, 10, borderPaint);
		}
		Bitmap toned = colorTone(color, highlighted ? (int)(percent + (100 - percent)/1.5) : percent, copy);
		c.drawBitmap(toned, 0, 0, new Paint());
	}
	
	public void highlight(boolean highlight) {
		this.highlighted = highlight;
		invalidate();
	}

	private Bitmap colorTone(int color, int percent, Bitmap src) {
		Bitmap result = src.copy(Config.ARGB_8888, true);
		for (int i = 0; i < src.getWidth(); i++) {
			for (int j = 0; j < src.getHeight(); j++) {
				int pixel = src.getPixel(i, j);
				int blueSrc = pixel & 0xFF;
				int greenSrc = (pixel & 0xFF00) >> 8;
				int redSrc = (pixel & 0xFF0000) >> 16;
				int alpha = (pixel & (0xFF << 24));
				if (blueSrc == greenSrc && greenSrc == redSrc && redSrc == 0) continue;
				
				int blue = color & 0xFF;
				int green = (color & 0xFF00) >> 8;
				int red = (color & 0xFF0000) >> 16;

				int blueDiff = blueSrc - blue;
				int greenDiff = greenSrc - green;
				int redDiff = redSrc - red;
				
				blueDiff = (int)(blueDiff / 100.0 * percent);
				greenDiff = (int)(greenDiff / 100.0 * percent);
				redDiff = (int)(redDiff / 100.0 * percent);
				
				blueSrc -= blueDiff;
				greenSrc -= greenDiff;
				redSrc -= redDiff;
				
				if (highlighted) {
					blueSrc += (255 - blueSrc)/3;
					redSrc += (255 - redSrc)/3;
					greenSrc += (255 - greenSrc)/3;
				}
				pixel = alpha | redSrc << 16 | greenSrc << 8 | blueSrc;
				result.setPixel(i, j, pixel);
			}
		}
		return result;
	}
}
