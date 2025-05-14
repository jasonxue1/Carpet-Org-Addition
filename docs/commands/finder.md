查找玩家周围的指定方块，物品或交易

## 语法

- `finder block <blockState> [<range>|from]`：在指定半径或指定范围内搜索指定方块
- `finder item <itemStack> [<range>]`：在指定半径内搜索指定物品
- `finder item <itemStack> from ...`
    - `... <from> to <to>`：在指定范围内搜索指定物品
    - `... offline_player [inventory|ender_chest] [<showUnknown>]`：在离线玩家的物品栏或末影箱内搜索指定物品
        - 如果`showUnknown`参数为`true`，则显示未查询到玩家名称的玩家
- `finder trade item <itemStack> [<range>]`：在指定范围内搜索指定交易
- `finder trade enchanted_book <enchantment> [<range>]`：在指定范围内搜索指定附魔书交易

## 示例

- 查找周围30格以内的黑曜石
    - `/finder block minecraft:obsidian 30`
- 查找周围32格的容器内的烟花火箭
    - `/finder item minecraft:firework_rocket 32`
- 查找周围32格以内出售绿宝石的村民
    - `/finder trade item minecraft:emerald 32`
- 在离线玩家的物品栏中查找活塞
    - `/finder item minecraft:piston from offline_player`
- 在离线玩家的末影箱中查找任意食物
    - `/finder item *[minecraft:food] from offline_player ender_chest`