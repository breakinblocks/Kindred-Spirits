package com.breakinblocks.kindredspirits.integration.jade;

import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class KindredJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(CompanionStatusProvider.INSTANCE, CompanionEntity.class);
    }
}
