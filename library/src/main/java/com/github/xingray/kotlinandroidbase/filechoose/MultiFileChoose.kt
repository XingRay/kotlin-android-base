package com.github.xingray.kotlinandroidbase.filechoose;

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.github.xingray.kotlinandroidbase.camera.contentToString


private val TAG = MultiFileChoose::class.simpleName

class MultiFileChoose(mimeType: MimeType = MimeType.ALL) {

    private var mCancelCallback: ((Int, String) -> Unit)? = null
    private var mSuccessCallback: ((List<Uri>) -> Unit)? = null
    private var mPickImageLauncher: ActivityResultLauncher<Intent>? = null
    private var mMimeType = mimeType


    fun onCreate(activity: ComponentActivity) {
        mPickImageLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "registerForActivityResult: onResult: result:${result}")

            val resultCode = result.resultCode
            Log.d(TAG, "onCreate: resultCode:${resultCode}")
            if (resultCode != Activity.RESULT_OK) {
                val cancelCallback = mCancelCallback
                if (cancelCallback == null) {
                    Log.d(TAG, "cancelCallback is null, resultCode:${resultCode}")
                } else {
                    cancelCallback.invoke(resultCode, "result code is not Activity.RESULT_OK")
                }

                return@registerForActivityResult
            }

            val resultData = result.data ?: run {
                mCancelCallback?.invoke(resultCode, "result.data is null")
                return@registerForActivityResult
            }
            Log.d(TAG, "onCreate: resultData:${resultData}")

            val clipData: ClipData = resultData.clipData ?: let {
                mCancelCallback?.invoke(resultCode, "ClipData is null")
                return@registerForActivityResult
            }


            val count = clipData.itemCount
            if (count == 0) {
                mCancelCallback?.invoke(resultCode, "ClipData is empty")
                return@registerForActivityResult
            }

            val uriList = mutableListOf<Uri>()
            for (i in 0 until count) {
                val item = clipData.getItemAt(i)
                val uri = item.uri
                uriList.add(uri)
            }
            Log.d(TAG, "onCreate: clipData, uriList:${uriList.contentToString()}")

            mSuccessCallback?.invoke(uriList)
        }
    }

    fun perform(
        cancelCallback: ((Int, String) -> Unit)?,
        successCallback: (List<Uri>) -> Unit
    ) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.setType(mMimeType.type)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        perform(intent, cancelCallback, successCallback)
    }

    fun perform(
        intent: Intent,
        cancelCallback: ((Int, String) -> Unit)?,
        successCallback: (List<Uri>) -> Unit
    ) {
        this.mSuccessCallback = successCallback
        this.mCancelCallback = cancelCallback
        mPickImageLauncher?.launch(intent)
    }
}
