package org.caexplorer.engine

import platform.posix.sysconf
import platform.posix._SC_NPROCESSORS_ONLN

actual val DEFAULT_WORKERS: Int = sysconf(_SC_NPROCESSORS_ONLN).toInt().coerceAtLeast(1)
