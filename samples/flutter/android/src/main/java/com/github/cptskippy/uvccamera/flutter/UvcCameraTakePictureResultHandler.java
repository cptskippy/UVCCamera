package com.github.cptskippy.uvccamera.flutter;

import java.io.File;

/**
 * Handler to be notified when the take-picture result is available.
 */
@FunctionalInterface
/* package-private */ interface UvcCameraTakePictureResultHandler {

/**
 * Report the result of a take-picture request.
 *
 * Args:
 *     outputFile: the file the picture was saved to, or null if the picture could not be taken
 *     error: the error that occurred while taking the picture, or null on success
 */
    void onResult(File outputFile, Exception error);

}
