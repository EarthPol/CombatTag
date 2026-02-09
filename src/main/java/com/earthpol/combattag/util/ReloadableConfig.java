package com.earthpol.combattag.util;

import com.earthpol.earthPolLib.config.ReloadableConfigNode;
import com.earthpol.earthPolLib.config.ReloadableConfiguration;
import com.earthpol.earthPolLib.config.ReloadableListNode;

import java.util.List;


public enum ReloadableConfig implements ReloadableConfiguration {

    ALLOWED_COMMANDS(
            ReloadableListNode.ofList(
                    "Commands-allowed",
                    String.class,
                    List.of(
                    "tc", "nc", "ally", "g", "general", "msg", "r", "reply", "tell", "pm", "mod", "admin","combattag",
                            "lc","sw","siegewar","n","nation","t","town","townchat","nationchat","whisper"),
                    "Commands you can run while combat tagged"
    ))
    ;

    private final ReloadableConfigNode<?> node;
    ReloadableConfig(ReloadableConfigNode<?> node) { this.node = node; }
    @Override public ReloadableConfigNode<?> node() { return node; }
}
