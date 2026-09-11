#ifndef LIBUVC_CONFIG_H
#define LIBUVC_CONFIG_H

/**
 * \brief Build configuration and version macros for libuvc.
 *
 * Exports:
 *   LIBUVC_VERSION_MAJOR/MINOR/PATCH: Version components
 *   LIBUVC_VERSION_STR: Human-readable version string
 *   LIBUVC_VERSION_INT: Packed version integer
 *   LIBUVC_HAS_JPEG: Feature flag for JPEG support
 *
 * Dependencies:
 *   - utilbase.h: Base utilities
 *
 * Architecture Note:
 *   This header is generated/configured at build time. Do not modify manually
 *   outside of build configuration steps.
 */
#include "utilbase.h"

#define LIBUVC_VERSION_MAJOR 0
#define LIBUVC_VERSION_MINOR 0
#define LIBUVC_VERSION_PATCH 4
#define LIBUVC_VERSION_STR "0.0.4"
#define LIBUVC_VERSION_INT \
  (0 << 16) |              \
  (0 << 8) |               \
  (4)

/* #undef LIBUVC_HAS_JPEG */

#endif // !def(LIBUVC_CONFIG_H)
