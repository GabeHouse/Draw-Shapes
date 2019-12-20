package com.example.drawshapes;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.os.Bundle;

import java.util.ArrayList;

public class MainActivity extends Activity {

	DrawingView dv ;
	private Paint mPaint;
	int toolW = 100, toolH = 100;
	int selectionToolX = 10, selectionToolY = 15;
	int eraseToolX = selectionToolX + toolW + 20, eraseToolY = selectionToolY;
	int rectToolX = eraseToolX + toolW + 60, rectToolY = selectionToolY;
	int circleToolX = rectToolX + toolW + 20, circleToolY = selectionToolY;
	int lineToolX = circleToolX + toolW + 20, lineToolY = selectionToolY;
	int redToolX = lineToolX + toolW + 60, redToolY = selectionToolY;
	int yellowToolX = redToolX + toolW + 20, yellowToolY = selectionToolY;
	int blueToolX = yellowToolX + toolW + 20, blueToolY = selectionToolY;
	int ocx, ocy, ocrad;
	int canvasW, canvasH;
	Rect toolBarRect;
	Rec rectBeingDragged;
	Rect tempRect = null;
	Line tempLine = null;
	boolean resizing = false;
	Shape selectedShape = null;
	OrientationEventListener mOrientationListener;
	private ScaleGestureDetector mScaleDetector;
	private float mScaleFactor = 1.f;
	ArrayList<Tool> tools = new ArrayList<>();
	int config = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dv = new DrawingView(this);
		setContentView(dv);
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(Color.GREEN);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(12);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());

		mOrientationListener = new OrientationEventListener(this,
				SensorManager.SENSOR_DELAY_NORMAL) {

			@Override
			public void onOrientationChanged(int orientation) {
	//			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				Log.d("gab", "orientation change " + orientation);
				if (config != 1 && ((orientation >= 225 && orientation <= 315))) {
					config = 1;
					dv.invalidate();
				} else if (config != 0 && ((orientation > 315 || orientation < 45))) {
					config = 0;
					dv.invalidate();
				} else if (config != 2 && ((orientation < 225 && orientation > 135))) {
					config = 2;
					dv.invalidate();
				} else if (config != 3 && (orientation <= 135 && orientation >= 45)) {
					config = 3;
					dv.invalidate();
				}

			}
		};
		mOrientationListener.enable();
	}
	private class ScaleListener
			extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor();

			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
			if (selectedShape != null) {
				resizing = true;
				if (selectedShape instanceof Rec) {
					Rec rec = ((Rec) selectedShape);
					int ow = tempRect.right - tempRect.left;
					int cx = tempRect.left + ow/2;
					int oh = tempRect.bottom - tempRect.top;
					int cy = tempRect.top + oh/2;
					rec.rect.left = (int) (cx - ow/2 * mScaleFactor);
					rec.rect.right = (int) (cx + ow/2 * mScaleFactor);
					rec.rect.top = (int) (cy - oh/2 * mScaleFactor);
					rec.rect.bottom = (int) (cy + oh/2 * mScaleFactor);

				} else if (selectedShape instanceof Circle) {

					((Circle) selectedShape).path.reset();
					((Circle) selectedShape).rad = (int) (ocrad*mScaleFactor);
					((Circle) selectedShape).path.addCircle(((Circle) selectedShape).cx, ((Circle) selectedShape).cy,
							((Circle) selectedShape).rad, Path.Direction.CW);

					Log.d("gab", "rad =  " + ((Circle) selectedShape).rad);
				} else {
					Line line = ((Line) selectedShape);
					int ocx = (tempLine.x1 + tempLine.x2)/2;
					int ocy = (tempLine.y1 + tempLine.y2)/2;
					int ox1d = tempLine.x1 - ocx;
					int ox2d = tempLine.x2 - ocx;
					int oy1d = tempLine.y1 - ocy;
					int oy2d = tempLine.y2 - ocy;
					line.x1 = (int) (ocx + ox1d*mScaleFactor);
					line.x2 = (int) (ocx + ox2d*mScaleFactor);
					line.y1 = (int) (ocy + oy1d*mScaleFactor);
					line.y2 = (int) (ocy + oy2d*mScaleFactor);

				}
			}

			dv.invalidate();
			return true;
		}
	}
	public class DrawingView extends View {
		ArrayList<Shape> shapes = new ArrayList<>();

		public int width;
		public  int height;
		private Bitmap mBitmap;
		private Canvas mCanvas;
		private Path mPath;
		private Paint   mBitmapPaint;
		Context context;
		private Paint circlePaint;
		private Path circlePath;
		private Circle circleBeingDragged;
		private Line lineBeingDragged;
		private Rect outlineRect;
		private Path outlineCircle;
		private int outlineCircleX = 0, outlineCircleY = 0, outlineCircleRad = 0;
		private Line outlineLine;
		private Tool selectedTool = null;



		RectTool rectTool = new RectTool(rectToolX, rectToolY, toolW, toolH, shapes);
		CircleTool circleTool = new CircleTool(circleToolX, rectToolY, toolW, toolH, shapes);
		LineTool lineTool = new LineTool(lineToolX, rectToolY, toolW, toolH, shapes);
		SelectTool selectTool = new SelectTool(selectionToolX, rectToolY, toolW, toolH);
		EraseTool eraseTool = new EraseTool(eraseToolX, rectToolY, toolW, toolH);
		RedTool redTool = new RedTool(redToolX, rectToolY, toolW, toolH);
		YellowTool yellowTool = new YellowTool(yellowToolX, rectToolY, toolW, toolH);
		BlueTool blueTool = new BlueTool(blueToolX, rectToolY, toolW, toolH);
		int selectedColor = Color.RED;
		public DrawingView(Context c) {
			super(c);

			context=c;
			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
			circlePaint = new Paint();
			circlePath = new Path();
			circlePaint.setAntiAlias(true);
			circlePaint.setColor(Color.BLUE);
			circlePaint.setStyle(Paint.Style.STROKE);
			circlePaint.setStrokeJoin(Paint.Join.MITER);
			circlePaint.setStrokeWidth(4f);

			tools.add(selectTool);
			tools.add(eraseTool);
			tools.add(rectTool);
			tools.add(circleTool);
			tools.add(lineTool);
			tools.add(redTool);
			tools.add(yellowTool);
			tools.add(blueTool);
			eraseTool.disabled = true;
			rectTool.disabled = false;
			circleTool.disabled = false;
			lineTool.disabled = false;
			selectTool.disabled = false;
			redTool.disabled = false;
			yellowTool.disabled = false;
			blueTool.disabled = false;
			redTool.selected = true;
		}
		boolean first = true;
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			if (first) {
				mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
				mCanvas = new Canvas(mBitmap);
				first = false;
			}
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			toolBarRect = new Rect(0,0, canvas.getWidth(), toolH + 30);
			Paint red = new Paint(Paint.ANTI_ALIAS_FLAG);
			red.setColor(selectedColor);
			red.setStrokeWidth(7);
			red.setStyle(Paint.Style.STROKE);
			for (Shape s : shapes) {
				s.draw(canvas);
			}

			if (outlineRect != null)
				canvas.drawRect(outlineRect, red);
			if (outlineCircle != null)
				canvas.drawPath(outlineCircle, red);
			if (outlineLine != null)
				outlineLine.draw(canvas);

			red.setColor(Color.WHITE);
			red.setStyle(Paint.Style.FILL);

			if (config == 0) {
				toolBarRect = new Rect(0, 0, canvas.getWidth(), toolH + 30);
				for (int i = 0; i < tools.size(); i++) {
					tools.get(i).y = 15;
					tools.get(i).x = 15 + i*(20 + toolW);
				}
			} else if (config == 1) {
				toolBarRect = new Rect(0, 0, canvas.getWidth(), toolH + 30);
				for (int i = 0; i < tools.size(); i++) {
					tools.get(i).y = 15;
					tools.get(i).x = canvas.getWidth() - 15 - toolW - i*(20+toolW);
				}
			} else if (config == 2) {
				toolBarRect = new Rect(0, 0, canvas.getWidth(), toolH + 30);
				for (int i = 0; i < tools.size(); i++) {
					tools.get(i).y = 15;
					tools.get(i).x = canvas.getWidth() - 15 - toolW - i*(20+toolW);
				}
			} else if (config == 3) {
				toolBarRect = new Rect(0, canvas.getHeight() - 30 - toolH, canvas.getWidth(), canvas.getHeight());
				for (int i = 0; i < tools.size(); i++) {
					tools.get(i).y = canvas.getHeight() - 15 - toolH;
					tools.get(i).x = 15 + i*(20 + toolW);
				}
			}
			canvas.drawRect(toolBarRect, red);
			for (Tool t : tools) {
				t.draw(canvas);
			}
			canvas.drawBitmap( mBitmap, 0, 0, mBitmapPaint);
			//canvas.drawPath( mPath,  mPaint);
			canvas.drawPath( circlePath,  circlePaint);
		}

		private float mX, mY, oX, oY;
		private static final float TOUCH_TOLERANCE = 4;

		private void touch_start(float x, float y) {
			if (toolBarRect.contains((int)x,(int)y)) {
				for (Tool t : tools) {
					Log.d("DEBUG", "toolexist ");
					if (t.contains(x, y) && !t.disabled) {
						Log.d("DEBUG", "contains ");
						if (t.equals(redTool)) {
							selectedColor = Color.RED;
							redTool.selected = true;
							yellowTool.selected = false;
							blueTool.selected = false;
							if (selectedShape != null)
								selectedShape.color = selectedColor;
						} else if (t.equals(yellowTool)) {
							selectedColor = Color.YELLOW;
							redTool.selected = false;
							yellowTool.selected = true;
							blueTool.selected = false;
							if (selectedShape != null)
								selectedShape.color = selectedColor;
						} else if (t.equals(blueTool)) {
							redTool.selected = false;
							yellowTool.selected = false;
							blueTool.selected = true;
							selectedColor = Color.BLUE;
							if (selectedShape != null)
								selectedShape.color = selectedColor;

						} else if (t.equals(rectTool)) {
							if (selectedTool != null)
								selectedTool.selected = false;
							selectedTool = t;
							t.selected = true;

						} else if (t.equals(eraseTool)) {
							shapes.remove(selectedShape);
							eraseTool.disabled = true;
							selectedShape = null;
							rectTool.disabled = false;
							circleTool.disabled = false;
							lineTool.disabled = false;
						} else {
							if (selectedTool != null)
								selectedTool.selected = false;
							selectedTool = t;
							t.selected = true;
						}

					}
				}
			} else {
				boolean shapeSelected = false;
				if (selectedTool == rectTool) {
					outlineRect = new Rect((int) x, (int) y, (int) x, (int) y);
				} else if (selectedTool == circleTool) {
					outlineCircle = new Path();
					outlineCircle.addCircle((int) x, (int) y, 0, Path.Direction.CW);
				} else if (selectedTool == lineTool) {
					outlineLine = new Line((int) x, (int) y, (int) x, (int) y);
					outlineLine.color = selectedColor;
				} else if (selectedTool == selectTool) {
					Log.d("DEBUG", "reccount =  " + shapes.size());
					int shapesSize = shapes.size();
					for (int i = shapesSize - 1; i > -1; i--) {
						if (shapes.get(i).contains((int) x, (int) y)) {
							if (shapes.get(i) instanceof Rec) {
								rectBeingDragged = (Rec) shapes.get(i);


								tempRect = new Rect(((Rec) shapes.get(i)).rect.left, ((Rec) shapes.get(i)).rect.top,
										((Rec) shapes.get(i)).rect.right, ((Rec) shapes.get(i)).rect.bottom);
								//			outlineRect = new Rect(rectBeingDragged.rect.left, rectBeingDragged.rect.top,
								//					rectBeingDragged.rect.right, rectBeingDragged.rect.bottom);
							} else if (shapes.get(i) instanceof Circle) {
								circleBeingDragged = (Circle) shapes.get(i);
								ocx = ((Circle) shapes.get(i)).cx;
								ocy = ((Circle) shapes.get(i)).cy;
								ocrad = ((Circle) shapes.get(i)).rad;
							} else if (shapes.get(i) instanceof Line) {
								lineBeingDragged = (Line) shapes.get(i);
								tempLine = new Line(lineBeingDragged.x1, lineBeingDragged.y1, lineBeingDragged.x2, lineBeingDragged.y2);
							}
							if (selectedShape != null)
								selectedShape.selected = false;
							selectedShape = shapes.get(i);
							selectedShape.selected = true;
							shapeSelected = true;
							eraseTool.disabled = false;
							rectTool.disabled = true;
							circleTool.disabled = true;
							lineTool.disabled = true;
							break;
						}
					}
					if (shapeSelected == false && selectedShape != null) {
						selectedShape.selected = false;
						selectedShape = null;
						eraseTool.disabled = true;
						rectTool.disabled = false;
						circleTool.disabled = false;
						lineTool.disabled = false;
					}
				}

			}
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
			oX = x;
			oY = y;
		}

		private void touch_move(float x, float y) {
			if (!resizing) {
			float DX = Math.abs(x - mX);
			float DY = Math.abs(y - mY);
			int dx = (int) (x - oX);
			int dy = (int) (y - oY);
			if (outlineRect != null) {
				outlineRect.left = (int) oX;
				outlineRect.top = (int) oY;
				outlineRect.right = (int) x;
				outlineRect.bottom = (int) y;

			} else if (outlineCircle != null) {
				outlineCircle.reset();
				outlineCircleX = (int) oX;
				outlineCircleY = (int) oY;
				outlineCircleRad = (int) Math.sqrt(dx * dx + dy * dy);
				outlineCircle.addCircle(oX, oY, outlineCircleRad, Path.Direction.CW);
			} else if (outlineLine != null) {
				outlineLine.x2 = (int) x;
				outlineLine.y2 = (int) y;
			}
			if (lineBeingDragged != null) {
				lineBeingDragged.x1 = tempLine.x1 + dx;
				lineBeingDragged.y1 = tempLine.y1 + dy;
				lineBeingDragged.x2 = tempLine.x2 + dx;
				lineBeingDragged.y2 = tempLine.y2 + dy;
			}
			if (circleBeingDragged != null) {
				circleBeingDragged.cx = ocx + dx;
				circleBeingDragged.cy = ocy + dy;
				circleBeingDragged.path.reset();
				circleBeingDragged.path.addCircle(circleBeingDragged.cx, circleBeingDragged.cy, circleBeingDragged.rad, Path.Direction.CW);
			}
			if (rectBeingDragged != null) {
				Log.d("DEBUG", "dx =  " + dx + ", dy = ");

				rectBeingDragged.rect.left = tempRect.left + dx;
				rectBeingDragged.rect.top = tempRect.top + dy;
				rectBeingDragged.rect.right = tempRect.right + dx;
				rectBeingDragged.rect.bottom = tempRect.bottom + dy;
			}
		}
		}

		private void touch_up() {
			resizing = false;
			mScaleFactor = 1;
	/*		if (rectBeingDragged != null) {
				rectBeingDragged.rect.top = outlineRect.top;
				rectBeingDragged.rect.left = outlineRect.left;
				rectBeingDragged.rect.bottom = outlineRect.bottom;
				rectBeingDragged.rect.right = outlineRect.right;
				rectBeingDragged = null;
				outlineRect = null;
			}*/
			circleBeingDragged = null;
			if (outlineCircle != null) {
				Circle nc = new Circle(outlineCircleX, outlineCircleY, outlineCircleRad);
				nc.path = new Path(outlineCircle);
				outlineCircle = null;
				nc.color = selectedColor;
				shapes.add(nc);
			}
			lineBeingDragged = null;
			if (outlineLine != null) {
				Line nl = new Line(outlineLine.x1, outlineLine.y1, outlineLine.x2, outlineLine.y2);
				outlineLine = null;
				nl.color = selectedColor;
				shapes.add(nl);
			}
			rectBeingDragged = null;
			if (outlineRect != null) {
				int l = outlineRect.left;
				int r = outlineRect.right;
				int t = outlineRect.top;
				int b = outlineRect.bottom;
				if (outlineRect.left > outlineRect.right){
					l = outlineRect.right;
					r = outlineRect.left;
				}
				if (outlineRect.top > outlineRect.bottom){
					t = outlineRect.bottom;
					b = outlineRect.top;
				}
				Rec nr = new Rec(0,0,0,0);
				nr.rect.left = l;
				nr.rect.top = t;
				nr.rect.right = r;
				nr.rect.bottom = b;
				nr.color = selectedColor;
				shapes.add(nr);
				outlineRect = null;
			}



			mPath.lineTo(mX, mY);
			circlePath.reset();
			// commit the path to our offscreen
		//	mCanvas.drawPath(mPath,  mPaint);
			// kill this so we don't double draw
			mPath.reset();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			mScaleDetector.onTouchEvent(event);
			float x = event.getX();
			float y = event.getY();

			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					touch_start(x, y);
					invalidate();
					break;
				case MotionEvent.ACTION_MOVE:
					touch_move(x, y);
					invalidate();
					break;
				case MotionEvent.ACTION_UP:
					touch_up();
					invalidate();
					break;
			}
			return true;
		}
	}
	public class Tool {
		int x,y,w,h;
		boolean disabled = true, selected = false;
		Tool(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
		boolean contains(float x, float y) {
			if (x >= this.x && x <= this.x + this.w && y >= this.y && y <= this.y + this.h)
				return true;
			return false;
		}
		void draw(Canvas c) {
			Paint black = new Paint(Paint.ANTI_ALIAS_FLAG);
			black.setColor(Color.BLACK);
			Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
			if (disabled) {
				background.setColor(Color.LTGRAY);
				c.drawRect(new Rect(x,y,x + w, y + h), background);
			}
			if (selected) {
				background.setColor(Color.GREEN);
				background.setStyle(Paint.Style.STROKE);
				background.setStrokeWidth(12);
				c.drawRect(new Rect(x, y, x + w, y + h), background);
			}
			black.setStyle(Paint.Style.STROKE);
			c.drawRect(new Rect(x,y,x + w, y + h), black);

		}
		boolean onPress() {
			return true;
		}

	}
	public class RectTool extends Tool {
		ArrayList<Shape> shapes;
		RectTool(int x, int y, int w, int h, ArrayList<Shape> shapes) {
			super(x,y,w,h);
			this.shapes = shapes;
		}

		@Override
		void draw(Canvas c) {
			super.draw(c);
			Paint p = new Paint();
			p.setColor(Color.BLACK);
			p.setStrokeWidth(7);
			c.drawRect(new Rect(x + 20, y + 20, x + w - 20, y + h - 20), p);
		}
	}
	public class CircleTool extends Tool {
		ArrayList<Shape> shapes;
		CircleTool(int x, int y, int w, int h, ArrayList<Shape> shapes) {
			super(x,y,w,h);
			this.shapes = shapes;
		}
		@Override
		void draw(Canvas c) {
			super.draw(c);
			Paint p = new Paint();
			p.setColor(Color.BLACK);
			p.setStrokeWidth(7);
			c.drawCircle(x + w /2, y + h/2, 30, p);
		}
	}
	public class LineTool extends Tool {
		ArrayList<Shape> shapes;
		LineTool(int x, int y, int w, int h, ArrayList<Shape> shapes) {
			super(x,y,w,h);
			this.shapes = shapes;
		}
		@Override
		void draw(Canvas c) {
			super.draw(c);
			Paint p = new Paint();
			p.setColor(Color.BLACK);
			p.setStrokeWidth(7);
			c.drawLine(x + 20, y + h - 20, x + w - 20, y + 20, p);
		}
	}

	public class SelectTool extends Tool {
		SelectTool(int x, int y, int w, int h) {
			super(x,y,w,h);
		}
		@Override
		void draw(Canvas c) {
			super.draw(c);
			Paint p = new Paint();
			p.setColor(Color.BLACK);
			p.setStrokeWidth(7);
			int rx = x + 20;
			int ry = y + 28;
			while (rx < x + w - 20 - 10) {
				c.drawLine(rx, y + 25, rx + 10, y + 25, p);
				rx += 15;
			}
			rx = x + 20;
			while (rx < x + w - 20 - 10) {
				c.drawLine(rx, y + h - 25, rx + 10, y + h - 25, p);
				rx += 15;
			}
			while (ry < y + h - 25 - 10) {
				c.drawLine(x + 20, ry, x + 20, ry + 10, p);
				ry += 15;
			}
			ry = y + 28;
			while (ry < y + h - 25 - 10) {
				c.drawLine(x + w - 20, ry, x + w - 20, ry + 10, p);
				ry += 15;
			}
		}
	}

	public class EraseTool extends Tool {
		EraseTool(int x, int y, int w, int h) {
			super(x,y,w,h);
		}
		@Override
		void draw(Canvas c) {
			super.draw(c);
			Paint p = new Paint();
			p.setColor(Color.RED);
			p.setStrokeWidth(7);
			c.drawLine(x + 20, y + h - 20, x + w - 20, y + 20, p);
			c.drawLine(x + 20, y + 20, x + w - 20, y + h - 20, p);
		}
	}
	public class RedTool extends Tool {
		RedTool(int x, int y, int w, int h) {
			super(x,y,w,h);
		}
		@Override
		void draw(Canvas c) {
			super.draw(c);
			Paint p = new Paint();
			p.setColor(Color.RED);
			p.setStrokeWidth(7);
			c.drawRect(new Rect(x, y, x + w, y + h), p);
		}
	}
	public class YellowTool extends Tool {
		YellowTool(int x, int y, int w, int h) {
			super(x,y,w,h);
		}
		@Override
		void draw(Canvas c) {
			super.draw(c);
			Paint p = new Paint();
			p.setColor(Color.YELLOW);
			p.setStrokeWidth(7);
			c.drawRect(new Rect(x, y, x + w, y + h), p);
		}
	}
	public class BlueTool extends Tool {
		BlueTool(int x, int y, int w, int h) {
			super(x,y,w,h);
		}
		@Override
		void draw(Canvas c) {
			super.draw(c);
			Paint p = new Paint();
			p.setColor(Color.BLUE);
			p.setStrokeWidth(7);
			c.drawRect(new Rect(x, y, x + w, y + h), p);
		}
	}
	//SHAPES
	public class Shape {
		boolean selected = false;
		int color = Color.RED;
		Shape() {
		}
		public void draw(Canvas c) {
		}
		public boolean contains(int x, int y) {
			return true;
		}
	}
	public class Rec extends Shape {
		Rect rect;

		Rec(int x, int y, int w, int h) {
			rect = new Rect(x, y, x + w, y + h);
		}
		@Override
		public void draw(Canvas c) {
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setColor(color);

			c.drawRect(rect, paint);
			if (selected) {
				Rect selectRect = new Rect(rect.left, rect.top, rect.right, rect.bottom);
				Paint selectPaint = new Paint();
				selectPaint.setStyle(Paint.Style.STROKE);
				selectPaint.setStrokeWidth(10);
				selectPaint.setColor(Color.BLACK);
				c.drawRect(selectRect, selectPaint);
			}
		}
		@Override
		public boolean contains(int x, int y) {
			if (rect.contains(x, y)) {
				return true;
			}
			return false;
		}
	}
	public class Circle extends Shape {
		Path path;
		int cx,cy,rad;
		Circle(int cx, int cy, int rad) {
			this.cx = cx;
			this.cy = cy;
			this.rad = rad;
		}
		@Override
		public void draw(Canvas c) {
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setColor(color);

			c.drawPath(path, paint);
			if (selected) {
				Path selectPath = new Path(path);
				Paint selectPaint = new Paint();
				selectPaint.setStyle(Paint.Style.STROKE);
				selectPaint.setStrokeWidth(10);
				selectPaint.setColor(Color.BLACK);
				c.drawPath(selectPath, selectPaint);
			}
		}
		@Override
		public boolean contains(int x, int y) {
			if (Math.sqrt((cx - x)*(cx - x) + (cy - y)*(cy - y)) <= rad) {
				return true;
			}
			return false;
		}
	}
	public class Line extends Shape {
		int x1,y1,x2,y2;
		Line(int x1, int y1, int x2, int y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
		@Override
		public void draw(Canvas c) {
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setColor(color);
			paint.setStrokeWidth(10);
			if (selected)
				paint.setColor(Color.BLACK);
			c.drawLine(x1,y1,x2,y2, paint);
		}
		@Override
		public boolean contains(int x, int y) {
			double run = x2 - x1;
			if (run < 1) {
				run = 1;
			}
			double m = (double)(y2 - y1)/ run;

			double b = y1 - m*x1;
			int lx = x1, rx = x2, uy= y1, by = y2;
			if (x1 > x2) {
				lx = x2;
				rx = x1;
			}
			if (y1 > y2) {
				uy = y2;
				by = y1;
			}
			double total;
			if (Math.abs(m) > 1) {
				total = Math.abs((y - m * x - b) / m);
			} else {
				total = Math.abs((y - m * x - b));
			}
			Log.d("DEBUG", "line contains slope = " + m + ", b = " + b + ", x" + x + ", y " + y + ", totals " + Math.abs(y - m * x - b)/m);
			if (total < 200 && x >= lx - 30 && x <= rx + 30 && y >= uy - 30 && y <= by + 30) {
				return true;
			}
			return false;
		}
	}

}