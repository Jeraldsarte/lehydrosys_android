package com.example.lehydrosys

import android.os.Bundle
import android.webkit.SslErrorHandler
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import androidx.fragment.app.Fragment
import android.view.View

class GraphFragment : Fragment(R.layout.fragment_graph) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView: WebView = view.findViewById(R.id.webView)
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                // Ignore SSL certificate errors (for development only)
                handler?.proceed()
            }
        }

        webView.settings.apply {
            javaScriptEnabled = true // Enable JavaScript for Chart.js
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // Allow mixed content
        }

        // Enable debugging for WebView
        WebView.setWebContentsDebuggingEnabled(true)

        // Load the hosted web page
        webView.loadUrl("https://lehydrosys-sqfy.onrender.com/graphs.html")
    }
}