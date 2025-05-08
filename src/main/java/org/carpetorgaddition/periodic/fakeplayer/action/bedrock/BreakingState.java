package org.carpetorgaddition.periodic.fakeplayer.action.bedrock;

public enum BreakingState {
    /**
     * 放置朝上的活塞
     */
    PLACE_THE_PISTON_FACING_UP,
    /**
     * 在基岩方块侧面放置并激活一个拉杆
     */
    PLACE_AND_ACTIVATE_THE_LEVER,
    /**
     * 挖掘基岩上方的活塞，并在挖掘完成前关闭拉杆，然后完成挖掘，接着放置一个朝下的活塞
     */
    PISTON_BREAK_BEDROCK,
    /**
     * 清理掉基岩上方的活塞
     */
    CLEAN_PISTON,
    /**
     * 已完成破基岩
     */
    COMPLETE
}
