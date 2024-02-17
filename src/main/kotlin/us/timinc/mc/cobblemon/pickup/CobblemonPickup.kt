package us.timinc.mc.cobblemon.pickup

import com.cobblemon.mod.common.api.events.CobblemonEvents
import net.fabricmc.api.ModInitializer
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
                    if (pokemon.ability.name != "pickup") {
                        debug("${pokemon.getDisplayName().string} doesn't have pickup")
                        continue
                    }
                    if (!pokemon.heldItem().isEmpty) {
                        debug("${pokemon.getDisplayName().string} is already holding an item")
                        continue
                    }
                    val roll = Random.nextDouble()
                    if (roll > config.pickupChance) {
                        debug("${pokemon.getDisplayName().string} rolled $roll and needed to roll under ${config.pickupChance}")
                        continue
                    }

                    val lootTable = world.server!!.lootManager.getLootTable(PICKUP_LT)
                    val list = lootTable.generateLoot(LootContextParameterSet(
                        world as ServerWorld,
                        mapOf(
                            LootContextParameters.ORIGIN to position
                        ),
                        mapOf(),
                        0F
                    ))

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