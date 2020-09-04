package com.metaapp.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.FloatRange;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @author zhou_hao
 * @date 2020/9/4
 * @description: 爆炸效果
 */
public class BombView extends SurfaceView implements SurfaceHolder.Callback {
    private Paint mPaint;
    private Random mRandom = new Random();
    private long mDuration = 100;
    /**
     * 控件的宽度
     */
    private int mWidth;
    /**
     * 控件的高度
     */
    private int mHeight;
    /**
     * 爆炸中心位于 View X 轴的位置
     */
    @FloatRange(from = 0, to = 1)
    private float mBombRatioX = 0.5F;
    /**
     * 爆炸中心位于 View Y 轴的位置
     */
    @FloatRange(from = 0, to = 1)
    private float mBombRatioY = 0.5F;
    /**
     * 爆炸中心 X (旋转的中心)
     */
    private int mBombX;
    /**
     * 爆炸中心 Y（旋转的中心）
     */
    private int mBombY;
    /**
     * 爆炸范围
     */
    @FloatRange(from = 0, to = 1)
    private float mBombRangRatioY = 0.5F;
    /**
     * 爆炸范围
     */
    private float mBombRangY = 0F;
    /**
     * 爆炸步调
     */
    private float mBombDeltaY = 0;
    /**
     * 爆炸进程
     */
    private float mBombDistanceY = 0;
    /**
     * 烟花/气泡数量
     */
    private int mBubbleCount = 20;
    /**
     * 是否处于消失阶段
     */
    private boolean mIsDismiss = false;
    /**
     * 绘制 UI 的线程
     */
    private DrawTask mDrawTask;
    /**
     * 引导的view
     */
    private FuseView mFuseView;
    /**
     * 存放需要展示的图
     */
    private Bitmap[] mDrawables;
    /**
     * 存放需要展示的图 id
     */
    private int[] mDrawableResIds;
    /**
     * 用于存放烟花信息
     */
    private final List<Bubble> mBubbles = Collections.synchronizedList(new LinkedList<Bubble>());

    private final int MSG_DRAW_BUBBLE = 10;

    private final int INTERVAL_DRAW_BUBBLE = 10;

    private HandlerThread mHandlerThread;

    private Handler mHandler;

    private Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_DRAW_BUBBLE) {
                mHandler.removeMessages(MSG_DRAW_BUBBLE);
                mHandler.post(mDrawTask);
                mHandler.sendEmptyMessageDelayed(MSG_DRAW_BUBBLE, INTERVAL_DRAW_BUBBLE);
            }
            return true;
        }
    };

    public BombView(Context context) {
        this(context, null);
    }

    public BombView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 设置背景透明
        setZOrderOnTop(true);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.TRANSPARENT);
        mPaint = new Paint();

        mDrawableResIds = new int[]{
                R.drawable.icon_bomb_1,
                R.drawable.icon_bomb_2,
                R.drawable.icon_bomb_3,
                R.drawable.icon_bomb_4,
                R.drawable.icon_bomb_5,
                R.drawable.icon_bomb_6,
                R.drawable.icon_bomb_7,
                R.drawable.icon_bomb_8,
                R.drawable.icon_bomb_9,
                R.drawable.icon_bomb_10,
                R.drawable.icon_bomb_11,
                R.drawable.icon_bomb_12,
                R.drawable.icon_bomb_13,
                R.drawable.icon_bomb_14,
                R.drawable.icon_bomb_15
        };
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        if (canvas == null) {
            return;
        }
        if (mFuseView == null || mFuseView.bitmap == null) {
            return;
        }

        /*
          清空界面
         */
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (!mIsDismiss) {
            mIsDismiss = mBubbles.size() > 0 && mBombDistanceY > mBombRangY;
        }

        synchronized (mBubbles) {
            /*
              绘制气泡
             */
            for (int i = mBubbles.size() - 1; i >= 0 && mBubbles.size() > 0; i--) {
                drawBubble(canvas, mBubbles.get(i));
            }
        }

        mBombDistanceY += mBombDeltaY;
        if (mBubbles.size() <= 0) {
            release();
        }
    }

    /**
     * 绘制气泡/烟花
     */
    private void drawBubble(Canvas canvas, Bubble bubble) {
        /*
          改变 y 从而改变距离 Bomb 中心的距离 配合 旋转达到扩散的效果
          故 y 轴 在 Bomb 中心下方的不展示
         */
        if (bubble.top + bubble.bitmap.getHeight() / 2F > mBombY) {
            bubble.top = bubble.top - mBombDeltaY * 2;
            return;
        }

        if (mIsDismiss) {
            bubble.alpha -= 12;
            if (bubble.alpha < 0) {
                mBubbles.remove(bubble);
                return;
            }
        }
        /*
          控制气泡/烟花扩散
         */
        bubble.top -= mBombDeltaY;
        if (bubble.scale < 2.2f) {
            bubble.scale += 0.05f;
        }

        mPaint.setAlpha(bubble.alpha);
        canvas.save();
        canvas.scale(bubble.scale, bubble.scale, bubble.left + bubble.bitmap.getWidth() / 2F, bubble.top + bubble.bitmap.getHeight() / 2F);
        canvas.rotate(bubble.rotate, mBombX, mBombY);
        canvas.drawBitmap(bubble.bitmap, bubble.left, bubble.top, mPaint);
        canvas.restore();
    }

    /**
     * 初始化
     *
     * @param bubbleCount    烟花/气泡数量
     * @param bombRatioX     爆炸中心位于 View X 轴的位置
     * @param bombRatioY     爆炸中心位于 View Y 轴的位置
     * @param bombRangRatioY 爆炸范围占 View 高度的百分比
     * @param duration       执行时间 毫秒
     */
    public void init(int bubbleCount, @FloatRange(from = 0, to = 1) float bombRatioX,
                     @FloatRange(from = 0, to = 1) float bombRatioY,
                     @FloatRange(from = 0, to = 1) float bombRangRatioY,
                     long duration, int[] drawableResIDs) {
        mBubbleCount = bubbleCount;
        mBombRatioX = bombRatioX;
        mBombRatioY = bombRatioY;
        mBombRangRatioY = bombRangRatioY;
        mDuration = duration;
        mDrawableResIds = drawableResIDs;
    }

    /**
     * 开始绘制爆炸彩蛋
     * 如果上一个效果还没结束，则不处理新的.
     */
    public void startBomb() {
        if (mFuseView != null) {
            return;
        }

        mHandlerThread = new HandlerThread("BombView");
        mHandlerThread.start();
        mHandler = new WeakRefHandler(mCallback, mHandlerThread.getLooper());

        initData();
        initFuseView();
        generateBubble(mBubbleCount);
        mHandler.sendEmptyMessage(MSG_DRAW_BUBBLE);
    }

    /**
     * 释放资源
     */
    private void release() {

//        if (mReleaseCallBack != null){
//            mReleaseCallBack.onRelease();
//        }
        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quit();
        mBubbles.clear();
        mFuseView = null;
        mHandlerThread = null;
        mHandler = null;
        for (Bitmap bitmap : mDrawables) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }

    }

    /**
     * 初始化数据
     */
    private void initData() {
        mDrawables = new Bitmap[mDrawableResIds.length];

        mBombX = (int) (mWidth * mBombRatioX);
        mBombY = (int) (mHeight * mBombRatioY);
        mBombRangY = mHeight * mBombRangRatioY;
        long drawCount = mDuration / INTERVAL_DRAW_BUBBLE;
        mBombDeltaY = mBombRangY / drawCount;
        mBombDistanceY = 0;
        mIsDismiss = false;
    }

    /**
     * 初始化引导 View
     */
    private void initFuseView() {
        mFuseView = new FuseView();
        mFuseView.bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_bomb_1);
        mFuseView.x = mBombX - mFuseView.bitmap.getWidth() / 2;
        mFuseView.y = mBombY - mFuseView.bitmap.getHeight() / 2;
    }

    /**
     * 生成气泡/烟花
     */
    private void generateBubble(int count) {
        for (int i = 0; i < count; i++) {
            Bubble bubble = new Bubble();
            bubble.bitmap = getRandBitmap();
            bubble.alpha = 155 + mRandom.nextInt(100);
            bubble.scale = 0.6f + mRandom.nextFloat() * 0.4f;

            bubble.rotate = 360 * i / count;
            if (bubble.rotate > 180) {
                bubble.rotate = bubble.rotate - 360;
            }
            bubble.left = mBombX - bubble.bitmap.getWidth() / 2F;

            float offset;
            if (i % 5 == 0) {
                offset = mBombY * 0.88f + mBombY * 0.12f * mRandom.nextFloat();
            } else if (i % 3 == 0) {
                offset = mBombY * 0.73f + mBombY * 0.15f * i / count;
            } else if (i % 2 == 0) {
                offset = mBombY * 0.43f * i / count;
            } else {
                offset = mBombY * 0.58f + mBombY * 0.15f * i / count;
            }
            bubble.top = offset - bubble.bitmap.getHeight() / 2F;
            mBubbles.add(0, bubble);
        }
    }

    /**
     * 随机获取一个bitmap
     */
    private Bitmap getRandBitmap() {
        int n = mRandom.nextInt(mDrawableResIds.length);
        Bitmap bitmap = mDrawables[n];
        if (bitmap == null || bitmap.isRecycled()) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inMutable = true;
            bitmap = BitmapFactory.decodeResource(getResources(), mDrawableResIds[n], opts);
            mDrawables[n] = bitmap;
        }
        return bitmap;
    }

    /**
     * 绘制UI的线程，只要是调用PraiseView.onDraw(canvas);
     * 并且做了锁保护（固定用法，不要轻易修改）
     * ###########################################
     */
    static class DrawTask implements Runnable {

        private final SurfaceHolder holder;
        private BombView bombView;

        public DrawTask(SurfaceHolder holder, BombView bombView) {
            this.bombView = bombView;
            this.holder = holder;
        }

        @SuppressLint("WrongCall")
        @Override
        public void run() {
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                synchronized (holder) {
                    bombView.onDraw(canvas);
                }
            } finally {
                if (canvas != null) {
                    try { // 修复umeng崩溃
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class Bubble {
        public Bitmap bitmap;
        public float scale = 1.0f; // 缩放
        public float top = 0f; // 偏移
        public float left = 0f; // 偏移
        public int rotate = 0; // 旋转
        public int alpha = 255; // 透明度

        public Bubble() {
            this.bitmap = getRandBitmap();
        }
    }

    /**
     * 实现回调弱引用的Handle
     * 防止由于内部持有导致的内存泄露
     * <p>
     * PS：
     * image1、传入的Callback不能使用匿名实现的变量，必须与使用这个Handle的对象的生命周期一致，否则会被立即释放掉了
     *
     * @author huamm
     */
    public static class WeakRefHandler extends Handler {
        private WeakReference<Callback> mWeakReference;

        public WeakRefHandler(Callback callback) {
            mWeakReference = new WeakReference<>(callback);
        }

        public WeakRefHandler(Callback callback, Looper looper) {
            super(looper);
            mWeakReference = new WeakReference<>(callback);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mWeakReference != null && mWeakReference.get() != null) {
                Callback callback = mWeakReference.get();
                callback.handleMessage(msg);
            }
        }
    }

    static class FuseView {
        public Bitmap bitmap;
        public float scale;
        public int alpha;
        public int x;
        public int y;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mDrawTask == null) {
            mDrawTask = new DrawTask(holder, this);
        }
        // 绘制背景
        // Canvas canvas = holder.lockCanvas();
        // canvas.drawColor(Color.TRANSPARENT);
        // holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}