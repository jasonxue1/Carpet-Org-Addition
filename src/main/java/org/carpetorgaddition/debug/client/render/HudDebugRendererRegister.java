package org.carpetorgaddition.debug.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ComparatorBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;
import org.carpetorgaddition.client.renderer.Tooltip;
import org.carpetorgaddition.client.util.ClientUtils;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.exception.ProductionEnvironmentError;
import org.carpetorgaddition.mixin.accessor.ExperienceOrbEntityAccessor;
import org.carpetorgaddition.mixin.accessor.HandledScreenAccessor;
import org.carpetorgaddition.mixin.accessor.ScreenAccessor;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.Counter;
import org.carpetorgaddition.wheel.TextBuilder;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;

public class HudDebugRendererRegister {
    private static final HashSet<HudDebugRenderer> renders = new HashSet<>();

    static {
        // 断言开发环境
        ProductionEnvironmentError.assertDevelopmentEnvironment();
    }

    static {
        // 显示方块挖掘速度
        renders.add((context, tickCounter) -> {
            if (DebugSettings.showBlockBreakingSpeed.get()) {
                HitResult hitResult = ClientUtils.getCrosshairTarget();
                if (hitResult == null) {
                    return;
                }
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    ClientPlayerEntity player = ClientUtils.getPlayer();
                    ServerWorld world = getServer().getWorld(FetcherUtils.getWorld(player).getRegistryKey());
                    if (world == null) {
                        return;
                    }
                    BlockState blockState = world.getBlockState(blockPos);
                    if (blockState.isAir()) {
                        return;
                    }
                    float speed = player.getBlockBreakingSpeed(blockState);
                    String formatted = "%.2f".formatted(speed);
                    if (formatted.endsWith(".00")) {
                        formatted = formatted.substring(0, formatted.length() - 3);
                    } else if (formatted.endsWith("0")) {
                        formatted = formatted.substring(0, formatted.length() - 1);
                    }
                    Tooltip.drawTooltip(context, TextBuilder.create("挖掘速度：" + formatted));
                }
            }
        });
        // 渲染比较器强度
        renders.add((context, tickCounter) -> {
            if (DebugSettings.showComparatorLevel.get()) {
                HitResult hitResult = ClientUtils.getCrosshairTarget();
                if (hitResult == null) {
                    return;
                }
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    ClientPlayerEntity player = ClientUtils.getPlayer();
                    ServerWorld world = getServer().getWorld(FetcherUtils.getWorld(player).getRegistryKey());
                    if (world == null) {
                        return;
                    }
                    BlockEntity blockEntity = world.getWorldChunk(blockPos).getBlockEntity(blockPos, WorldChunk.CreationType.IMMEDIATE);
                    if (blockEntity instanceof ComparatorBlockEntity comparator) {
                        int level = comparator.getOutputSignal();
                        if (level == 0) {
                            return;
                        }
                        Tooltip.drawTooltip(context, TextBuilder.create("红石信号等级：" + level));
                    }
                }
            }
        });
        // 渲染灵魂沙物品数量
        renders.add((context, tickCounter) -> {
            if (showSoulSandItemCount()) {
                HitResult hitResult = ClientUtils.getCrosshairTarget();
                if (hitResult == null) {
                    return;
                }
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    ServerWorld world = getPlayerWorld();
                    BlockState blockState = world.getBlockState(blockPos);
                    if (blockState.isAir()) {
                        return;
                    }
                    if (blockState.isOf(Blocks.SOUL_SAND)) {
                        Box box = new Box(blockPos.up());
                        List<Entity> itemEntities;
                        try {
                            // 方法可能被多个线程调用
                            itemEntities = world.getEntitiesByClass(Entity.class, box, EntityPredicates.VALID_ENTITY);
                        } catch (ConcurrentModificationException e) {
                            return;
                        }
                        if (itemEntities.isEmpty()) {
                            return;
                        }
                        Counter<Item> counter = new Counter<>();
                        int experienceOrbEntityCount = 0;
                        int experienceOrbTotalValue = 0;
                        for (Entity entity : itemEntities) {
                            switch (entity) {
                                case ItemEntity itemEntity -> {
                                    ItemStack itemStack = itemEntity.getStack();
                                    counter.add(itemStack.getItem(), itemStack.getCount());
                                }
                                case ExperienceOrbEntity experienceOrb -> {
                                    experienceOrbEntityCount++;
                                    ExperienceOrbEntityAccessor accessor = (ExperienceOrbEntityAccessor) experienceOrb;
                                    experienceOrbTotalValue += accessor.getPickingCount() * experienceOrb.getValue();
                                }
                                default -> {
                                }
                            }
                        }
                        List<Text> list = new ArrayList<>();
                        for (Item item : counter) {
                            int count = counter.getCount(item);
                            list.add(TextBuilder.combineAll(item.getName(), "*", count));
                        }
                        if (experienceOrbEntityCount != 0) {
                            list.add(TextBuilder.combineAll("经验球实体数量：", experienceOrbEntityCount));
                            list.add(TextBuilder.combineAll("经验球总价值：", experienceOrbTotalValue));
                        }
                        Tooltip.drawTooltip(context, list);
                    }
                }
            }
        });
        // 渲染当前HUD信息
        renders.add((context, tickCounter) -> {
            if (DebugSettings.HUDInformationDisplay.get()) {
                MinecraftClient client = ClientUtils.getClient();
                Screen screen = ClientUtils.getCurrentScreen();
                if (screen == null) {
                    return;
                }
                ArrayList<String> list = new ArrayList<>();
                String name = screen.getClass().getSimpleName();
                list.add("类名：" + name);
                Mouse mouse = ClientUtils.getMouse();
                double mouseX = mouse.getX();
                double mouseY = mouse.getY();
                list.add("鼠标X：" + (int) mouseX);
                list.add("鼠标Y：" + (int) mouseY);
                if (screen instanceof HandledScreen<?> handledScreen) {
                    try {
                        ScreenHandlerType<?> type = handledScreen.getScreenHandler().getType();
                        Identifier id = Registries.SCREEN_HANDLER.getId(type);
                        if (id != null) {
                            list.add("屏幕类型id：" + id);
                        }
                    } catch (UnsupportedOperationException e) {
                        list.add("屏幕类型id：null");
                    }
                    HandledScreenAccessor accessor = (HandledScreenAccessor) handledScreen;
                    Window window = client.getWindow();
                    Slot slot = accessor.invokerGetSlotAt(
                            mouseX * (double) window.getScaledWidth() / (double) window.getWidth(),
                            mouseY * (double) window.getScaledHeight() / (double) window.getHeight()
                    );
                    if (slot != null) {
                        list.add("槽位索引：" + slot.id);
                        list.add("槽位物品栏：" + slot.inventory.getClass().getSimpleName());
                    }
                }
                context.drawTooltip(ClientUtils.getTextRenderer(), list.stream().map(Text::of).toList(), 3, 25);
            }
        });
    }

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renders.forEach(renderer -> renderer.render(drawContext, tickCounter)));
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (DebugSettings.HUDInformationDisplay.get() && screen instanceof HandledScreen<?> handledScreen) {
                DefaultedList<Slot> slots = handledScreen.getScreenHandler().slots;
                slots.forEach(slot -> ((ScreenAccessor) screen).putDrawable((context, mouseX, mouseY, delta) -> {
                    HandledScreenAccessor accessor = (HandledScreenAccessor) handledScreen;
                    int x = accessor.getX();
                    int y = accessor.getY();
                    context.drawText(
                            Screens.getTextRenderer(screen),
                            String.valueOf(slot.id),
                            slot.x + x + 1,
                            slot.y + y,
                            0xFFFE9900,
                            true
                    );
                }));
            }
        });
    }

    // 是否应该显示灵魂沙物品计数
    private static boolean showSoulSandItemCount() {
        if (DebugSettings.showSoulSandItemCount.get()) {
            if (ClientUtils.getServer() == null) {
                return false;
            }
            if (ClientUtils.getCurrentScreen() == null) {
                return true;
            }
            return ClientUtils.getCurrentScreen() instanceof ChatScreen;
        }
        return false;
    }

    @Contract("-> !null")
    private static ClientWorld getClientWorld() {
        return ClientUtils.getWorld();
    }

    @Contract("-> !null")
    private static ServerWorld getPlayerWorld() {
        return getServer().getWorld(getClientWorld().getRegistryKey());
    }

    @Contract("-> !null")
    private static IntegratedServer getServer() {
        return ClientUtils.getServer();
    }
}
