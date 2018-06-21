package dz.solc.uihandler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import dz.solc.uihandler.interfaces.Poster;
import dz.solc.uihandler.interfaces.Task;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
* creat_user: zhengzaihong 
* Email:1096877329@qq.com
* creat_date: 2018/6/20
* creat_time: 15:20
* describe
**/

public class HandlerPoster extends Handler implements Poster {

    private static int MAX_MILLIS_INSIDE_HANDLE_MESSAGE = 16;
    private final Dispatcher mAsyncDispatcher;
    private final Dispatcher mSyncDispatcher;

    /**
     * 构造初始化
     *
     * @param looper                       Handler Looper
     * @param maxMillisInsideHandleMessage 最大占用时间在主线程
     * @param onlyAsync                    If TRUE the {@link #mSyncDispatcher} same as {@link #mAsyncDispatcher}
     */
    HandlerPoster(Looper looper, int maxMillisInsideHandleMessage, boolean onlyAsync) {
        super(looper);
        // inside time
        MAX_MILLIS_INSIDE_HANDLE_MESSAGE = maxMillisInsideHandleMessage;

        // async runner
        mAsyncDispatcher = new Dispatcher(new LinkedList<Task>(),
                new Dispatcher.IPoster() {
                    @Override
                    public void sendMessage() {
                        HandlerPoster.this.sendMessage(ASYNC);
                    }
                });

        // sync runner
        if (onlyAsync) {
            mSyncDispatcher = mAsyncDispatcher;
        } else {
            mSyncDispatcher = new Dispatcher(new LinkedList<Task>(),
                    new Dispatcher.IPoster() {
                        @Override
                        public void sendMessage() {
                            HandlerPoster.this.sendMessage(SYNC);
                        }
                    });
        }
    }

    /**
     * 清除线程池
     */
    public void dispose() {
        this.removeCallbacksAndMessages(null);
        this.mAsyncDispatcher.dispose();
        this.mSyncDispatcher.dispose();
    }

    /**
     * 添加一个async事务到队列里去
     *
     * @param task {@link Task}
     */
    public void async(Task task) {
        mAsyncDispatcher.offer(task);
    }

    /**
     * 添加一个sync事务到队列里去
     *
     * @param task {@link Task}
     */
    public void sync(Task task) {
        mSyncDispatcher.offer(task);
    }

    /**
     * 运行在主线程
     *
     * @param msg 接收一个msg
     */
    @Override
    public void handleMessage(Message msg) {
        if (msg.what == ASYNC) {
            mAsyncDispatcher.dispatch();
        } else if (msg.what == SYNC) {
            mSyncDispatcher.dispatch();
        } else super.handleMessage(msg);
    }

    /**
     * 向handler 发送一个消息
     *
     * @param what 标示同步或异步
     */
    private void sendMessage(int what) {
        if (!sendMessage(obtainMessage(what))) {
            throw new RuntimeException("Could not send handler message");
        }
    }


    /**
     * 事务分发器
     */
    private static class Dispatcher {
        private final Queue<Task> mPool;
        private IPoster mPoster;
        private boolean isActive;

        Dispatcher(Queue<Task> pool, IPoster poster) {
            mPool = pool;
            mPoster = poster;
        }

        /**
         * 添加一个元素到mPool{@link #mPool}
         *
         * @param task {@link Task}
         */
        void offer(Task task) {
            synchronized (mPool) {
                // offer to queue pool
                mPool.offer(task);
                // set the task pool reference
                task.setPool(mPool);

                if (!isActive) {
                    isActive = true;
                    // send again message
                    IPoster poster = mPoster;
                    if (poster != null)
                        poster.sendMessage();
                }
            }
        }

        /**
         * mPool取出里面的事务进行分发 {@link #mPool}
         */
        void dispatch() {
            boolean rescheduled = false;
            try {
                long started = SystemClock.uptimeMillis();
                while (true) {
                    Runnable runnable = poll();
                    if (runnable == null) {
                        synchronized (mPool) {
                            // 再次检查双重同步锁 这里使用synchronized 防止同时进入了两个线程，第二个再次 mPool.poll()取出头元素会出现空指针
                            runnable = poll();
                            if (runnable == null) {
                                isActive = false;
                                return;
                            }
                        }
                    }
                    runnable.run();
                    long timeInMethod = SystemClock.uptimeMillis() - started;
                    if (timeInMethod >= MAX_MILLIS_INSIDE_HANDLE_MESSAGE) {
                        // send again message
                        IPoster poster = mPoster;
                        if (poster != null)
                            poster.sendMessage();

                        // rescheduled is true
                        rescheduled = true;
                        return;
                    }
                }
            } finally {
                isActive = rescheduled;
            }
        }

        /**
         * 在不需要的时候释放分发器
         */
        void dispose() {
            mPool.clear();
            mPoster = null;
        }

        /**
         * 得到一个 Runnable{@link #mPool}
         *
         * @return Runnable
         */
        private Runnable poll() {
            synchronized (mPool) {
                try {
                    return mPool.poll();
                } catch (NoSuchElementException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        /**
         * 发送一个刷新消息
         */
        interface IPoster {
            void sendMessage();
        }
    }
}
