package com.unfinished.app.data

enum class MemoryState { DRAFT, SEALED, ARCHIVED, DELETED }
enum class ResurfaceEventState { AVAILABLE, OPENED, DEFERRED, EXPIRED, COMPLETED }
enum class RecallSessionState { CREATED, CUEING, ANSWERED, REVEALED, COMPLETED, ABANDONED }

/** Future account/recovery capabilities stay behind interfaces and are disabled in V1. */
interface AccountBindingGateway
interface RecoveryGateway
interface RemoteConfigProvider
interface AdminExtension
interface SettingsFeatureRegistry
