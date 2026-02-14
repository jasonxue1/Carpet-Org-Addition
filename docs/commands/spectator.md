用来在生存模式和旁观模式之间切换

## 语法

- `spectator [<player>]`：在生存模式和旁观模式直接切换
    - 从旁观模式切换会生存模式后会自动回到原位置
    - 如果指定了`player`参数，则切换指定假玩家的游戏模式
- `spectator teleport dimension <dimension> [<location>]`：将自己传送至指定维度
    - 自己必须处于旁观模式
    - 如果指定了`location`，则表示传送至指定维度的指定位置
    - 如果在主世界和下界直接传送并且未指定位置，则会自动换算坐标
- `spectator teleport entity <entity>`：将自己传送至指定实体位置
    - 自己必须处于旁观模式
- `spectator teleport location <location>`：将自己传送至指定位置
    - 自己必须处于旁观模式

## 示例

- 将自己的游戏模式在生存模式和旁观模式之间切换
    - `/spectator`
- 将处于旁观模式的自己传送至下界对应位置
    - `/spectator teleport dimension minecraft:the_nether`
- 将Bot在生存模式和旁观模式之间切换
    - `/spectator Bot`