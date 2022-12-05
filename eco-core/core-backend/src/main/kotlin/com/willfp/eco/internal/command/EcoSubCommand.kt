package com.willfp.eco.internal.command

import com.willfp.eco.core.EcoPlugin

class EcoSubCommand(
    plugin: EcoPlugin, name: String,
    permission: String,
    playersOnly: Boolean
) : EcoHandledCommand(plugin, name, permission, playersOnly) {

}