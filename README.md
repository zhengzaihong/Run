# Run
一个简单线程调度框架

implementation 'com.github.zhengzaihong:Run:xx.xx.xx' 如下版本号

[![](https://jitpack.io/v/zhengzaihong/Run.svg)](https://jitpack.io/#zhengzaihong/Run)


  //1.获取一个Handler
  
  Run.getUiHandler()
  
  //2.开启UI线程 同步， 子线程中也可更新ui
  
  Run.onUiSync( ...do something)
  
  //3.开启UI线程 异步， 子线程中也可更新ui
  
  Run.onUiAsync( ...do something )
