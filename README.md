# originIDE
  1. originIDE是原子灵动的英文名字
  2. 开源版本只有测试版和正式版，官方发行版不开源
  3. 申明：为了维护模组（也就是插件）开发者的权益，开源版本在模组方面仅包含模组的依赖库，类的加载逻辑与模组可作用于的类地址（发行版本中会进行修改），不包括安装，解析等
### 当前版本更新日志
    1.更新了语言包，当前已支持中文和英文，后续将会推出更多语言
    2.更新了模组加载逻辑，添加了javac编译模组
### app目录详解
#### libs目录
     1.javac1.8.jar -- 用于编译java，也是原子灵动默认装载的模组
     2.editor.jar  -- 编辑框的运用库，后续版本更新会进行修改（这里采用的是结绳开发者开源在gitee上的非原版文侠）
     3.rhino.jar  -- 用于执行build.gradjs的脚本运行环境
#### scr/main/java目录
     1.cn目录 -- 原子灵动本体
     2.com目录    -- 一些兼容库和基本库
     3.coyamo目录 -- xml转view的库，后续版本会移除
### oms目录详解
    oms就是oms库，oms库就是从这里分出去的
## 捐赠
### 一分也是爱！
