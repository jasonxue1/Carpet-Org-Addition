允许打开假玩家的物品栏，末影箱，以及获取位置等

## 语法

- `playerTools <player> inventory`：打开一名玩家的物品栏
- `playerTools <player> enderChest`：打开一名玩家的末影箱
- `playerTools <player> heal`：治疗一名玩家
- `playerTools <player> teleport`：将一名玩家传送至自己的位置
- `playerTools <player> isFakePlayer`：判断一名玩家是否是假玩家
- `playerTools <player> position`：获取一名假玩家的位置

## 示例

- 回复Alex的生命值
    - `/playerTools Alex heal`
- 将Steve传送到自己的位置
    - `/playerTools Steve teleport`
- 判断Steve是否是假玩家
    - `/playerTools Steve isFakePlayer`