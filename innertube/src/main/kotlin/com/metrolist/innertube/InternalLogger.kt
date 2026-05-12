package com.metrolist.innertube

import java.util.logging.Level
import java.util.logging.Logger

object InternalLogger {
    private val logger = Logger.getLogger("InnerTube")

    fun d(message: String) {
        logger.fine(message)
    }

    fun i(message: String) {
        logger.info(message)
    }

    fun w(message: String) {
        logger.warning(message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            logger.log(Level.SEVERE, message, throwable)
        } else {
            logger.severe(message)
        }
    }
}
