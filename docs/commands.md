## 所有命令

### 下表列出了所有的服务端命令

命令权限由对应的规则控制

| 命令                                          | 效果                           | 默认权限  | 
|---------------------------------------------|------------------------------|-------|
| [/itemshadowing](commands/itemshadowing.md) | 用来制作物品分身                     | ops   |
| [/xpTransfer](commands/xpTransfer.md)       | 用来在玩家之间分享经验                  | ops   |
| [/spectator](commands/spectator.md)         | 在生存模式和旁观模式间切换                | ops   |
| [/finder](commands/finder.md)               | 用来查找玩家周围的指定方块或物品以及交易         | ops   |
| [/killMe](commands/killMe.md)               | 自杀                           | ops   |
| [/locations](commands/locations.md)         | 路径点管理器                       | ops   |
| [/playerAction](commands/playerAction.md)   | 假玩家动作命令，控制假玩家物品合成，自动交易等行为    | ops   |
| [/creeper](commands/creeper.md)             | 在指定玩家周围生成一只立即爆炸的苦力怕，爆炸不会破坏方块 | false |
| [/ruleSearch](commands/ruleSearch.md)       | 用来搜索翻译后名称中包含指定字符串的规则         | ops   |
| [/playerManager](commands/playerManager.md) | 用来管理一些常用假玩家的快速上线下线           | ops   |
| [/navigate](commands/navigate.md)           | 用来指引玩家前往某一个位置                | true  |
| [/mail](commands/mail.md)                   | 向另一名玩家发送物品                   | ops   |
| [/orange](commands/orange.md)               | 一些与本模组相关的功能                  | -     |

### 下表列出了所有的客户端命令

所有的客户端命令都是无权限的

| 命令                                           | 效果               |
|----------------------------------------------|------------------|
| [/dictionary](commands/client/dictionary.md) | 根据对象翻译后的名称查询对应ID |
| [/highlight](commands/client/highlight.md)   | 高亮路径点            |

## 对原版或其它模组命令的修改

### 服务端

- /player
    - `inventory`子命令，用来打开玩家的物品栏
        - 使用规则“打开玩家物品栏”控制使用权限
    - `enderChest`子命令，用来打开玩家的末影箱
        - 使用规则“打开玩家物品栏”控制使用权限
        - 规则值为`fake_player`时不允许打开自己的末影箱
    - `teleport`子命令，用来将假玩家传送到自己的位置
        - 使用规则“假玩家传送”控制使用权限

## 自定义命令名称

可以通过编辑配置文件来更改模组命令的名称，即给命令起别名

配置文件位于`<游戏根目录/config/carpetorgaddition/custom_command_name.json>`，格式如下：

- `根对象`
    - `data_version`：用于记录数据的版本号，玩家不应该修改它的值
    - `commands`：一个Json对象，保存所有可自定义名称的命令
        - `命令名称`：一条可自定义名称的命令，值可以为字符串，可以为字符串数组，如果为其他值会被忽略

### 示例

#### 将命令`/finder`修改为`/search`

- 原数据：
    ```json
       {
           "data_version": 1,
           "commands": {
               "finder": "finder"
           }
       }
    ```
- 修改后的数据
    ```json
       {
           "data_version": 1,
           "commands": {
               "finder": "search"
           }
       }
    ```
- 在游戏中，就可以通过`/search`而不是`/finder`来执行命令

#### 为`/spectator`命令指定多个名称

- 命令的自定义名称可以为多个，只需要将表示命令名称的字符串改为字符串数组，例如：
    - ```json
         {
             "data_version": 1,
             "commands": {
                 "spectator": [
                     "spectator",
                     "s",
                     "旁观"
                 ]
             }
         }
      ```
    - 可以使用这些名称中的任意一个名称来执行命令

自定义命令名称需要在游戏启动前修改配置文件，游戏启动后再修改不会有任何效果