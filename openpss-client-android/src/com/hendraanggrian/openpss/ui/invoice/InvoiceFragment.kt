package com.hendraanggrian.openpss.ui.invoice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.ui.BaseFragment

class InvoiceFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_invoice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }
}
