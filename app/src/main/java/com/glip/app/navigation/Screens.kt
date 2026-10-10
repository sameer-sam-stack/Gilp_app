package com.glip.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object Registration : Screen

    @Serializable
    data object MainHost : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object Services : Screen

    @Serializable
    data object Profile : Screen
}
