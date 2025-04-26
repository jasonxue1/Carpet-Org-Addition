路径点管理器，是MCDR中!!loc命令的替代命令，它通过强制使用UTF-8编码来避免乱码问题

## 语法

- `locations add <name> [<pos>]`：添加一个路径点
    - 如果未指定坐标，则取添加路径点时玩家的坐标
- `locations remove <name>`：删除指定名称的路径点
- `locations list [<filter>]`：列出所有路径点
    - 如果指定`filter`参数：则只列出名称中包含该字符串的路径点
- `locations set <name> [<pos>]`：为指定路径点修改坐标
- `locations supplement <name> another_pos [<anotherPos>]`：为指定主世界或下界路径点添加或移除另一个坐标
- `locations supplement <name> illustrate [<illustrate>]`：为指定路径点添加或移除说明文本

## 示例

- 列出所有路径点
    - `/locations list`
- 列出所有名称中包含“村民”的路径点
    - `/locations list "村民"`
- 添加一个名为“路径点”且指定坐标的路径点
    - `/locations add "路径点" 0 70 0`
- 将名称为`路径点`的路径点坐标修改为当前位置
    - `/locations set "路径点"`
- 删除一个名为“路径点”的路径点
    - `/locations remove "路径点"`