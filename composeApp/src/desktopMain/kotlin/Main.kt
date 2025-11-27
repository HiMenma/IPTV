import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.menmapro.iptv.App
import com.menmapro.iptv.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "IPTV Player",
            icon = painterResource("app_icon.png")
        ) {
            App()
        }
    }
}
