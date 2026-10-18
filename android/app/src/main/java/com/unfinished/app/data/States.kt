package com.unfinished.app.data

enum class MemoryState { DRAFT, SEALED, ARCHIVED, DELETED }
enum class ResurfaceEventState { PENDING_DUE, ACTIVE, DEFERRED, COMPLETED, LEGACY_PENDING, EXPIRED }
enum class RecallSessionState { RESURFACED, RECALLING, RECALL_STOPPED, REVEAL_REQUESTED, REVEALED, REFLECTED, COMPLETED, ABANDONED }

/** Future account/recovery capabilities stay behind interfaces and are disabled in V1. */
interface AccountBindingGateway
interface RecoveryGateway
interface RemoteConfigProvider
interface AdminExtension
interface SettingsFeatureRegistry
