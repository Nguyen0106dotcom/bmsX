package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "thien_co_so_settings")

data class AlgorithmPreference(
    val id: String,
    val enabled: Boolean = true,
    val weightEnabled: Boolean = false,
    val weightValue: Double = 1.0,
    val params: Map<String, Double> = emptyMap()
)

data class StoredBundleRecord(
    val id: String,
    val name: String,
    val description: String,
    val rawJson: String,
    val isPlaceholderSample: Boolean = true
)

data class AppSettings(
    val selectedLotteryId: String = LotteryProfiles.XSMB.id,
    val darkTheme: Boolean = true,
    val algorithmPrefs: Map<String, AlgorithmPreference> = emptyMap(),
    val customBundles: List<StoredBundleRecord> = emptyList(),
    val syncUrls: Map<String, String> = emptyMap(),
    val disclaimerDismissed: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SELECTED_LOTTERY_ID = stringPreferencesKey("selected_lottery_id")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ALGORITHM_PREFS_JSON = stringPreferencesKey("algorithm_prefs_json")
        val CUSTOM_BUNDLES_JSON = stringPreferencesKey("custom_bundles_json")
        val SYNC_URLS_JSON = stringPreferencesKey("sync_urls_json")
        val DISCLAIMER_DISMISSED = booleanPreferencesKey("disclaimer_dismissed")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            selectedLotteryId = prefs[Keys.SELECTED_LOTTERY_ID] ?: LotteryProfiles.XSMB.id,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            algorithmPrefs = decodeAlgorithmPrefs(prefs[Keys.ALGORITHM_PREFS_JSON]),
            customBundles = decodeCustomBundles(prefs[Keys.CUSTOM_BUNDLES_JSON]),
            syncUrls = decodeSyncUrls(prefs[Keys.SYNC_URLS_JSON]),
            disclaimerDismissed = prefs[Keys.DISCLAIMER_DISMISSED] ?: false
        )
    }

    suspend fun setSelectedLottery(lotteryId: String) {
        context.dataStore.edit { it[Keys.SELECTED_LOTTERY_ID] = lotteryId }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun saveAlgorithmPreferences(map: Map<String, AlgorithmPreference>) {
        context.dataStore.edit { it[Keys.ALGORITHM_PREFS_JSON] = encodeAlgorithmPrefs(map) }
    }

    suspend fun saveCustomBundle(bundle: StoredBundleRecord) {
        context.dataStore.edit { prefs ->
            val current = decodeCustomBundles(prefs[Keys.CUSTOM_BUNDLES_JSON]).toMutableList()
            current.removeAll { it.id == bundle.id }
            current.add(0, bundle)
            prefs[Keys.CUSTOM_BUNDLES_JSON] = encodeCustomBundles(current)
        }
    }

    suspend fun deleteCustomBundle(bundleId: String) {
        context.dataStore.edit { prefs ->
            val current = decodeCustomBundles(prefs[Keys.CUSTOM_BUNDLES_JSON])
                .filterNot { it.id == bundleId }
            prefs[Keys.CUSTOM_BUNDLES_JSON] = encodeCustomBundles(current)
        }
    }

    suspend fun setSyncUrl(lotteryId: String, url: String) {
        context.dataStore.edit { prefs ->
            val current = decodeSyncUrls(prefs[Keys.SYNC_URLS_JSON]).toMutableMap()
            if (url.isBlank()) {
                current.remove(lotteryId)
            } else {
                current[lotteryId] = url.trim()
            }
            prefs[Keys.SYNC_URLS_JSON] = JSONObject(current as Map<*, *>).toString()
        }
    }

    suspend fun resetAllDefaults() {
        context.dataStore.edit { it.clear() }
    }

    private fun encodeAlgorithmPrefs(map: Map<String, AlgorithmPreference>): String {
        val root = JSONObject()
        for ((id, pref) in map) {
            val obj = JSONObject()
            obj.put("enabled", pref.enabled)
            obj.put("weightEnabled", pref.weightEnabled)
            obj.put("weightValue", pref.weightValue)
            val paramsObj = JSONObject()
            for ((pk, pv) in pref.params) {
                paramsObj.put(pk, pv)
            }
            obj.put("params", paramsObj)
            root.put(id, obj)
        }
        return root.toString()
    }

    private fun decodeAlgorithmPrefs(json: String?): Map<String, AlgorithmPreference> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val root = JSONObject(json)
            val out = LinkedHashMap<String, AlgorithmPreference>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val obj = root.optJSONObject(id) ?: continue
                val paramsObj = obj.optJSONObject("params")
                val params = LinkedHashMap<String, Double>()
                if (paramsObj != null) {
                    val pKeys = paramsObj.keys()
                    while (pKeys.hasNext()) {
                        val pk = pKeys.next()
                        params[pk] = paramsObj.optDouble(pk, 0.0)
                    }
                }
                out[id] = AlgorithmPreference(
                    id = id,
                    enabled = obj.optBoolean("enabled", true),
                    weightEnabled = obj.optBoolean("weightEnabled", false),
                    weightValue = obj.optDouble("weightValue", 1.0),
                    params = params
                )
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun encodeCustomBundles(list: List<StoredBundleRecord>): String {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("description", item.description)
            obj.put("rawJson", item.rawJson)
            obj.put("isPlaceholderSample", item.isPlaceholderSample)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeCustomBundles(json: String?): List<StoredBundleRecord> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val out = mutableListOf<StoredBundleRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                out.add(
                    StoredBundleRecord(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        description = obj.optString("description"),
                        rawJson = obj.optString("rawJson"),
                        isPlaceholderSample = obj.optBoolean("isPlaceholderSample", true)
                    )
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun decodeSyncUrls(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val out = LinkedHashMap<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = obj.optString(k)
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
