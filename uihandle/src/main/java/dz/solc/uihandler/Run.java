package dz.solc.uihandler;


import android.os.Handler;
import android.os.Looper;

import dz.solc.uihandler.interfaces.Action;
import dz.solc.uihandler.interfaces.Func;
import dz.solc.uihandler.interfaces.Result;

/**
 * creat_user: zhengzaihong
 * Email:1096877329@qq.com
 * creat_date: 2018/6/20
 * creat_time: 14:16
 * describe 这是一个Ui 操作类,你可以运行一个同步或异步的线程在主线程
 **/

@SuppressWarnings("WeakerAccess")
final public class Run {
    private static HandlerPoster uiPoster = null;
    private static HandlerPoster backgroundPoster = null;

    /**
     * 获取一个Ui Handler
     *
     * @return Handler
     */
    public static Handler getUiHandler() {
        return getUiPoster();
    }

    private static HandlerPoster getUiPoster() {
        if (uiPoster == null) {
            synchronized (Run.class) {
                if (uiPoster == null) {
                    uiPoster = new HandlerPoster(Looper.getMainLooper(), 16, false);
                }
            }
        }
        return uiPoster;
    }

    /**
     * 获取一个后台线程的 Handler
     *
     * @return Handler
     */
    public static Handler getBackgroundHandler() {
        return getBackgroundPoster();
    }

    private static HandlerPoster getBackgroundPoster() {
        if (backgroundPoster == null) {
            synchronized (Run.class) {
                if (backgroundPoster == null) {
                    Thread thread = new Thread("ThreadRunHandler") {
                        @Override
                        public void run() {
                            Looper.prepare();
                            synchronized (Run.class) {
                                backgroundPoster = new HandlerPoster(Looper.myLooper(), 3 * 1000, true);
                                try {
                                    Run.class.notifyAll();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            Looper.loop();
                        }
                    };

                    thread.setDaemon(true);
                    thread.setPriority(Thread.MAX_PRIORITY);
                    thread.start();

                    try {
                        Run.class.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return backgroundPoster;
    }

    /**
     * 这个方法相当于开辟了一个子线程,并非在当前线程运行
     *
     * @param action 你可以在Action的回调中做相应的事,并且返回Result 执行状态,可取消该操作
     */
    public static Result onBackground(Action action) {
        final HandlerPoster poster = getBackgroundPoster();
        if (Looper.myLooper() == poster.getLooper()) {
            action.call();
            return new ActionAsyncTask(action, true);
        }
        ActionAsyncTask task = new ActionAsyncTask(action);
        poster.async(task);
        return task;
    }


    /**
     * 在Ui线程开启一个异步事务
     *
     * @param action Action Interface
     */
    public static Result onUiAsync(Action action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.call();
            return new ActionAsyncTask(action, true);
        }
        ActionAsyncTask task = new ActionAsyncTask(action);
        getUiPoster().async(task);
        return task;
    }

    /**
     * 在Ui线程开启一个同步事务
     *
     * @param action Action Interface
     */
    public static void onUiSync(Action action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.call();
            return;
        }
        ActionSyncTask poster = new ActionSyncTask(action);
        getUiPoster().sync(poster);
        poster.waitRun();
    }

    /**
     * 在Ui线程开启一个同步事务
     *
     * @param action          Action Interface
     * @param waitMillis      等待多少毫秒开始执行
     * @param cancelOnTimeOut 超过该时间取消操作
     */
    public static void onUiSync(Action action, long waitMillis, boolean cancelOnTimeOut) {
        onUiSync(action, waitMillis, 0, cancelOnTimeOut);
    }

    /**
     * @param action          Action Interface
     * @param waitMillis      等待多少毫秒开始执行
     * @param waitNanos       等待主线waitNanos 纳秒
     * @param cancelOnTimeOut 超过该时间取消操作
     */
    public static void onUiSync(Action action, long waitMillis, int waitNanos, boolean cancelOnTimeOut) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.call();
            return;
        }
        ActionSyncTask poster = new ActionSyncTask(action);
        getUiPoster().sync(poster);
        poster.waitRun(waitMillis, waitNanos, cancelOnTimeOut);
    }


    /**
     * 在Ui线程开启一个同步事务
     *
     * @param func Func Interface
     * @param <T>  你可以设置一个返回类型
     * @return {@link T}
     */
    public static <T> T onUiSync(Func<T> func) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return func.call();
        }

        FuncSyncTask<T> poster = new FuncSyncTask<T>(func);
        getUiPoster().sync(poster);
        return poster.waitRun();
    }

    /**
     * 在Ui线程开启一个同步事务
     *
     * @param func            Func Interface
     * @param waitMillis      等待多少毫秒开始执行
     * @param cancelOnTimeOut 超过该时间取消操作
     * @param <T>             你可以设置一个返回类型
     * @return {@link T}
     */
    public static <T> T onUiSync(Func<T> func, long waitMillis, boolean cancelOnTimeOut) {
        return onUiSync(func, waitMillis, 0, cancelOnTimeOut);
    }


    /**
     * 在Ui线程开启一个同步事务
     *
     * @param func            Func Interface
     * @param waitMillis      等待多少毫秒开始执行
     * @param cancelOnTimeOut 超过该时间取消操作
     * @param waitNanos       等待主线waitNanos 纳秒
     * @param <T>             你可以设置一个返回类型
     * @return {@link T}
     */
    public static <T> T onUiSync(Func<T> func, long waitMillis, int waitNanos, boolean cancelOnTimeOut) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return func.call();
        }

        FuncSyncTask<T> poster = new FuncSyncTask<T>(func);
        getUiPoster().sync(poster);
        return poster.waitRun(waitMillis, waitNanos, cancelOnTimeOut);
    }


    /**
     * 释放资源
     */
    public static void dispose() {
        if (uiPoster != null) {
            uiPoster.dispose();
            uiPoster = null;
        }
    }


    /**
     *
     *  该框架知识点
     java队列——queue详细分析

     Queue： 基本上，一个队列就是一个先入先出（FIFO）的数据结构

     Queue接口与List、Set同一级别，都是继承了Collection接口。LinkedList实现了Deque接 口。

     Queue的实现

     1、没有实现的阻塞接口的LinkedList： 实现了java.util.Queue接口和java.util.AbstractQueue接口
     　　内置的不阻塞队列： PriorityQueue 和 ConcurrentLinkedQueue
     　　PriorityQueue 和 ConcurrentLinkedQueue 类在 Collection Framework 中加入两个具体集合实现。 
     　　PriorityQueue 类实质上维护了一个有序列表。加入到 Queue 中的元素根据它们的天然排序（通过其 java.util.Comparable 实现）或者根据传递给构造函数的 java.util.Comparator 实现来定位。
     　　ConcurrentLinkedQueue 是基于链接节点的、线程安全的队列。并发访问不需要同步。因为它在队列的尾部添加元素并从头部删除它们，所以只要不需要知道队列的大 小，　　　　    　　ConcurrentLinkedQueue 对公共集合的共享访问就可以工作得很好。收集关于队列大小的信息会很慢，需要遍历队列。


     2)实现阻塞接口的：
     　　java.util.concurrent 中加入了 BlockingQueue 接口和五个阻塞队列类。它实质上就是一种带有一点扭曲的 FIFO 数据结构。不是立即从队列中添加或者删除元素，线程执行操作阻塞，直到有空间或者元素可用。
     五个队列所提供的各有不同：
     　　* ArrayBlockingQueue ：一个由数组支持的有界队列。
     　　* LinkedBlockingQueue ：一个由链接节点支持的可选有界队列。
     　　* PriorityBlockingQueue ：一个由优先级堆支持的无界优先级队列。
     　　* DelayQueue ：一个由优先级堆支持的、基于时间的调度队列。
     　　* SynchronousQueue ：一个利用 BlockingQueue 接口的简单聚集（rendezvous）机制。


     下表显示了jdk1.5中的阻塞队列的操作：

     　　add        增加一个元索                     如果队列已满，则抛出一个IIIegaISlabEepeplian异常
     　　remove   移除并返回队列头部的元素    如果队列为空，则抛出一个NoSuchElementException异常
     　　element  返回队列头部的元素             如果队列为空，则抛出一个NoSuchElementException异常
     　　offer       添加一个元素并返回true       如果队列已满，则返回false
     　　poll         移除并返问队列头部的元素    如果队列为空，则返回null
     　　peek       返回队列头部的元素             如果队列为空，则返回null
     　　put         添加一个元素                      如果队列满，则阻塞
     　　take        移除并返回队列头部的元素     如果队列为空，则阻塞


     remove、element、offer 、poll、peek 其实是属于Queue接口。 

     阻塞队列的操作可以根据它们的响应方式分为以下三类：aad、removee和element操作在你试图为一个已满的队列增加元素或从空队列取得元素时 抛出异常。当然，在多线程程序中，队列在任何时间都可能变成满的或空的，所以你可能想使用offer、poll、peek方法。这些方法在无法完成任务时 只是给出一个出错示而不会抛出异常。

     注意：poll和peek方法出错进返回null。因此，向队列中插入null值是不合法的

     最后，我们有阻塞操作put和take。put方法在队列满时阻塞，take方法在队列空时阻塞。


     LinkedBlockingQueue的容量是没有上限的（说的不准确，在不指定时容量为Integer.MAX_VALUE，不要然的话在put时怎么会受阻呢），但是也可以选择指定其最大容量，它是基于链表的队列，此队列按 FIFO（先进先出）排序元素。


     ArrayBlockingQueue在构造时需要指定容量， 并可以选择是否需要公平性，如果公平参数被设置true，等待时间最长的线程会优先得到处理（其实就是通过将ReentrantLock设置为true来 达到这种公平性的：即等待时间最长的线程会先操作）。通常，公平性会使你在性能上付出代价，只有在的确非常需要的时候再使用它。它是基于数组的阻塞循环队 列，此队列按 FIFO（先进先出）原则对元素进行排序。


     PriorityBlockingQueue是一个带优先级的 队列，而不是先进先出队列。元素按优先级顺序被移除，该队列也没有上限（看了一下源码，PriorityBlockingQueue是对 PriorityQueue的再次包装，是基于堆数据结构的，而PriorityQueue是没有容量限制的，与ArrayList一样，所以在优先阻塞 队列上put时是不会受阻的。虽然此队列逻辑上是无界的，但是由于资源被耗尽，所以试图执行添加操作可能会导致 OutOfMemoryError），但是如果队列为空，那么取元素的操作take就会阻塞，所以它的检索操作take是受阻的。另外，往入该队列中的元 素要具有比较能力。


     DelayQueue（基于PriorityQueue来实现的）是一个存放Delayed 元素的无界阻塞队列，只有在延迟期满时才能从中提取元素。该队列的头部是延迟期满后保存时间最长的 Delayed 元素。如果延迟都还没有期满，则队列没有头部，并且poll将返回null。当一个元素的 getDelay(TimeUnit.NANOSECONDS) 方法返回一个小于或等于零的值时，则出现期满，poll就以移除这个元素了。此队列不允许使用 null 元素。
     */


}
