用来指引玩家前往某一个位置

## 语法

- `navigate blockPos <blockPos>`：指引玩家前往指定坐标
- `navigate (entity|player) <entity|player> [continue]`：指引玩家前往指定实体或玩家的位置
    - `player`是`entity`的替代品，`entity`可以选择包括玩家在内的任意实体，`player`只能选择玩家
    - 如果添加了`continue`参数，则在靠近实体后导航器不会自动结束
- `navigate death [<player>]`：指引玩家前往指定玩家的上一次死亡位置
- `navigate uuid <uuid>`：指引玩家前往指定UUID实体的位置
- `navigate waypoint <waypoint>`：指引玩家前往指定路径点
- `navigate spawnpoint`：指引玩家前往自己的出（重）生点
- `navigate stop`：停止导航器

## 示例

- 指引玩家前往Steve的位置
    - `/navigate player Steve`
- 指引玩家前往自己的上一次死亡位置
    - `/navigate death`
- 停止导航
    - `/navigate stop`
