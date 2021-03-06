/*
 * Copyright © 2013 – 2016 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.etesync.syncadapter.ui.setup

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import com.etesync.syncadapter.App
import com.etesync.syncadapter.R
import com.etesync.syncadapter.ui.DebugInfoActivity
import com.etesync.syncadapter.ui.setup.BaseConfigurationFinder.Configuration

class DetectConfigurationFragment : DialogFragment(), LoaderManager.LoaderCallbacks<Configuration> {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val progress = ProgressDialog(activity)
        progress.setTitle(R.string.login_configuration_detection)
        progress.setMessage(getString(R.string.login_querying_server))
        progress.isIndeterminate = true
        progress.setCanceledOnTouchOutside(false)
        isCancelable = false
        return progress
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loaderManager.initLoader(0, arguments, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Configuration> {
        return ServerConfigurationLoader(context!!, args!!.getParcelable(ARG_LOGIN_CREDENTIALS) as LoginCredentials)
    }

    override fun onLoadFinished(loader: Loader<Configuration>, data: Configuration?) {
        if (data != null) {
            if (data.isFailed)
            // no service found: show error message
                fragmentManager!!.beginTransaction()
                        .add(NothingDetectedFragment.newInstance(data.logs), null)
                        .commitAllowingStateLoss()
            else
            // service found: continue
                fragmentManager!!.beginTransaction()
                        .replace(android.R.id.content, EncryptionDetailsFragment.newInstance(data))
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
        } else
            App.log.severe("Configuration detection failed")

        dismissAllowingStateLoss()
    }

    override fun onLoaderReset(loader: Loader<Configuration>) {}


    class NothingDetectedFragment : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(activity!!)
                    .setTitle(R.string.login_configuration_detection)
                    .setIcon(R.drawable.ic_error_dark)
                    .setMessage(R.string.login_wrong_username_or_password)
                    .setNeutralButton(R.string.login_view_logs) { dialog, which ->
                        val intent = Intent(activity, DebugInfoActivity::class.java)
                        intent.putExtra(DebugInfoActivity.KEY_LOGS, arguments!!.getString(KEY_LOGS))
                        startActivity(intent)
                    }
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        // dismiss
                    }
                    .create()
        }

        companion object {
            private val KEY_LOGS = "logs"

            fun newInstance(logs: String): NothingDetectedFragment {
                val args = Bundle()
                args.putString(KEY_LOGS, logs)
                val fragment = NothingDetectedFragment()
                fragment.arguments = args
                return fragment
            }
        }
    }

    internal class ServerConfigurationLoader(context: Context, val credentials: LoginCredentials) : AsyncTaskLoader<Configuration>(context) {

        override fun onStartLoading() {
            forceLoad()
        }

        override fun loadInBackground(): Configuration? {
            return BaseConfigurationFinder(context, credentials).findInitialConfiguration()
        }
    }

    companion object {
        protected val ARG_LOGIN_CREDENTIALS = "credentials"

        fun newInstance(credentials: LoginCredentials): DetectConfigurationFragment {
            val frag = DetectConfigurationFragment()
            val args = Bundle(1)
            args.putParcelable(ARG_LOGIN_CREDENTIALS, credentials)
            frag.arguments = args
            return frag
        }
    }
}
