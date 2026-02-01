package com.bandtrack.shared

actual class Platform {
    actual val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}
