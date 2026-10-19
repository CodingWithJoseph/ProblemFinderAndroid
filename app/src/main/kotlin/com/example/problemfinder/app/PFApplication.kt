package com.example.problemfinder.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application-level Hilt entry point used to assemble the module bindings. */
@HiltAndroidApp
class PFApplication : Application()

