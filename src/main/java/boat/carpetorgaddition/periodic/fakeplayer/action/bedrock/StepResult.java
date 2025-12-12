package boat.carpetorgaddition.periodic.fakeplayer.action.bedrock;

/**
 * 当前步骤的执行结果
 */
public enum StepResult {
    /**
     * 当前步骤执行完毕，应继续执行下一步
     */
    CONTINUE,
    /**
     * 不再执行下一步，但是应继续执行下一个位置
     */
    COMPLETION,
    /**
     * 不再执行下一步，并且应该结束当前tick
     */
    TICK_COMPLETION,
    /**
     * 破基岩失败，重新开始
     */
    FAIL
}
