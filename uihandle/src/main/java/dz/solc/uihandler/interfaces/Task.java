package dz.solc.uihandler.interfaces;

import java.util.Queue;

/**
 * creat_user: zhengzaihong
 * Email:1096877329@qq.com
 * creat_date: 2018/6/20
 * creat_time: 14:11
 * describe 事务接口
 **/
public interface Task extends Runnable, Result {
    /**
     * 添加一个事务到队列里去
     * @param pool
     */
    void setPool(Queue<Task> pool);
}