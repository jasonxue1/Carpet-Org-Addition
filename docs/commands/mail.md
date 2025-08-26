用于向其他玩家发送物品

## 语法

- `mail ship <player>`：向一名玩家发送手上的物品
- `mail multiple <player>`：向一名玩家发送多个物品
    - 执行命令后会打开一个GUI，将物品放入GUI再关闭屏幕即可发送
- `mail receive [<id>]`：接收指定单号的快递
    - 如果未指定单号，则接收所有快递
- `mail cancel [<id>]`：撤回指定单号的快递
    - 如果未指定单号，则撤回所有快递
- `mail intercept <id>`：拦截指定单号的快递，必须指定单号
    - 需要`2`级命令权限
- `mail list`：列出所有快递

## 示例

- 将手上的物品发送给Alex
    - `/mail ship Alex`
- 接收单号为3的快递
    - `/mail receive 3`
- 列出所有快递
    - `/mail list`