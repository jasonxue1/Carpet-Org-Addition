控制假玩家自动化执行一些任务

## 语法

- `playerAction <player> ...`：具体效果见下文
    - `... empty [<filter>]`
    - `... craft ...`
        - `... inventory <item1> <item2> <item3> <item4>`
        - `... crafting_table <item1> <item2> <item3> <item4> <item5> <item6> <item7> <item8> <item9>`
        - `... four <item>`
        - `... gui`
        - `... nine <item>`
        - `... one <item>`
    - `... fill [<filter>] [<dropOther>]`
    - `... info`
    - `... rename <item> <name>`
    - `... sorting <item> <this> <other>`
    - `... stonecutting item <item> <button>`
    - `... stonecutting gui`
    - `... trade <index> [void_trade]`
    - `... fishing`
    - `... stop`

## 效果

- `/playerAction <player> ...`
    - `empty [<filter>]`
        - 让假玩家自动清空容器，需要让假玩家打开容器，清空完毕后自动关闭容器
        - 如果指定的`filter`参数，则只清空指定的物品
    - `craft ...`
        - `inventory <item1> <item2> <item3> <item4>`
            - 让假玩家在生存模式物品栏合成指定配方的物品
        - `crafting_table <item1> <item2> <item3> <item4> <item5> <item6> <item7> <item8> <item9>`
            - 让假玩家在工作台合成指定配方的物品
        - `four`
            - 让假玩家在生存模式物品栏合成指定配方为四个相同材料的物品
        - `gui`
            - 打开一个GUI用来为假玩家指定配方，然后根据配方决定在生存模式物品栏还是工作台合成
        - `nine`
            - 让假玩家在工作台合成指定配方为九个相同材料的物品
        - `one`
            - 让假玩家在生存模式物品栏合成配方为单个材料的物品
        - 如果在工作台合成，则需要打开一个工作台，并且不能使用副手槽和盔甲槽中的物品，在生存模式物品栏合成的，不需要打开工作台，并且副手槽和盔甲槽的物品也会被使用
    - `fill [<filter>] [<dropOther>]`
        - 让假玩家向容器中放入指定物品，玩家物品栏内的指定物品会被填入容器
        - 无法再向容器内填充物品时，容器会被自动关闭
        - 如果未指定物品，则表示向容器中填充任意物品
            - 潜影盒物品不会尝试放入潜影盒容器中
        - 如果`dropOther`参数未指定或为`true`，则填充容器时非指定物品会被丢出
        - 支持以下容器
            - 潜影盒
            - 通用9*3容器，例如箱子，木桶，陷阱箱
            - 通用9*6容器，例如大箱子
            - 通用3*3容器，例如发射器，投掷器
            - 漏斗
            - 合成器
    - `info`
        - 在聊天栏显示假玩家当前动作的详细信息
    - `rename <item> <name>`
        - 让假玩家给物品重命名，需要打开一个铁砧，重命名需要消耗经验
        - 为了避免经验浪费，物品必须满一组才会开始重命名
    - `sorting <item> <this> <other>`
        - 让假玩家从一堆物品实体中分拣出指定的物品并丢在`this`的位置，其他物品物品会丢在`other`位置
        - 潜影盒物品会先取出潜影盒内的物品，可以利用这个特性来快速拆解潜影盒
    - `stonecutting ...`
        - `... <item> <button>`
            - 为假玩家指定一个切石机配方
        - `... gui`
            - 使用GUI设置假玩家的切石机配方
    - `trade <index> [void_trade]`
        - 让假玩家与一名村民或流浪商人进行交易，需要打开一个交易界面
        - 如果指定了void_trade参数，表示当前进行的是虚空交易，虚空交易会在村民被卸载后的下一个游戏刻进行，交易完毕后会自动关闭交易页面
    - `fishing`
        - 让假玩家开始钓鱼
    - `stop`
        - 让假玩家停止当前的动作

## 示例

- 让Steve合成铁块
    - `/playerAction Steve craft nine minecraft:iron_ingot`
    - `/playerAction Steve craft crafting_table minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot`
- 让Bot使用切石机制作石砖（如果修改了切石机的配方，按钮的索引也要相应调整）
    - `/playerActions Bot stonecutting minecraft:stone 5`
- 显示Alex正在做什么
    - `/playerAction Alex info`
- 让Steve开始钓鱼
    - `/playerAction Steve fishing`