package com.earthpol.combattag.util;

import com.earthpol.earthPolLib.config.ReloadableConfigNode;
import com.earthpol.earthPolLib.config.ReloadableConfiguration;

public enum ReloadableConfig implements ReloadableConfiguration {

    ALLOWED_COMMANDS(
            ReloadableConfigNode.of(
                    "Commands-allowed",
                    String.class,
                    "tc,nc,ally,g,general,msg,r,reply,tell,pm,mod,admin,combattag,lc,sw,siegewar,n,nation,t,town,townchat,nationchat,whisper",
                    "Commands that should be allowed while combat tagged")
    )
    ;

    private final ReloadableConfigNode<?> node;
    ReloadableConfig(ReloadableConfigNode<?> node) { this.node = node; }
    @Override public ReloadableConfigNode<?> node() { return node; }
}
