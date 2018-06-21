package dz.solc.uihandler.interfaces;

/**
 * creat_user: zhengzaihong
 * Email:1096877329@qq.com
 * creat_date: 2018/6/20
 * creat_time: 14:00
 * describe 事件分发
 **/
public interface Poster {

    /**
     * 异步和同步的标示
     */
    int ASYNC = 0x10101010;
    int SYNC = 0x20202020;


    /**
     * 添加一个异步事务到 Handler pool中
     *
     * @param runnable
     */
    void async(Task runnable);

    /**
     * 添加一个同步事务到 Handler pool中
     *
     * @param runnable
     */
    void sync(Task runnable);


    /**
     * 释放资源
     */
    void dispose();
}
