//
// Created by hihua on 17/10/27.
//

#ifndef BROWSEANIMATE_MAIN_H
#define BROWSEANIMATE_MAIN_H

#include <jni.h>
#include <sys/types.h>
#include <pthread.h>
#include <android/log.h>
#include <algorithm>
#include <list>

#define LOG_TAG		"browseanimate_jni"

#define LOGI(...)	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...)	__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...)	__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define FUNCNAME(CLASS, FUNC) 		Java_##CLASS##_##FUNC
#define FUNCS(CLASS, FUNC) 			FUNCNAME(CLASS, FUNC)

#endif //BROWSEANIMATE_MAIN_H
