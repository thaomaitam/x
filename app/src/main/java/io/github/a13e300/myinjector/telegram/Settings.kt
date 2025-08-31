@file:Suppress("DEPRECATION")

package io.github.a13e300.myinjector.telegram

import android.app.AlertDialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.preference.ListPreference
import android.preference.MultiSelectListPreference
import android.preference.Preference
import android.preference.PreferenceGroup
import android.preference.PreferenceScreen
import android.preference.SwitchPreference
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListAdapter
import android.widget.ListView
import io.github.a13e300.myinjector.Entry
import io.github.a13e300.myinjector.R
import io.github.a13e300.myinjector.arch.IHook
import io.github.a13e300.myinjector.arch.addModuleAssets
import io.github.a13e300.myinjector.arch.category
import io.github.a13e300.myinjector.arch.forceSetSelection
import io.github.a13e300.myinjector.arch.getObjAs
import io.github.a13e300.myinjector.arch.hookAllAfter
import io.github.a13e300.myinjector.arch.inflateLayout
import io.github.a13e300.myinjector.arch.newInst
import io.github.a13e300.myinjector.arch.newInstAs
import io.github.a13e300.myinjector.arch.preference
import io.github.a13e300.myinjector.arch.restartApplication
import io.github.a13e300.myinjector.arch.setObj
import io.github.a13e300.myinjector.arch.sp2px
import io.github.a13e300.myinjector.arch.switchPreference

class SettingDialog(context: Context) : AlertDialog.Builder(context),
    Preference.OnPreferenceChangeListener,
    Preference.OnPreferenceClickListener {

    private lateinit var listView: ListView
    private lateinit var adapter: BaseAdapter
    private lateinit var prefScreen: PreferenceScreen
    private var searchItems = listOf<SearchItem>()
    private var ListAdapter.preferenceList: List<Preference>
        get() = getObjAs("mPreferenceList")
        set(value) {
            setObj("mPreferenceList", value)
        }


    @Deprecated("Deprecated in Java")
    override fun onPreferenceChange(
        preference: Preference,
        newValue: Any?
    ): Boolean {
        val settings = TelegramHandler.settings.toBuilder()
        val v = newValue as Boolean
        when (preference.key) {
            "enabled" -> settings.disabled = !v
            "autoCheckDeleteMessageOption" ->
                settings.autoCheckDeleteMessageOption = v

            "autoUncheckSharePhoneNumber" ->
                settings.autoUncheckSharePhoneNumber = v

            "avatarPageScrollToCurrent" ->
                settings.avatarPageScrollToCurrent = v

            "contactPermission" ->
                settings.contactPermission = v

            "customEmojiMapping" ->
                settings.customEmojiMapping = v

            "customMapPosition" ->
                settings.customMapPosition = v

            "defaultSearchTab" ->
                settings.defaultSearchTab = v

            "disableVoiceOrCameraButton" ->
                settings.disableVoiceOrCameraButton = v

            "emojiStickerMenu" ->
                settings.emojiStickerMenu = v

            "fakeInstallPermission" ->
                settings.fakeInstallPermission = v

            "fixHasAppToOpen" ->
                settings.fixHasAppToOpen = v

            "longClickMention" ->
                settings.longClickMention = v

            "mutualContact" ->
                settings.mutualContact = v

            "noGoogleMaps" ->
                settings.noGoogleMaps = v

            "openLinkDialog" ->
                settings.openLinkDialog = v

            "sendImageWithHighQualityByDefault" ->
                settings.sendImageWithHighQualityByDefault = v

            "hidePhoneNumber" -> {
                settings.hidePhoneNumber = v
                prefScreen.findPreference("hidePhoneNumberForSelfOnly").isEnabled = v
            }

            "hidePhoneNumberForSelfOnly" ->
                settings.hidePhoneNumberForSelfOnly = v

            "alwaysShowStorySaveIcon" ->
                settings.alwaysShowStorySaveIcon = v

            "removeArchiveFolder" ->
                settings.removeArchiveFolder = v

            "alwaysShowDownloadManager" ->
                settings.alwaysShowDownloadManager = v

            "hideFloatFab" ->
                settings.hideFloatFab = v

            "openTgUserLink" ->
                settings.openTgUserLink = v

            "copyPrivateChatLink" ->
                settings.copyPrivateChatLink = v
        }
        TelegramHandler.updateSettings(settings.build())
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onPreferenceClick(preference: Preference): Boolean {
        if (preference.key == "customEmojiMappingConfig") {
            CustomEmojiMapping.importEmojiMap(context)
            return true
        }
        return false
    }

    fun search(text: String) {
        val preferences = if (text.isEmpty()) {
            searchItems.map { it.restore(); it.preference }
        } else {
            searchItems.sortedByDescending { it.calcScoreAndApplyHintBy(text) }
                .filterNot { it.cacheScore == 0 }.map { it.preference }
        }
        adapter.preferenceList = preferences
        adapter.notifyDataSetChanged()
        listView.forceSetSelection(0)
    }

    private fun retrieve(group: PreferenceGroup): List<SearchItem> = buildList {
        for (i in 0 until group.preferenceCount) {
            val preference = group.getPreference(i)
            val entries = when (preference) {
                is ListPreference -> preference.entries
                is MultiSelectListPreference -> preference.entries
                else -> arrayOf()
            }.orEmpty()
            if (preference !is PreferenceGroup) {
                preference.isPersistent = false
                preference.onPreferenceChangeListener = this@SettingDialog
                preference.onPreferenceClickListener = this@SettingDialog
                when (preference.key) {
                    "enabled" -> (preference as SwitchPreference).isChecked =
                        !TelegramHandler.settings.disabled

                    "autoCheckDeleteMessageOption" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.autoCheckDeleteMessageOption

                    "autoUncheckSharePhoneNumber" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.autoUncheckSharePhoneNumber

                    "avatarPageScrollToCurrent" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.avatarPageScrollToCurrent

                    "contactPermission" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.contactPermission

                    "customEmojiMapping" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.customEmojiMapping

                    "customEmojiMappingConfig" -> preference.summary =
                        "Đã tải ${CustomEmojiMapping.emotionMap.map.size} quy tắc ánh xạ"

                    "customMapPosition" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.customMapPosition

                    "defaultSearchTab" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.defaultSearchTab

                    "disableVoiceOrCameraButton" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.disableVoiceOrCameraButton

                    "emojiStickerMenu" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.emojiStickerMenu

                    "fakeInstallPermission" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.fakeInstallPermission

                    "fixHasAppToOpen" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.fixHasAppToOpen

                    "longClickMention" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.longClickMention

                    "mutualContact" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.mutualContact

                    "noGoogleMaps" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.noGoogleMaps

                    "openLinkDialog" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.openLinkDialog

                    "sendImageWithHighQualityByDefault" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.sendImageWithHighQualityByDefault

                    "alwaysShowStorySaveIcon" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.alwaysShowStorySaveIcon

                    "removeArchiveFolder" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.removeArchiveFolder

                    "alwaysShowDownloadManager" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.alwaysShowDownloadManager

                    "hidePhoneNumber" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.hidePhoneNumber

                    "hidePhoneNumberForSelfOnly" -> (preference as SwitchPreference).apply {
                        isChecked =
                            TelegramHandler.settings.hidePhoneNumberForSelfOnly
                        isEnabled = TelegramHandler.settings.hidePhoneNumber
                    }

                    "hideFloatFab" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.hideFloatFab

                    "openTgUserLink" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.openTgUserLink

                    "copyPrivateChatLink" -> (preference as SwitchPreference).isChecked =
                        TelegramHandler.settings.copyPrivateChatLink
                }
            }
            val searchItem = SearchItem(
                preference,
                preference.key.orEmpty(),
                preference.title ?: "",
                preference.summary ?: "",
                entries,
                preference is PreferenceGroup,
            )
            // searchItem.appendExtraKeywords()
            add(searchItem)
            if (preference is PreferenceGroup) {
                addAll(retrieve(preference))
            }
        }
    }

    class Hint(val hint: String, val startIdx: Int, val fullText: CharSequence)
    class SearchItem(
        val preference: Preference,
        val key: String,
        private val title: CharSequence,
        private val summary: CharSequence,
        private val entries: Array<out CharSequence>,
        private val isGroup: Boolean,
        val extra: MutableList<String> = mutableListOf(),
    ) {
        var cacheScore = 0
            private set

        fun calcScoreAndApplyHintBy(text: String): Int {
            if (text.isEmpty() || isGroup) {
                cacheScore = 0
                return 0
            }
            var score = 0
            var titleHint: Hint? = null
            var summaryHint: Hint? = null
            var otherHint: Hint? = null
            if (title.isNotEmpty() && title.indexOf(text, ignoreCase = true).takeIf { it != -1 }
                    ?.also { titleHint = Hint(text, it, title) } != null
            ) score += 12
            if (summary.isNotEmpty() && summary.indexOf(text, ignoreCase = true).takeIf { it != -1 }
                    ?.also { summaryHint = Hint(text, it, summary) } != null
            ) score += 6
            if (entries.isNotEmpty() && entries.firstNotNullOfOrNull { e ->
                    e.indexOf(text, ignoreCase = true).takeIf { it != -1 }
                        ?.also { otherHint = Hint(text, it, e) }
                } != null) {
                score += 3
            }
            if (extra.isNotEmpty() && extra.firstNotNullOfOrNull { e ->
                    e.indexOf(text, ignoreCase = true).takeIf { it != -1 }
                        ?.also { if (otherHint == null) otherHint = Hint(text, it, e) }
                } != null) {
                score += 2
            }
            cacheScore = score
            applyHint(titleHint, summaryHint, otherHint)
            return score
        }

        fun restore() {
            preference.title = title
            preference.summary = summary
        }

        private fun applyHint(titleHint: Hint?, summaryHint: Hint?, otherHint: Hint?) {
            preference.title = title.withHint(titleHint)
            if (titleHint == null && summaryHint != null) {
                preference.summary = summary.withHint(summaryHint)
            } else if (titleHint == null && otherHint != null) {
                preference.summary = SpannableStringBuilder(summary).apply {
                    if (isNotEmpty()) appendLine()
                    append(otherHint.fullText.withHint(otherHint, true))
                }
            } else {
                preference.summary = summary
            }
        }

        private fun CharSequence.withHint(hint: Hint?, other: Boolean = false): CharSequence {
            if (hint == null || hint.hint.isEmpty())
                return this
            val startIdx = hint.startIdx
            if (startIdx == -1) return this
            val endIdx = startIdx + hint.hint.length
            if (endIdx > length) return this
            val flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            val hintColor = preference.context.getColor(R.color.text_search_hint)
            val colorSpan = ForegroundColorSpan(hintColor)
            val boldSpan = StyleSpan(Typeface.BOLD)
            return SpannableStringBuilder(this).apply {
                setSpan(colorSpan, startIdx, endIdx, flags)
                setSpan(boldSpan, startIdx, endIdx, flags)
                if (other) {
                    // to make other text smaller and append to summary
                    val sizeSpan =
                        AbsoluteSizeSpan(12.sp2px(preference.context.resources).toInt(), false)
                    setSpan(sizeSpan, 0, length, flags)
                }
            }
        }

        override fun toString(): String {
            return buildString {
                append("SearchItem {")
                append("\n  preference=")
                append(preference)
                append("\n  title=")
                append(title)
                append("\n  summary=")
                append(summary)
                append("\n score=")
                append(cacheScore)
                append("\n}")
            }
        }
    }

    private fun getContentView(): View {
        prefScreen = PreferenceScreen::class.java.newInstAs(context, null)
        listView = ListView(context)

        prefScreen.run {
            switchPreference("Công tắc chính", "enabled") {
                setDefaultValue(true)
            }

            category("Thay đổi hành vi mặc định") {
                switchPreference(
                    "Tự động chọn xóa cho cả hai bên",
                    "autoCheckDeleteMessageOption",
                    "Khi xóa tin nhắn trong trò chuyện riêng, tự động chọn tùy chọn xóa cho cả đối phương"
                )
                switchPreference(
                    "Tự động hủy chia sẻ số điện thoại",
                    "autoUncheckSharePhoneNumber",
                    "Khi thêm liên hệ, tự động bỏ chọn mục chia sẻ số điện thoại của bạn"
                )
                switchPreference(
                    "Danh sách ảnh đại diện mặc định là ảnh hiện tại",
                    "avatarPageScrollToCurrent",
                    "Nếu có nhiều ảnh đại diện và ảnh hiện tại không phải là ảnh đầu tiên, khi kéo xuống để xem danh sách ảnh đầy đủ, sẽ tự động chuyển đến ảnh đại diện hiện tại (hành vi gốc là luôn chuyển đến ảnh đầu tiên)"
                )
                switchPreference(
                    "Hashtag luôn tìm trong kênh hiện tại",
                    "defaultSearchTab",
                )
                switchPreference(
                    "Mặc định gửi ảnh chất lượng cao",
                    "sendImageWithHighQualityByDefault",
                    "Yêu cầu phiên bản 11.12.0 (5997) trở lên, đồng thời xóa biểu tượng HD ở góc dưới bên trái của xem trước ảnh"
                )
                switchPreference(
                    "Luôn cho phép lưu tin story",
                    "alwaysShowStorySaveIcon"
                )
                switchPreference(
                    "Luôn hiển thị trình quản lý tải xuống",
                    "alwaysShowDownloadManager"
                )
            }

            category("Quyền riêng tư") {
                switchPreference(
                    "Mặc định ẩn số điện thoại",
                    "hidePhoneNumber",
                    "Ẩn số điện thoại trong menu chính, nhấn vào để hiện/ẩn; ẩn số điện thoại trên trang cá nhân, nhấn nút bên phải để hiện/ẩn"
                )
                switchPreference(
                    "Chỉ ẩn số điện thoại của chính mình",
                    "hidePhoneNumberForSelfOnly",
                    "Số điện thoại của người khác sẽ mặc định hiển thị (nếu có), cũng có thể ẩn đi, cần bật 'Mặc định ẩn số điện thoại' trước"
                )
            }

            category("Bỏ qua quyền") {
                switchPreference(
                    "Bỏ qua quyền truy cập danh bạ",
                    "contactPermission",
                    "Mở trang danh bạ sẽ không yêu cầu quyền truy cập danh bạ nữa"
                )
                switchPreference(
                    "Mở file APK không cần xin quyền",
                    "fakeInstallPermission",
                    "Khi mở file APK sẽ không kiểm tra quyền REQUEST_INSTALL_PACKAGE, điều này không thực sự cấp quyền"
                )
                switchPreference(
                    "Không cần Google Maps",
                    "noGoogleMaps",
                    "Không còn hiển thị thông báo yêu cầu cài đặt Google Maps"
                )
            }



            category("Emoji và Sticker") {
                switchPreference(
                    "Ánh xạ emoji tùy chỉnh",
                    "customEmojiMapping",
                )
                preference(
                    "Cấu hình ánh xạ emoji tùy chỉnh",
                    "customEmojiMappingConfig",
                )
                switchPreference(
                    "Xem người tạo Gói Emoji và Sticker",
                    "emojiStickerMenu",
                    "Thêm tùy chọn xem người tạo trong menu của hộp thoại danh sách Gói Emoji và Sticker"
                )
            }

            category("Tối ưu hóa liên kết") {
                switchPreference(
                    "Chặn mở liên kết trùng lặp",
                    "fixHasAppToOpen",
                )
                switchPreference(
                    "Sửa các ký tự không mong muốn trong liên kết",
                    "openLinkDialog",
                    "Nếu liên kết được mở chứa các ký tự không mong muốn (ví dụ: liên kết và văn bản phía sau không có khoảng trắng), sẽ luôn hiển thị một hộp thoại và có thể nhấp vào nút 'Sửa' để mở liên kết đã loại bỏ các ký tự đó"
                )
                switchPreference(
                    "Mở liên kết người dùng Telegram",
                    "openTgUserLink",
                    "Chuyển đổi tg://user?id=xxx thành tg://openmessage?user_id=xxx và mở nó"
                )
                switchPreference(
                    "Sao chép liên kết tin nhắn trong trò chuyện riêng",
                    "copyPrivateChatLink",
                    "Liên kết chỉ có hiệu lực với chính bạn, định dạng tg://openmessage?user_id=xxx&message_id=yyy"
                )
            }

            category("Khác") {
                switchPreference(
                    "Tắt nút ghi âm hoặc camera trong ô nhập liệu",
                    "disableVoiceOrCameraButton",
                )
                switchPreference(
                    "Tùy chỉnh kinh độ và vĩ độ trên bản đồ",
                    "customMapPosition",
                    "Nhấn giữ nút định vị để mở hộp thoại"
                )
                switchPreference(
                    "Nhấn giữ để nhắc đến không có tên người dùng",
                    "longClickMention",
                    "Trong danh sách nhắc đến (@), nhấn giữ vào một người để nhắc đến họ mà không kèm theo tên người dùng"
                )
                switchPreference(
                    "Đánh dấu liên hệ hai chiều",
                    "mutualContact",
                    "Đánh dấu các liên hệ hai chiều của bạn (↑↓) trong danh sách liên hệ"
                )
                switchPreference(
                    "Xóa mục 'Lưu trữ' khi kéo xuống",
                    "removeArchiveFolder"
                )
                switchPreference(
                    "Xóa nút nổi trên trang chủ",
                    "hideFloatFab"
                )
            }
        }
        prefScreen.bind(listView)
        searchItems = retrieve(prefScreen)
        adapter = listView.adapter as BaseAdapter
        val contentView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val searchBar = context.inflateLayout(R.layout.search_bar)
        val editView = searchBar.findViewById<EditText>(R.id.search)
        val clearView = searchBar.findViewById<View>(R.id.clear)
        searchBar.setOnClickListener {
            editView.requestFocus()
            context.getSystemService(InputMethodManager::class.java)
                .showSoftInput(editView, 0)
        }
        editView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) = search(s?.toString()?.trim().orEmpty())
        })
        editView.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(v.text.toString().trim())
                true
            } else false
        }
        clearView.setOnClickListener {
            editView.setText("")
        }
        contentView.addView(searchBar)
        contentView.addView(listView)
        return contentView
    }

    companion object {

        fun show(context: Context) {
            val themedContext = ContextThemeWrapper(
                context,
                android.R.style.Theme_DeviceDefault_DayNight
            )
            try {
                SettingDialog(themedContext).show()
            } catch (_: Resources.NotFoundException) {
                AlertDialog.Builder(themedContext)
                    .setTitle("Cần khởi động lại")
                    .setMessage("Do không tải được tài nguyên, bạn cần khởi động lại ứng dụng để hiển thị giao diện cài đặt.")
                    .setPositiveButton("Khởi động lại") { _, _ ->
                        restartApplication(context.findBaseActivity())
                    }.show()
            }
        }
    }

    init {
        val activity = context.findBaseActivity()
        activity.addModuleAssets(Entry.modulePath)

        setView(getContentView())
        setTitle("MyInjector")
        setNegativeButton("Quay lại", null)
        setPositiveButton("Khởi động lại") { _, _ ->
            restartApplication(activity)
        }
    }
}

class Settings : IHook() {
    override fun onHook() {
        val drawerLayoutAdapterClass = findClass("org.telegram.ui.Adapters.DrawerLayoutAdapter")
        val itemClass = findClass("org.telegram.ui.Adapters.DrawerLayoutAdapter\$Item")

        drawerLayoutAdapterClass.hookAllAfter("resetItems") { param ->
            val items = param.thisObject.getObjAs<ArrayList<Any?>>("items")
            val settingsIdx = items.indexOfFirst { it != null && itemClass.isInstance(it) && it.getObjAs<Int>("id") == 8 }
            val settingsItem = items[settingsIdx]
            // getItemViewType() return 3 by default
            val mySettingsItem =
                itemClass.newInst(114514, "MyInjector", settingsItem.getObjAs<Int>("icon"))
            mySettingsItem.setObj("listener", object : View.OnClickListener {
                override fun onClick(v: View) {
                    SettingDialog.show(v.context)
                }
            })
            items.add(settingsIdx + 1, mySettingsItem)
        }
    }
}