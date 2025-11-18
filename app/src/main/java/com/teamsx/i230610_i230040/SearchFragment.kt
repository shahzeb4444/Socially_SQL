package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment

class SearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        val searchBox = view.findViewById<AutoCompleteTextView>(R.id.searchfield)

        // Single tap → open the dedicated search screen
        searchBox.setOnClickListener {
            val q = searchBox.text?.toString().orEmpty()
            startActivity(
                Intent(requireContext(), UserSearchActivity::class.java)
                    .putExtra(UserSearchActivity.EXTRA_QUERY, q)
            )
        }

        // (Optional) Also open when field gains focus via keyboard navigation
        searchBox.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val q = searchBox.text?.toString().orEmpty()
                startActivity(
                    Intent(requireContext(), UserSearchActivity::class.java)
                        .putExtra(UserSearchActivity.EXTRA_QUERY, q)
                )
                // Clear focus so we don’t re-trigger when returning
                searchBox.clearFocus()
            }
        }

        return view
    }
}
