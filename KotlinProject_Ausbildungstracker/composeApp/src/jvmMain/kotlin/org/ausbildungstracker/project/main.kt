package org.ausbildungstracker.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KotlinProject_Ausbildungstracker",
    ) {
        App()
    }
}