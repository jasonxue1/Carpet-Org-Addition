将自己的经验转移给其他玩家，或者从假玩家身上拿取经验

## 语法

- `xpTransfer <from> <to> ...`
    - `... all`：转移所有经验
    - `... half`：转移一半的经验
    - `... level <level>`：转移从0级升级到指定等级所需的经验
    - `... points <number>`：转移指定数量的经验
    - `... upgrade <level>`：转移从当前等级**升级**指定等级所需的经验
    - `... upgradeto <level>`：转移从当前等级**升级到**指定等级所需的经验

## 示例

- 将Steve所有的经验转移给Alex
    - `/xpTransfer Steve Alex all`
- 将自己一半的经验转移给Steve
    - `/xpTransfer @s Steve half`