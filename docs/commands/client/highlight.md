高亮路径点

## 语法

- `/highlight <blockPos> [<second>|continue]`：设置高亮路径点
    - 第一次执行会高亮路径点，第二次执行会直接看向该路径点
    - `second`参数用来控制路径点高亮的描述，如果未指定，则默认高亮60秒
    - 如果指定时间为`continue`，则路径点会持续显示，直到玩家执行了`/highlight clear`
- `/highlight clear`：清除正在高亮的路径点

## 示例

- 高亮显示坐标X=0，Y=64，Z=0：
    - `/highlight 0 64 0`
- 清除高亮路径点
    - `/highlight clear`