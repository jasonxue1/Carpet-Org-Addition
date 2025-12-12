package boat.carpetorgaddition.dataupdate;

import boat.carpetorgaddition.rule.RuleConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

public class CarpetConfDataUpdater implements DataUpdater {
    /**
     * 所有规则，截至{@code 2025年6月19日}
     */
    public static final List<String> OLD_VERSION_RULES = List.of(
            "commandItemShadowing",
            "setBedrockHardness",
            "bindingCurseInvalidation",
            "disableOpenOrWaterDetection",
            "creativeImmuneKill",
            "flyingUseOnBlockFirework",
            "staringEndermanNotAngry",
            "farmlandPreventStepping",
            "maxBlockPlaceDistance",
            "simpleUpdateSkipper",
            "channelingIgnoreWeather",
            "notDamageEnderPearl",
            "disableDamageImmunity",
            "disableBatCanSpawn",
            "turtleEggFastHatch",
            "openShulkerBoxForcibly",
            "villagerInfiniteTrade",
            "fireworkRocketUseCooldown",
            "riptideIgnoreWeather",
            "pickaxeMinedBedrock",
            "villagerHeal",
            "fakePlayerHeal",
            "maxBlockPlaceDistanceReferToEntity",
            "knockbackStick",
            "disableRespawnBlocksExplode",
            "CCEUpdateSuppression",
            "openSeedPermissions",
            "openCarpetPermissions",
            "openGameRulePermissions",
            "openVillagerInventory",
            "peacefulCreeper",
            "commandXpTransfer",
            "commandSpectator",
            "commandFinder",
            "commandKillMe",
            "commandLocations",
            "healthNotFullCanEat",
            "canMineSpawner",
            "fakePlayerSpawnNoKnockback",
            "canActivatesObserver",
            "disableWaterFreezes",
            "fakePlayerCraftKeepItem",
            "commandParticleLine",
            "disableMobPeacefulDespawn",
            "climbingBoat",
            "reusableSmithingTemplate",
            "openTpPermissions",
            "softDeepslate",
            "softObsidian",
            "softOres",
            "betterTotemOfUndying",
            "commandPlayerAction",
            "fakePlayerCraftPickItemFromShulkerBox",
            "customPiglinBarteringTime",
            "quickSettingFakePlayerCraft",
            "fakePlayerKeepInventory",
            "commandCreeper",
            "commandRuleSearch",
            "superChargedCreeper",
            "playerDropHead",
            "beaconRangeExpand",
            "beaconWorldHeight",
            "canHighlightBlockPos",
            "commandPlayerManager",
            "blockDropsDirectlyEnterInventory",
            "turtleEggFastMine",
            "commandNavigate",
            "playerDropsNotDespawning",
            "fakePlayerMaxCraftCount",
            "fakePlayerSpawnMemoryLeakFix",
            "commandMail",
            "suppressionMismatchInDestroyBlockPosWarn",
            "syncNavigateWaypoint",
            "shulkerBoxStackable",
            "maxBlockPlaceDistanceSyncClient",
            "limitPhantomSpawn",
            "applyToolEffectsImmediately",
            "forceRestock",
            "autoSyncPlayerStatus",
            "recordPlayerCommand",
            "protectionEnchantmentCompatible",
            "damageEnchantmentCompatible",
            "finderCommandMaxFeedbackCount",
            "totemOfUndyingInvincibleTime",
            "playerCommandOpenPlayerInventory",
            "playerCommandTeleportFakePlayer",
            "experienceOrbMerge",
            "quickShulker",
            "disableCreativeContainerDrops"
    );

    @Override
    public JsonObject update(JsonObject json, int version) {
        if (version <= 1) {
            JsonObject newJson = new JsonObject();
            newJson.addProperty(DataUpdater.DATA_VERSION, 2);
            JsonObject newRules = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(RuleConfig.RULES).entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().getAsString();
                switch (key) {
                    case "playerCommandOpenPlayerInventory" -> {
                        if (!"false".equals(value)) {
                            newRules.addProperty("playerCommandOpenPlayerInventoryOption", value);
                            newRules.addProperty("playerCommandOpenPlayerInventory", "true");
                        }
                    }
                    case "betterTotemOfUndying" -> {
                        String newValue = switch (value) {
                            case "true" -> "inventory";
                            case "false" -> "vanilla";
                            case "shulker_box" -> "inventory_with_shulker_box";
                            default -> null;
                        };
                        if (newValue != null) {
                            newRules.addProperty(key, newValue);
                        }
                    }
                    default -> {
                        String newKey = switch (key) {
                            case "openSeedPermissions" -> "openSeedPermission";
                            case "openCarpetPermissions" -> "openCarpetPermission";
                            case "openGameRulePermissions" -> "openGameRulePermission";
                            case "openTpPermissions" -> "openTpPermission";
                            case "fakePlayerCraftKeepItem" -> "fakePlayerActionKeepItem";
                            case "fakePlayerCraftPickItemFromShulkerBox" -> "fakePlayerShulkerBoxItemHandling";
                            case "fakePlayerMaxCraftCount" -> "fakePlayerMaxItemOperationCount";
                            case "finderCommandMaxFeedbackCount" -> "maxLinesPerPage";
                            default -> key;
                        };
                        newRules.addProperty(newKey, value);
                    }
                }
            }
            newJson.add(RuleConfig.RULES, newRules);
            return newJson;
        }
        return json;
    }
}
