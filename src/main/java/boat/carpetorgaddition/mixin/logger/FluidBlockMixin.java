package boat.carpetorgaddition.mixin.logger;

import boat.carpetorgaddition.logger.FunctionLogger;
import boat.carpetorgaddition.logger.LoggerNames;
import boat.carpetorgaddition.logger.LoggerRegister;
import boat.carpetorgaddition.logger.Loggers;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LiquidBlock.class)
public class FluidBlockMixin {
    @Unique
    private static final LocalizationKey KEY = LocalizationKeys.LOGGER.then(LoggerNames.OBSIDIAN);

    @WrapOperation(method = "shouldSpreadLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0))
    private boolean receiveNeighborFluids(Level world, BlockPos pos, BlockState state, Operation<Boolean> original) {
        if (LoggerRegister.obsidian && state.is(Blocks.OBSIDIAN)) {
            MinecraftServer server = world.getServer();
            if (server != null) {
                FunctionLogger logger = Loggers.getObsidianLogger();
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (logger.isSubscribed(player)) {
                        Component text = TextProvider.blockPos(pos, ServerUtils.getColor(world));
                        MessageUtils.sendMessage(player, KEY.then("generate").translate(text));
                    }
                }
            }
        }
        return original.call(world, pos, state);
    }
}
