package com.sharkmod.client;

import com.sharkmod.SharkMod;
import com.sharkmod.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SharkMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SharkModClient {
    private SharkModClient() {}

    /** Fator de tamanho do filhote em relação ao adulto. */
    public static final float BABY_SIZE = 0.375f;

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHARK.get(), ctx -> new SharkRenderer(ctx, 1.0f));
        event.registerEntityRenderer(ModEntities.BABY_SHARK.get(), ctx -> new SharkRenderer(ctx, BABY_SIZE));
    }
}
