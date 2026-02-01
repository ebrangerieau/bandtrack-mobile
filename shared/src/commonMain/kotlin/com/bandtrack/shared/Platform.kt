package com.bandtrack.shared

expect class Platform() {
    val name: String
}

class Greeting {
    private val platform = Platform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}
