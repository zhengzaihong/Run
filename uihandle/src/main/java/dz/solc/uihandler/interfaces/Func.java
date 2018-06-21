package dz.solc.uihandler.interfaces;

/**
 * creat_user: zhengzaihong
 * Email:1096877329@qq.com
 * creat_date: 2018/6/20
 * creat_time: 13:56
 * describe 异步或同步的回调并持有一个可接收返回值
 **/
public interface Func<Out> {
    Out call();
}
