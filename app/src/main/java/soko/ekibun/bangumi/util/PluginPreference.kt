package soko.ekibun.bangumi.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import kotlinx.android.synthetic.main.pref_plugin_widget.view.*
import soko.ekibun.bangumi.R

class PluginPreference(context: Context?, private val plugin: Map.Entry<Context, Any>) : SwitchPreference(context) {

    init {
        val appInfo = plugin.key.applicationInfo
        key = "use_plugin_${plugin.key.packageName}"
        title = plugin.key.getString(appInfo.labelRes)
        icon = plugin.key.applicationInfo.loadIcon(plugin.key.packageManager)
        summary = plugin.key.packageName
        setDefaultValue(true)
        widgetLayoutResource = R.layout.pref_plugin_widget
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.item_settings.setOnClickListener {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + plugin.key.packageName)
                )
            )
        }
    }
}