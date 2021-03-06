package com.unciv.models.ruleset.tile

import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.Unique
import com.unciv.models.stats.NamedStats
import com.unciv.models.translations.tr
import java.util.*
import kotlin.math.roundToInt

class TileImprovement : NamedStats() {

    var terrainsCanBeBuiltOn: Collection<String> = ArrayList()

    // Used only for Camp - but avoid hardcoded comparison and *allow modding*
    // Terrain Features that need not be cleared if the improvement enables a resource
    @Deprecated("As of 3.14.15")
    var resourceTerrainAllow: Collection<String> = ArrayList()

    var techRequired: String? = null

    var uniqueTo:String? = null
    var uniques = ArrayList<String>()
    val uniqueObjects:List<Unique> by lazy { uniques.map { Unique(it) } }
    val shortcutKey: Char? = null

    val turnsToBuild: Int = 0 // This is the base cost.

    fun getTurnsToBuild(civInfo: CivilizationInfo): Int {
        var realTurnsToBuild = turnsToBuild.toFloat() * civInfo.gameInfo.gameParameters.gameSpeed.modifier
        // todo UNIFY THESE
        if (civInfo.hasUnique("Worker construction increased 25%"))
            realTurnsToBuild *= 0.75f
        if (civInfo.hasUnique("Tile improvement speed +25%"))
            realTurnsToBuild *= 0.75f
        return realTurnsToBuild.roundToInt()
    }

    fun getDescription(ruleset: Ruleset, forPickerScreen: Boolean = true): String {
        val stringBuilder = StringBuilder()
        val statsDesc = this.clone().toString()
        if (statsDesc.isNotEmpty()) stringBuilder.appendLine(statsDesc)
        if (uniqueTo!=null && !forPickerScreen) stringBuilder.appendLine("Unique to [$uniqueTo]".tr())
        if (!terrainsCanBeBuiltOn.isEmpty()) {
            val terrainsCanBeBuiltOnString: ArrayList<String> = arrayListOf()
            for (i in terrainsCanBeBuiltOn) {
                terrainsCanBeBuiltOnString.add(i.tr())
            }
            stringBuilder.appendLine("Can be built on ".tr() + terrainsCanBeBuiltOnString.joinToString(", "))//language can be changed when setting changes.
        }
        val statsToResourceNames = HashMap<String, ArrayList<String>>()
        for (tr: TileResource in ruleset.tileResources.values.filter { it.improvement == name }) {
            val statsString = tr.improvementStats.toString()
            if (!statsToResourceNames.containsKey(statsString))
                statsToResourceNames[statsString] = ArrayList()
            statsToResourceNames[statsString]!!.add(tr.name.tr())
        }
        statsToResourceNames.forEach {
            stringBuilder.appendLine(it.key + " for ".tr() + it.value.joinToString(", "))
        }

        if (techRequired != null) stringBuilder.appendLine("Required tech: [$techRequired]".tr())

        for(unique in uniques)
            stringBuilder.appendLine(unique.tr())

        return stringBuilder.toString()
    }

    fun hasUnique(unique: String) = uniques.contains(unique)
    fun isGreatImprovement() = hasUnique("Great Improvement")

    /**
     * Check: Is this improvement allowed on a [given][name] terrain feature?
     * 
     * Uses both _legacy_ [resourceTerrainAllow] and unique "Does not need removal of []"
     * 
     * Background: This not used for e.g. a lumbermill - it derives the right to be placed on forest
     * from [terrainsCanBeBuiltOn]. Other improvements may be candidates without fulfilling the
     * [terrainsCanBeBuiltOn] check - e.g. they are listed by a resource as 'their' improvement.
     * I such cases, the 'unbuildable' property of the Terrain feature might prevent the improvement,
     * so this check is done in conjunction - for the user, success means he does not need to remove
     * a terrain feature, thus the unique name.
     */
    fun isAllowedOnFeature(name: String): Boolean {
        if (name in resourceTerrainAllow) return true
        return uniqueObjects.filter { it.placeholderText == "Does not need removal of []"
                && it.params[0] == name
        }.any()
    }
}

