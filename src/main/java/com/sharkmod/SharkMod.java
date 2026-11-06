package com.sharkmod;

import com.sharkmod.entity.ModEntities;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SharkMod.MOD_ID)
public class SharkMod {
    public static final String MOD_ID = "sharkmod";

    public SharkMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITY_TYPES.register(modBus);
        ModEntities.ITEMS.register(modBus);
    }
}
