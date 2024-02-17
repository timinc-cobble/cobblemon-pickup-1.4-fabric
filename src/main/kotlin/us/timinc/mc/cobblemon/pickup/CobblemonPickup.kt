package us.timinc.mc.cobblemon.pickup

import com.cobblemon.mod.common.api.abilities.Ability
import com.cobblemon.mod.common.api.events.CobblemonEvents
import net.fabricmc.api.ModInitializer
import net.minecraft.loot.LootDataKey
import net.minecraft.loot.LootDataType
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import us.timinc.mc.cobblemon.pickup.config.Config
import kotlin.random.Random

class CobblemonPickup : ModInitializer {
    companion object {
        const val MOD_ID = "pickup"
        val PICKUP_LT = Identifier(MOD_ID, "gameplay/pickup")
    }
    private var config: Config = Config.Builder.load()

    override fun onInitialize() {
        CobblemonEvents.BATTLE_VICTORY.subscribe { evt ->
            val world = evt.winners.firstNotNullOf { winner -> winner.pokemonList.firstNotNullOf { battlePokemon -> battlePokemon.entity?.world } }
            if (world.server == null) return@subscribe
            for (winner in evt.winners) {
                val position = winner.pokemonList.firstNotNullOf { it.entity }.blockPos
                for (battlePokemon in winner.pokemonList) {
                    val pokemon = battlePokemon.effectedPokemon
                    if (!pokemon.heldItem().isEmpty) {
                        debug("${pokemon.getDisplayName().string} is already holding an item")
                        continue
                    }

                    val identifier = Identifier("pickup", "gameplay/${pokemon.ability.name}")
                    val lootManager = world.server!!.lootManager
                    val lootTable = lootManager.getLootTable(identifier)
                    val list = lootTable.generateLoot(LootContextParameterSet(
                        world as ServerWorld,
                        mapOf(
                            LootContextParameters.ORIGIN to position
                        ),
                        mapOf(),
                        0F
                    ))

                    if (list.isEmpty) {
                        debug("Attempted to roll for ability ${pokemon.ability.name} but nothing dropped.")
                        continue
                    }

                    pokemon.swapHeldItem(list.first())
                }
            }
        }
    }

    fun debug(msg: String) {
        if (!config.debug) return
        println(msg)
    }
}