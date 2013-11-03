package com.almeros.android.multitouch;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.almeros.android.multitouch.gesturedetectors.MoveGestureDetector;
import com.almeros.android.multitouch.model.SeatMo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author captain_miao
 */
public class SelectMovieSeatActivity extends Activity implements OnTouchListener {
	private Matrix mMatrix = new Matrix();
    private float mScaleFactor = 1.0f;
    private float mFocusX = 0.f;
    private float mFocusY = 0.f;  

    private ScaleGestureDetector mScaleDetector;
    private MoveGestureDetector mMoveDetector;

    SeatTableView seatTableView;
    LinearLayout rowView;
    private SeatMo[][] seatTable;

    public List<SeatMo> selectedSeats;// 保存选中座位
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        initSeatTable();
        selectedSeats = new ArrayList<SeatMo>();
        seatTableView = (SeatTableView) findViewById(R.id.seatviewcont);
        rowView = (LinearLayout) findViewById(R.id.seatraw);

        //居中线的画笔
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.LTGRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);

        seatTableView.setSeatTable(seatTable);
        seatTableView.setRowSize(seatTable.length);
        seatTableView.setColumnSize(seatTable[0].length);
        seatTableView.setOnTouchListener(this);
        seatTableView.setLinePaint(paint);
        seatTableView.setDefWidth(40);
        seatTableView.invalidate();
        onChanged();

		// Setup Gesture Detectors
		mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());
		mMoveDetector = new MoveGestureDetector(this, new MoveListener());
	}

    int[] oldClick = new int[2];
   	int[] newClick = new int[2];
    boolean eatClick = true;//在缩放和移动的时候,不触发click事件

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                oldClick = getClickPoint(event);
                eatClick = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                eatClick = true;
                break;
            case MotionEvent.ACTION_UP:
                newClick = getClickPoint(event);
                int i = newClick[0];
                int j = newClick[1];
                if (!eatClick && i != -1 && j != -1 && i == oldClick[0] && j == oldClick[1] ) {
                    if (seatTable[i][j].status == 1) {
                        seatTable[i][j].status = 2;
                        selectedSeats.add(seatTable[i][j]);
                        Toast.makeText(this,seatTable[i][j].seatName, Toast.LENGTH_LONG ).show();
                    } else {
                        seatTable[i][j].status = 1;
                        selectedSeats.remove(seatTable[i][j]);
                    }
                }
                break;
        }


        mScaleDetector.onTouchEvent(event);
        mMoveDetector.onTouchEvent(event);

        mMatrix.reset();
        mMatrix.postScale(mScaleFactor, mScaleFactor);

        seatTableView.mScaleFactor = mScaleFactor;
        seatTableView.mPosX = mFocusX;
        seatTableView.mPosY = mFocusY;
        seatTableView.invalidate();
        onChanged();

		return true;
	}

    //左侧的座位列号
    public void onChanged() {
        rowView.removeAllViews();
        rowView.setPadding(getResources().getDimensionPixelSize(R.dimen.padding_1dp),
                (int) (mFocusY), 0, 0);//上下移动
        for (int i = 0; i < seatTableView.getRowSize(); i++) {
            TextView textView = new TextView(SelectMovieSeatActivity.this);
            textView.setText(seatTable[i][0].rowName);
            textView.setTextSize(9.0f * mScaleFactor);
            textView.setTextColor(Color.LTGRAY);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setLayoutParams(new ViewGroup.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, seatTableView.getSeatWidth()));
            rowView.addView(textView);
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor(); // scale change since previous event
			mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));

			return true;
		}
	}

	
	private class MoveListener extends MoveGestureDetector.SimpleOnMoveGestureListener {
		@Override
		public boolean onMove(MoveGestureDetector detector) {
			PointF d = detector.getFocusDelta();
			mFocusX += d.x;
			mFocusY += d.y;
            eatClick = d.x > 1 || d.y > 1;

			return true;
		}
	}		
	


    private void initSeatTable() {
        seatTable = new SeatMo[20][15];// mock data
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 15; j++) {
                SeatMo seat = new SeatMo();
                seat.row = i;
                seat.column = j;
                seat.rowName = String.valueOf((char)('A' + i));
                seat.seatName = seat.rowName + "排" + (j + 1) + "座";
                seat.status = randInt(-1, 1);
                seatTable[i][j] = seat;
            }
        }
    }

    public  int randInt(int min, int max) {

        Random rand = new Random();

        return rand.nextInt((max - min) + 1) + min;
    }

    int[] noSeat = {-1, -1};
    //click的坐标
    public int[] getClickPoint(MotionEvent event) {
        float currentXPosition = event.getX() - mFocusX;
        float currentYPosition = event.getY() - mFocusY;//修正坐标
        float area = seatTableView.getSeatWidth();
        for (int i = 0; i < seatTableView.getRowSize(); i++) {
            for (int j = 0; j < seatTableView.getColumnSize(); j++) {
                if ((j * area) < currentXPosition
                        && currentXPosition < j * area + area
                        && (i * area) < currentYPosition
                        && currentYPosition < i * area + area
                        && seatTable[i][j].status >= 1) {//1 和 2  才能被点击

                    return new int[]{i, j};
                }
            }
        }
        return noSeat;
    }

}