package com.squadris.squadris.compose.base

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel

/** Base View model as a proxy of data, data requests and view control */
abstract class BaseViewModel: ViewModel(), LifecycleObserver