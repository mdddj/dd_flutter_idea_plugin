import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import shop.itbug.flutterx.services.PubChangelogService

@RunWith(Parameterized::class)
class PubChangelogLiveIntegrationTest(
    private val packageName: String,
) {

    @Test
    fun shouldParseLatestChangelogFromLivePubPage() {
        assumeTrue(
            "Set -Dpub.changelog.live=true to enable live pub.dev changelog tests",
            java.lang.Boolean.getBoolean("pub.changelog.live")
        )

        val url = PubChangelogService.getChangelogUrl(packageName)
        val result = PubChangelogService.fetchLatestChangelog(packageName)
        assertTrue("Expected a changelog entry for $packageName from $url", result != null)
        assertTrue("Expected a non-empty version for $packageName from $url", result!!.version.isNotBlank())
        assertTrue("Expected a non-empty changelog content for $packageName from $url", result.content.isNotBlank())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun packages(): List<Array<String>> {
            return versionedPackages.map { arrayOf(it) }
        }

        private val versionedPackages = listOf(
            "cupertino_icons",
            "after_layout",
            "convert",
            "configurable_expansion_tile_null_safety",
            "share_plus",
            "easy_refresh",
            "shared_preferences",
            "loading_more_list_fast",
            "loading_more_list_library_fast",
            "loading_animation_widget",
            "rate_my_app",
            "url_launcher",
            "firebase_core",
            "firebase_auth",
            "firebase_app_check",
            "firebase_crashlytics",
            "connectivity_plus",
            "permission_handler",
            "printing",
            "flustars_flutter3",
            "uuid",
            "extended_list",
            "flutter_staggered_grid_view",
            "flutter_swiper_null_safety_flutter3",
            "waterfall_flow",
            "get_it",
            "pdf",
            "the_apple_sign_in",
            "scrollview_observer",
            "common_utils",
            "google_sign_in",
            "app_settings",
            "ffloat_nullsafety",
            "encrypt",
            "flutter_svg",
            "extended_image",
            "logger",
            "path_provider",
            "provider",
            "path",
            "fdottedline_nullsafety",
            "fbroadcast_nullsafety",
            "crypto",
            "show_up_animation",
            "whatsapp_unilink",
            "flutter_slidable",
            "flutter_facebook_auth",
            "flutter_widget_from_html_core",
            "fsuper_nullsafety",
            "fcontrol_nullsafety",
            "hooks_riverpod",
            "hive_ce",
            "hive_ce_flutter",
            "jwt_decoder",
            "readmore",
            "barcode_scan2",
            "salesiq_mobilisten",
            "package_info_plus",
            "flutter_smart_dialog",
            "google_maps_flutter",
            "webview_flutter",
            "fast_immutable_collections",
            "flutter_stripe",
            "dd_js_util",
            "simple_ui_theme",
            "google_places_for_flutter_plus",
            "freezed_annotation",
            "json_annotation",
            "flutter_animate",
            "intl",
            "youtube_player_flutter",
            "youtube_player_iframe",
            "riverpod_annotation",
            "scoped_model",
            "flutter_rating_bar",
            "image_picker",
            "line_icons",
            "go_router",
            "expandable",
            "responsive_builder",
            "copy_with_extension",
            "html",
            "synchronized",
            "version",
            "fading_edge_scrollview",
            "location",
            "dio",
            "animate_do",
            "pdfrx",
            "flutter_bounceable",
            "blurrycontainer",
            "isar_community",
            "isar_community_flutter_libs",
            "timeago",
            "dual_screen",
            "animated_flip_counter",
            "loader_plus",
            "camera",
            "device_info_plus",
            "flutter_image_compress",
            "top_modal_sheet",
            "smooth_sheets",
            "appscheme",
            "collection",
            "devicelocale",
            "webview_completion_fix",
            "rxdart",
            "tobias",
            "flutter_lints",
            "freezed",
            "go_router_builder",
            "hive_ce_generator",
            "json_serializable",
            "build_runner",
            "copy_with_extension_gen",
            "isar_community_generator"
        )
    }
}
