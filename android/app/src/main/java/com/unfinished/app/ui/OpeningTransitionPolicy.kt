package com.unfinished.app.ui

internal enum class OpeningAssetAvailability {
    Loading,
    Ready
}

internal enum class OpeningTransitionAction {
    WaitForAssets,
    HoldThenAnimate,
    Animate,
    EnterHome
}

/** Describes the opening-scene transition policy independently of Compose timing. */
internal fun openingTransitionAction(
    leaving: Boolean,
    assets: OpeningAssetAvailability,
    assetWaitExpired: Boolean,
    reducedMotion: Boolean
): OpeningTransitionAction = when {
    leaving && (reducedMotion || assets != OpeningAssetAvailability.Ready) -> OpeningTransitionAction.EnterHome
    leaving -> OpeningTransitionAction.Animate
    assets == OpeningAssetAvailability.Ready && reducedMotion -> OpeningTransitionAction.EnterHome
    assets == OpeningAssetAvailability.Ready -> OpeningTransitionAction.HoldThenAnimate
    assetWaitExpired -> OpeningTransitionAction.EnterHome
    else -> OpeningTransitionAction.WaitForAssets
}
