假玩家管理器

## 语法

- `playerManager annotation <name> [<annotation>]`：为指定玩家数据添加或移除注释
- `playerManager autologin <name> <autoloing>`：控制假玩家是否在服务器启动时自动登录
- `playerManager (save|resave) <player> [<annotation>]`：保存或重新保存玩家数据
- `playerManager remove <name>`：删除一个玩家数据
- `playerManager spawn <name>`：生成一个假玩家
- `playerManager list <filter>`：列出所有保存的玩家
    - 如果指定了`filter`参数，则只列出名称或注释中包含指定字符串的玩家
- `playerManager schedule ...`
    - `... relogin <name> ...`
        - `... <interval>`：用来控制假玩家每隔指定时间重复的上线下线
        - `... stop`：停止上述操作
    - `... (login|logout) <name> <delayed> (h|min|s|t)`：用来控制指定假玩家在指定时间后上线/下线
    - `... cancel <name>`：取消指定玩家的上线/下线计划
    - `... list`：列出所有上述计划
- `playerManager safeafk ...`
    - `... set <player> [<threshold>] [<save>]`
        - 为指定假玩家设置安全挂机
        - 如果未指定`threshold`，默认为5
        - `save`参数用于控制是否永久更改
    - `... cancel <player> [<save>]`
        - 取消设置指定假玩家的安全挂机
        - `save`参数用于控制是否永久更改
    - `... list`：列出所有设置了安全挂机的假玩家
    - `... query <player>`：查询指定玩家的安全挂机阈值

## 示例

- 保存Steve的玩家数据
    - `/playerManager save Steve`
- 召唤Steve
    - `/playerManager spawn Steve`
- 列出所有已保存的假玩家
    - `/playerManager list`