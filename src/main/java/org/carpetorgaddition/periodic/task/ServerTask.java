package org.carpetorgaddition.periodic.task;

import net.minecraft.server.ServerTickManager;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.exception.TaskExecutionException;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class ServerTask implements Thread.UncaughtExceptionHandler {
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() + 1,
            Runtime.getRuntime().availableProcessors() + 1,
            5,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            this::ofPlatformThread
    );

    public ServerTask() {
        this.executor.allowCoreThreadTimeOut(true);
    }

    /**
     * 该任务是否应该被删除
     */
    private boolean remove = false;

    /**
     * 每个游戏刻都调用此方法
     */
    protected abstract void tick();

    /**
     * @return 当前任务是否已经执行完毕
     */
    protected abstract boolean stopped();

    /**
     * 执行任务
     *
     * @return 当前任务是否已经执行结束
     */
    public final boolean execute(ServerTickManager tickManager) {
        if (this.remove) {
            return true;
        }
        try {
            if (tickManager.shouldTick() || this.constantSpeed()) {
                this.tick();
                return this.stopped();
            } else {
                return false;
            }
        } catch (TaskExecutionException e) {
            e.disposal();
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error("{}任务执行时遇到意外错误", this.getLogName(), e);
        }
        return true;
    }

    /**
     * @return 当前任务的名称，不在游戏中使用，只在日志中使用
     */
    public String getLogName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    /**
     * @return 此任务是否不受/tick命令影响
     */
    public boolean constantSpeed() {
        return true;
    }

    /**
     * 将当前任务标记为已删除
     */
    public void markRemove() {
        this.remove = true;
    }

    protected void submit(Runnable task) {
        this.executor.submit(task);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        CarpetOrgAddition.LOGGER.warn("An unexpected error occurred: ", e);
    }

    /**
     * 为线程池创建线程
     */
    private Thread ofPlatformThread(Runnable runnable) {
        return Thread.ofPlatform()
                .daemon()
                .name(this.getClass().getSimpleName() + "-Thread")
                .uncaughtExceptionHandler(this)
                .unstarted(runnable);
    }
}
