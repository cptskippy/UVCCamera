/**
 * \brief Define libusb version macros for build and runtime identification.
 *
 * Header providing version number macros used throughout libusb for API
 * compatibility checks and version reporting. Parsed by m4, windres, and RC.EXE.
 *
 * Exports:
 *   LIBUSB_MAJOR, LIBUSB_MINOR, LIBUSB_MICRO, LIBUSB_NANO: Version components
 *   LIBUSB_RC: Release candidate suffix
 *
 * Dependencies:
 *   - version_nano.h: Nano version definition
 *
 * Architecture Note:
 *   Keep file simple for m4/windres parsing. Version macros are used by
 *   libusb_get_version() and build scripts.
 */
/* This file is parsed by m4 and windres and RC.EXE so please keep it simple. */
#include "version_nano.h"
#ifndef LIBUSB_MAJOR
#define LIBUSB_MAJOR 1
#endif
#ifndef LIBUSB_MINOR
#define LIBUSB_MINOR 0
#endif
#ifndef LIBUSB_MICRO
#define LIBUSB_MICRO 19
#endif
#ifndef LIBUSB_NANO
#define LIBUSB_NANO 0
#endif
/* LIBUSB_RC is the release candidate suffix. Should normally be empty. */
#ifndef LIBUSB_RC
#define LIBUSB_RC ""
#endif
