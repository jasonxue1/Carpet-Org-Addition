一些与本模组相关的功能

## 语法

- `orange permission <node> <level>`：设置一些子命令的使用权限
    - 需要2级命令权限
    - 支持以下命令
        - `/finder`命令的所有子命令，以及查询离线玩家物品的命令
        - `/navigate death`命令，此命令用来导航到指定玩家的上一次死亡位置
        - `/playerManager schedule relogin`命令，此命令用来设置假玩家周期性上下线
        - `/playerManager autologin`命令，此命令用来设置假玩家在服务器启动时自动登录
- `orange version`：获取`Carpet Org Additon`的版本
- `orange ruleself <player> <rule> [<value>]`：设置一些规则不对指定玩家生效
    - 玩家可以为自己，也可以为假玩家，但不能是其他真玩家
    - 如果未指定规则值，则表示查询规则是否对自己生效
    - 规则值永久更改，即服务器重启后仍然有效
    - 支持以下规则：
        - `方块掉落物直接进入物品栏`
- `orange textclickevent ...`：用于实现在聊天界面中单击按钮后执行对应的操作
    - 目前仅有一条`queryPlayerName`子命令，用于通过`Mojang API`根据玩家UUID查询玩家名称

## 示例

- 设置不允许非op玩家使用`/finder`命令查找方块
    - `/orange permission finder.block ops`
- 显示模组版本
    - `/orange version`