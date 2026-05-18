package org.caexplorer.engine

actual val DEFAULT_WORKERS: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
