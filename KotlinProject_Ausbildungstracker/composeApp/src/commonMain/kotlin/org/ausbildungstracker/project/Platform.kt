package org.ausbildungstracker.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform