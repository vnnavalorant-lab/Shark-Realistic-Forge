package com.sharkmod.entity;

import com.sharkmod.SharkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SharkMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SharkMod.MOD_ID);

    /** Tubarão adulto ("dad"): tamanho normal. */
    public static final RegistryObject<EntityType<SharkEntity>> SHARK = ENTITY_TYPES.register("shark",
            () -> EntityType.Builder.of(SharkEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.6f, 1.2f)
                    .clientTrackingRange(4)
                    .build("shark"));

    /** Tubarão filhote ("baby"): o mesmo modelo/animações, só que menor. */
    public static final RegistryObject<EntityType<SharkEntity>> BABY_SHARK = ENTITY_TYPES.register("baby_shark",
            () -> EntityType.Builder.of(SharkEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.6f, 0.45f)
                    .clientTrackingRange(3)
                    .build("baby_shark"));

    public static final RegistryObject<Item> SHARK_SPAWN_EGG = ITEMS.register("shark_spawn_egg",
            () -> new ForgeSpawnEggItem(SHARK, 0x56616B, 0xE4E9ED, new Item.Properties()));

    public static final RegistryObject<Item> BABY_SHARK_SPAWN_EGG = ITEMS.register("baby_shark_spawn_egg",
            () -> new ForgeSpawnEggItem(BABY_SHARK, 0x7C8A96, 0xF1F4F6, new Item.Properties()));

    /**
     * Eventos do barramento do mod. O spawn natural (biomas) é feito por biome modifiers em
     * data/sharkmod/forge/biome_modifier/*.json.
     */
    @Mod.EventBusSubscriber(modid = SharkMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {}

        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(SHARK.get(), SharkEntity.createDadAttributes().build());
            event.put(BABY_SHARK.get(), SharkEntity.createBabyAttributes().build());
        }

        @SubscribeEvent
        public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
            event.register(SHARK.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.OCEAN_FLOOR_WG,
                    SharkEntity::canSpawnShark, SpawnPlacementRegisterEvent.Operation.OR);
            event.register(BABY_SHARK.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.OCEAN_FLOOR_WG,
                    SharkEntity::canSpawnShark, SpawnPlacementRegisterEvent.Operation.OR);
        }

        @SubscribeEvent
        public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(SHARK_SPAWN_EGG);
                event.accept(BABY_SHARK_SPAWN_EGG);
            }
        }
    }
}
