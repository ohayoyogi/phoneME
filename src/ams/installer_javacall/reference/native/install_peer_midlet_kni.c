/*
 *
 *
 * Copyright  1990-2007 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 */

#include <kni.h>
#include <sni.h>
#include <midpError.h>
#include <midpUtilKni.h>
#include <midpServices.h>

#include <suitestore_common.h>

#include <pcsl_memory.h> /* to have definition of NULL */

#include <javacall_defs.h>
#include <javacall_memory.h>
#include <javacall_ams_installer.h>
#include <javacall_ams_platform.h>

/*
 * IMPL_NOTE: this request code is not used in java_ams_install_ask/answer
 * and is treated as other requests for handiness. If this value is changed,
 * the corresponding value in InstallerMIDletPeer class must also be changed.
 */
#define RQ_UPDATE_STATUS 6

/* IMPL_NOTE: for now we assume that only one installer may run at a time. */
int g_installerIsolateId = -1;

/**
 * This field is set up by the javacall_ams_install_answer() callback.
 */
jboolean g_fAnswer = JAVACALL_FALSE, g_fAnswerReady = JAVACALL_FALSE;

/**
 * Sends a request of type defined by the given request code to
 * the party that uses this installer via the native callback.
 *
 * Note: only some of parameters are used, depending on the request code
 *
 * IMPL_NOTE: the request code passed to this method as an argument must
 *            be the same as the corresponding JAVACALL_INSTALL_REQUEST_*
 *            value defined in javacall_ams_installer.h.
 *
 * @param requestCode   code of the request to the native callback
 * @param installState  current installation state
 * @param installStatus current status of the installation, -1 if not used
 * @param newLocation   new url of the resource to install; null if not used
 */
KNIEXPORT KNI_RETURNTYPE_VOID
KNIDECL(com_sun_midp_installer_InstallerPeerMIDlet_sendNativeRequest0) {
    javacall_ams_install_state jcInstallState;
    javacall_ams_install_data  jcRequestData;
    javacall_result            jcRes = JAVACALL_OK;
    javacall_ams_install_request_code requestCode =
        (javacall_ams_install_request_code) KNI_GetParameterAsInt(1);
    pcsl_string pcslJarUrl = PCSL_STRING_NULL_INITIALIZER,
                pcslSuiteName = PCSL_STRING_NULL_INITIALIZER;

    KNI_StartHandles(4);
    KNI_DeclareHandle(javaInstallState);
    KNI_DeclareHandle(clazz);
    KNI_DeclareHandle(tmpHandle);

    KNI_GetParameterAsObject(2, javaInstallState);

    if (g_fAnswerReady == JAVACALL_TRUE) {
        g_fAnswerReady = JAVACALL_FALSE;
        /* all done, the answer is ready, just return */
    } else {
        jcRequestData.installStatus =
            (javacall_ams_install_status) KNI_GetParameterAsInt(3);

        /*
         * Converting javaInstallState state object into
         * the Javacall structure.
         */

        /*
         * This is needed because KNI_SAVE_PCSL_STRING_FIELD() macro
         * calls pcsl_mem_free(destString) for some reason.
         */
        memset((char*)&jcInstallState, 0, sizeof(jcInstallState));

        jcRes = JAVACALL_FAIL;

        do {
            pcsl_string_status pcslRes;
            jsize strSize, convertedLength;
            jint appId, suiteId, jarSize, exceptionCode;

            KNI_GetObjectClass(javaInstallState, clazz);

            KNI_SAVE_INT_FIELD(javaInstallState, clazz, "appId", appId);
            jcInstallState.appId = (javacall_app_id)appId;

            KNI_SAVE_INT_FIELD(javaInstallState, clazz, "suiteId", suiteId);
            jcInstallState.suiteId = (javacall_suite_id)suiteId;

            KNI_SAVE_INT_FIELD(javaInstallState, clazz, "jarSize", jarSize);
            jcInstallState.jarSize = (javacall_int32)jarSize;

            KNI_SAVE_INT_FIELD(javaInstallState, clazz, "exceptionCode",
                               exceptionCode);
            jcInstallState.exceptionCode =
                (javacall_ams_install_exception_code)exceptionCode;

            KNI_SAVE_PCSL_STRING_FIELD(javaInstallState, clazz, "jarUrl",
                                       &pcslJarUrl, tmpHandle);
            strSize = pcsl_string_utf16_length(&pcslJarUrl);
            jcInstallState.jarUrl = javacall_malloc((strSize + 1) << 1);
            pcslRes = pcsl_string_convert_to_utf16(&pcslJarUrl,
                (jchar*)jarUrl, strSize, &convertedLength);
            if (pcslRes != PCSL_STRING_OK) {
                break;
            }

            KNI_SAVE_PCSL_STRING_FIELD(javaInstallState, clazz, "suiteName",
                &pcslSuiteName, tmpHandle);
            strSize = pcsl_string_utf16_length(&pcslsuiteName);
            jcInstallState.suiteName = javacall_malloc((strSize + 1) << 1);
            pcslRes = pcsl_string_convert_to_utf16(&pcslSuiteName,
                (jchar*)suiteName, strSize, &convertedLength);
            if (pcslRes != PCSL_STRING_OK) {
                break;
            }

            jcRes = JAVACALL_OK;
        } while (0);

        pcsl_string_free(&pcslJarUrl);
        pcsl_string_free(&pcslSuiteName);

        /*
        TODO:
        public String[] suiteProperties;
        public String[] authPath;

        javacall_ams_properties suiteProperties;
        javacall_const_utf16_string authPath[];
        */

        if (jcRes == JAVACALL_OK) {
            if ((int)requestCode == RQ_UPDATE_STATUS) {
                /* reporting the current installation progress */
                int currStepDone = 50;

                java_ams_install_report_progress(&jcInstallState,
                    jcRequestData.installStatus, currStepDone,
                        (int)((int)jcRequestData.installStatus * 100 /
                              (int) JAVACALL_INSTALL_STATUS_COMPLETED));
            } else {
                /* get request type-dependent parameters */
                if (requestCode ==
                        JAVACALL_INSTALL_REQUEST_CONFIRM_REDIRECTION) {
                    pcsl_string_status pcslRes;
                    jsize strSize, convertedLength;

                    GET_PARAMETER_AS_PCSL_STRING(4, newLocation)

                    strSize = pcsl_string_utf16_length(&newLocation);
                    jcRequestData.newLocation =
                        javacall_malloc((strSize + 1) << 1);

                    pcslRes = pcsl_string_convert_to_utf16(&newLocation,
                        (jchar*)jcRequestData.newLocation, strSize,
                            &convertedLength);

                    RELEASE_PCSL_STRING_PARAMETER

                    if (pcslRes != PCSL_STRING_OK) {
                        jcRes = JAVACALL_FAIL;
                    }
                }

                if (jcRes == JAVACALL_OK) {
                    /* sending the request */
                    g_installerIsolateId = -1;
                    jcRes = java_ams_install_ask(requestCode, &jcInstallState,
                                                 &jcRequestData);

                    if (jcRes != JAVACALL_OK) {
                        /* If something is wrong, apply "No" answer */
                        g_fAnswer = JAVACALL_FALSE;
                        g_fAnswerReady = JAVACALL_TRUE;
                    }
                }
            } /* if RQ_UPDATE_REQUEST */
        } /* if (jcRes == JAVACALL_OK) */
    }

    KNI_EndHandles();

    KNI_ReturnVoid();
}

/**
 * Returns yes/no answer from the native callback.
 *
 * @return yes/no answer from the native callback
 */
KNIEXPORT KNI_RETURNTYPE_BOOLEAN
KNIDECL(com_sun_midp_installer_InstallerPeerMIDlet_getAnswer0) {
    if (g_fAnswerReady) {
        g_fAnswerReady = JAVACALL_FALSE;
        g_installerIsolateId = -1;
        return g_fAnswer;
    } else {
        /* block the thread only if the request was sent successfully */
        g_installerIsolateId = getCurrentIsolateId();
        SNI_SetSpecialThread(g_installerIsolateId);
        SNI_BlockThread();
    }

    KNI_ReturnBoolean(g_fAnswer);
}

/**
 * Reports to the party using this installer that
 * the operation has been completed.
 *
 * @param appId this application ID
 * @param suiteId ID of the newly installed midlet suite, or
 *                MIDletSuite.UNUSED_SUITE_ID if the installation
 *                failed
 * @param errMsg error message if the installation failed, null otherwise
 */
KNIEXPORT KNI_RETURNTYPE_VOID
KNIDECL(com_sun_midp_installer_InstallerPeerMIDlet_reportFinished0) {
    pcsl_string errMsgParam = PCSL_STRING_NULL_INITIALIZER;
    jint appId = KNI_GetParameterAsInt(1);
    SuiteIdType suiteId = (SuiteIdType) KNI_GetParameterAsInt(2);
    javacall_ams_install_data jcInstallData;

    KNI_StartHandles(2);
    KNI_DeclareHandle(errMsg);

    /* get request type-dependent parameters */
    if (suiteId == UNUSED_SUITE_ID) {
        GET_PARAMETER_AS_PCSL_STRING(3, errMsgParam)

        if (!pcsl_string_is_null(&errMsgParam)) {
            /* IMPL_NOTE: pass the error message to the listener */
        }

        RELEASE_PCSL_STRING_PARAMETER
    }

    jcInstallData.installStatus = JAVACALL_INSTALL_STATUS_COMPLETED;
    jcInstallData.suiteId = (javacall_suite_id)suiteId;

    java_ams_operation_completed(JAVACALL_OPCODE_INSTALL_SUITE,
                                 (javacall_app_id)appId,
                                 &jcInstallData);

    KNI_EndHandles();

    KNI_ReturnVoid();
}
