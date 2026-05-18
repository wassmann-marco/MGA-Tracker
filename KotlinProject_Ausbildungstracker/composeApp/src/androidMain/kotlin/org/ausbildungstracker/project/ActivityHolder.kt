package org.ausbildungstracker.project

import androidx.activity.result.ActivityResultLauncher

object ActivityHolder {
    var importLauncher: ActivityResultLauncher<Array<String>>? = null
    var importCallback: ((String) -> Unit)? = null
}
